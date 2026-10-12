package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.player.Player

object HealthManager : Manager() {
    private val trackerMap = Int2ObjectMaps.synchronize(Int2ObjectOpenHashMap<Tracker>())

    init {
        listener<WorldEvent.Entity.Remove>(true) {
            trackerMap.remove(it.entity.id)
        }

        listener<TotemPopEvent.Pop>(true) {
            val tracker = getTracker(it.entity)
            tracker.lastDamage += tracker.health
        }

        safeListener<PacketEvent.Receive>(true) { event ->
            when (val packet = event.packet) {
                is ClientboundAnimatePacket -> {
                    if (packet.action == ClientboundAnimatePacket.CRITICAL_HIT) {
                        trackerMap[packet.id]?.hurtTime = System.currentTimeMillis()
                    }
                }
                is ClientboundEntityEventPacket -> {
                    when (packet.eventId.toInt()) {
                        2, 33, 36, 37 -> {
                            packet.getEntity(world)?.let {
                                trackerMap[it.id]?.hurtTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
                is ClientboundSetEntityDataPacket -> {
                    val entity = world.getEntity(packet.id) as? Player ?: return@safeListener
                    val tracker = trackerMap.computeIfAbsent(entity.id) {
                        Tracker(entity)
                    }

                    val entry = packet.packedItems.find {
                        it.id == HealthManagerHelper.healthDataId
                    } ?: return@safeListener

                    (entry.value() as? Float)?.let { health ->
                        val prevHealth = tracker.health
                        val diff = prevHealth - health
                        if (diff > 0.0f
                            && (diff > 2.0f || TotemPopManager.getTracker(entity)
                                ?.let { System.currentTimeMillis() - it.popTime > 10L } == true)
                        ) {
                            tracker.lastDamage += diff
                        }

                        EntityEvent.UpdateHealth(entity, prevHealth, health).post()
                        tracker.health = health
                    }
                }
            }
        }
    }

    fun getTracker(entity: Player): Tracker {
        return trackerMap.computeIfAbsent(entity.id) {
            Tracker(entity)
        }
    }

    class Tracker(val entity: Player) {
        var health = entity.health
        var lastDamage = 0.0f
        var hurtTime = 0L
            set(value) {
                lastDamage = 0.0f
                field = value
            }

        init {
            val hurtTimeTicks = entity.hurtTime
            if (hurtTimeTicks != 0) {
                hurtTime = System.currentTimeMillis() - hurtTimeTicks * 50L
            }
        }
    }
}
