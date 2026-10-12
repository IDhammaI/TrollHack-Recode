package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.math.intersectsBlock
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

object EntityManager : Manager() {
    private var entity0 = emptyList<Entity>()
    val entity: List<Entity>
        get() = entity0

    private var livingBase0 = emptyList<LivingEntity>()
    val livingBase: List<LivingEntity>
        get() = livingBase0

    private var players0 = emptyList<Player>()
    val players: List<Player>
        get() = players0

    private var entityByID = Int2ObjectMaps.emptyMap<Entity>()
    private var livingBaseByID = Int2ObjectMaps.emptyMap<LivingEntity>()
    private var playersByID = Int2ObjectMaps.emptyMap<Player>()

    init {
        listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
            entity0 = emptyList()
            livingBase0 = emptyList()
            players0 = emptyList()

            entityByID = Int2ObjectMaps.emptyMap()
            livingBaseByID = Int2ObjectMaps.emptyMap()
            playersByID = Int2ObjectMaps.emptyMap()
        }

        listener<WorldEvent.Entity.Add>(Int.MAX_VALUE, true) {
            entity0 = entity0 + it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.put(it.entity.id, it.entity)
            this.entityByID = entityByID

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 + it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.put(it.entity.id, it.entity)
                this.livingBaseByID = livingBaseByID

                if (it.entity is Player) {
                    players0 = players0 + it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.put(it.entity.id, it.entity)
                    this.playersByID = playersByID
                }
            }
        }

        listener<WorldEvent.Entity.Remove>(Int.MAX_VALUE, true) {
            entity0 = entity0 - it.entity
            val entityByID = Int2ObjectOpenHashMap(this.entityByID)
            entityByID.remove(it.entity.id)
            this.entityByID = entityByID

            if (it.entity is LivingEntity) {
                livingBase0 = livingBase0 - it.entity
                val livingBaseByID = Int2ObjectOpenHashMap(this.livingBaseByID)
                livingBaseByID.remove(it.entity.id)
                this.livingBaseByID = livingBaseByID

                if (it.entity is Player) {
                    players0 = players0 - it.entity
                    val playersByID = Int2ObjectOpenHashMap(this.playersByID)
                    playersByID.remove(it.entity.id)
                    this.playersByID = playersByID
                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            val allEntities = world.entitiesForRendering().toList()
            entity0 = allEntities
            livingBase0 = allEntities.filterIsInstance<LivingEntity>()
            players0 = world.players().toList()

            entityByID = mapFromList(entity0)
            livingBaseByID = mapFromList(livingBase0)
            playersByID = mapFromList(players0)
        }
    }

    private fun <T : Entity> mapFromList(list: List<T>): Int2ObjectMap<T> {
        val map = Int2ObjectOpenHashMap<T>(list.size)
        for (i in list.indices) {
            val entity = list[i]
            map[entity.id] = entity
        }
        return map
    }

    fun getEntityByID(id: Int): Entity? = entityByID[id]

    fun getLivingBaseByID(id: Int): LivingEntity? = livingBaseByID[id]

    fun getPlayerByID(id: Int): Player? = playersByID[id]

    fun checkNoEntityCollision(box: AABB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.blocksBuilding }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkNoEntityCollision(box: AABB, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(box)

        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.blocksBuilding }
            .filter { it != ignoreEntity || it.isPassengerOfSameVehicle(ignoreEntity) }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos, ignoreEntity: Entity?): Boolean {
        if (ignoreEntity == null) return checkNoEntityCollision(pos)

        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.blocksBuilding }
            .filter { it != ignoreEntity || it.isPassengerOfSameVehicle(ignoreEntity) }
            .filter { it.boundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntityCollision(box: AABB): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.blocksBuilding }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntityCollision(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.blocksBuilding }
            .filter { it.boundingBox.intersectsBlock(pos) }
            .none()
    }

    fun checkNoEntity(box: AABB, predicate: (Entity) -> Boolean): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .filter(predicate)
            .none()
    }

    fun checkNoEntity(box: AABB, ignoreEntity: Entity): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it != ignoreEntity }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(box: AABB): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersects(box) }
            .none()
    }

    fun checkNoEntity(pos: BlockPos): Boolean {
        return entity.asSequence()
            .filter { it.isAlive }
            .filter { it.boundingBox.intersectsBlock(pos) }
            .none()
    }
}
