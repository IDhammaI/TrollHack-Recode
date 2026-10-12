package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.Manager
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

object TotemPopManager : Manager() {
    private val trackerMap = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Tracker>())

    init {
        listener<ConnectionEvent.Disconnect>(true) {
            trackerMap.clear()
        }

        listener<EntityEvent.Death>(5000, true) { event ->
            if (event.entity !is Player) return@listener
            if (event.entity == mc.player) {
                trackerMap.clear()
            } else {
                trackerMap.remove(event.entity.id)?.let {
                    TotemPopEvent.Death(event.entity, it.count).post()
                }
            }
        }

        safeListener<PacketEvent.Receive>(true) {
            val packet = it.packet
            if (packet is ClientboundEntityEventPacket && packet.eventId.toInt() == 35) {
                val entity = packet.getEntity(world) as? Player ?: return@safeListener
                val tracker = trackerMap.computeIfAbsent(entity.id) { _ ->
                    Tracker(entity.id, entity.gameProfile.name)
                }
                tracker.updateCount()
                TotemPopEvent.Pop(entity, tracker.count).post()
            }
        }

        safeParallelListener<TickEvent.Post>(true) {
            for (entity in EntityManager.players) {
                trackerMap[entity.id]?.update()
            }

            val removeTime = System.currentTimeMillis()
            val iterator = trackerMap.values.iterator()

            while (iterator.hasNext()) {
                val tracker = iterator.next()
                if (tracker.timeout < removeTime) {
                    TotemPopEvent.Clear(tracker.name, tracker.count).post()
                    iterator.remove()
                }
            }
        }
    }

    fun getPopCount(entity: Entity): Int {
        return trackerMap[entity.id]?.count ?: 0
    }

    fun getTracker(entity: Entity): Tracker? {
        return trackerMap[entity.id]
    }

    class Tracker(val entityID: Int, val name: String) {
        var count = 0; private set
        var timeout = System.currentTimeMillis() + 15000L; private set
        var popTime = 0L; private set

        fun update() {
            timeout = System.currentTimeMillis() + 15000L
        }

        fun updateCount() {
            synchronized(this) {
                count++
                timeout = System.currentTimeMillis() + 15000L
                popTime = System.currentTimeMillis()
            }
        }
    }
}
