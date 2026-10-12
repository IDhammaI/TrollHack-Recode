package dev.luna5ama.trollhack.manager.managers

import com.google.common.collect.MapMaker
import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CombatEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.collections.compareFloatByDescending
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CrystalDamage
import dev.luna5ama.trollhack.util.combat.CrystalUtils.blockPos
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.combat.DamageReduction
import dev.luna5ama.trollhack.util.combat.MotionTracker
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.runIf
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import dev.luna5ama.trollhack.util.threads.isActiveOrFalse
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import it.unimi.dsi.fastutil.ints.Int2FloatMaps
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.sqrt

object CombatManager : Manager() {
    private val combatModules: List<AbstractModule>

    private val playerAttackTimeMap = WeakHashMap<Player, Long>().synchronized()
    private val damageReductionTimer = TickTimer()
    private val damageReductions = MapMaker().weakKeys().makeMap<LivingEntity, DamageReduction>()
    private val hurtTimeMap = Int2LongMaps.synchronize(Int2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val healthMap = Int2FloatMaps.synchronize(Int2FloatOpenHashMap()).apply { defaultReturnValue(Float.NaN) }

    var targetOverride: WeakReference<LivingEntity>? = null
    var target: LivingEntity? = null
        get() {
            if (field?.isAlive == false) {
                field = null
            }

            return field
        }
        set(value) {
            if (value != field) {
                CombatEvent.UpdateTarget(field, value).post()
                field = value
            }
        }
    var targetList = emptySet<LivingEntity>()

    var trackerSelf: MotionTracker? = null; private set
    var trackerTarget: MotionTracker? = null

    var contextSelf: CalcContext? = null; private set
    var contextTarget: CalcContext? = null; private set

    private val crystalTimer = TickTimer()
    private val removeTimer = TickTimer()

    private var placeJob: Job? = null
    private var crystalJob: Job? = null

    private val placeMap0 = ConcurrentHashMap<BlockPos, CrystalDamage>()
    private val crystalMap0 = MapMaker().weakKeys().makeMap<EndCrystal, CrystalDamage>()
    val placeMap: Map<BlockPos, CrystalDamage>
        get() = placeMap0
    val crystalMap: Map<EndCrystal, CrystalDamage>
        get() = crystalMap0

    var placeList = emptyList<CrystalDamage>(); private set
    var crystalList = emptyList<Pair<EndCrystal, CrystalDamage>>(); private set

    private const val PLACE_RANGE = 6
    private const val PLACE_RANGE_SQ = 36.0
    private const val CRYSTAL_RANGE_SQ = 144.0

    private const val MAX_RANGE_SQ = 256.0

    init {
        safeListener<PacketEvent.Receive>(114514) { event ->
            when (val packet = event.packet) {
                is ClientboundSoundPacket -> {
                    if (packet.source != SoundSource.BLOCKS) return@safeListener
                    if (packet.sound.value() != SoundEvents.GENERIC_EXPLODE.value()) return@safeListener
                    val list = crystalList.asSequence()
                        .map(Pair<EndCrystal, CrystalDamage>::first)
                        .filter { it.distanceSqTo(packet.x, packet.y, packet.z) <= 144.0 }
                        .runIf(CombatSetting.crystalSetDead) { onEach(EndCrystal::discard) }
                        .toList()

                    if (list.isNotEmpty() && CombatSetting.crystalSetDead) {
                        onMainThreadSafe {
                            list.forEach {
                                world.removeEntity(it.id, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED)
                            }
                        }
                    }
                    CrystalSetDeadEvent(packet.x, packet.y, packet.z, list).post()
                }

                is ClientboundAddEntityPacket -> {
                    if (packet.type == EntityTypes.END_CRYSTAL) {
                        val distSq = player.eyePosition.distanceSqTo(packet.x, packet.y, packet.z)
                        if (distSq > CRYSTAL_RANGE_SQ) return@safeListener

                        val blockPos = BlockPos(
                            packet.x.floorToInt(),
                            packet.y.floorToInt() - 1,
                            packet.z.floorToInt()
                        )
                        getCrystalDamage(blockPos)?.let {
                            CrystalSpawnEvent(packet.id, it).post()
                        }
                    }
                }

                is ClientboundAnimatePacket -> {
                    if (packet.action == ClientboundAnimatePacket.CRITICAL_HIT) {
                        hurtTimeMap[packet.id] = System.currentTimeMillis()
                    }
                }

                is ClientboundPlayerCombatKillPacket -> {
                    if (packet.playerId == player.id) {
                        EntityEvent.Death(player).post()
                    }
                }

                is ClientboundEntityEventPacket -> {
                    when (packet.eventId.toInt()) {
                        3 -> {
                            (packet.getEntity(world) as? LivingEntity)?.let {
                                EntityEvent.Death(it).post()
                            }
                            (packet.getEntity(world))?.let {
                                if (it.id == target?.id) {
                                    target = null
                                }
                            }
                        }

                        2, 33, 36, 37 -> {
                            (packet.getEntity(world))?.let {
                                hurtTimeMap[it.id] = System.currentTimeMillis()
                            }
                        }
                    }
                }

                is ClientboundSetEntityDataPacket -> {
                    val entity = world.getEntity(packet.id) as? LivingEntity ?: return@safeListener
                    val entry = packet.packedItems.find {
                        it.id == HealthManagerHelper.healthDataId
                    } ?: return@safeListener

                    (entry.value() as? Float)?.let {
                        var prevHealth = healthMap[entity.id]
                        if (prevHealth.isNaN()) prevHealth = entity.health
                        EntityEvent.UpdateHealth(entity, prevHealth, it).post()
                        healthMap[entity.id] = it

                        if (it <= 0.0f) {
                            EntityEvent.Death(entity).post()
                            if (packet.id == target?.id) {
                                target = null
                            }
                        }
                    }
                }

                is ClientboundRemoveEntitiesPacket -> {
                    packet.entityIds.forEach {
                        if (it == target?.id) target = null
                        hurtTimeMap.remove(it)
                        healthMap.remove(it)
                    }
                }
            }
        }

        listener<ConnectionEvent.Disconnect> {
            damageReductions.clear()
            hurtTimeMap.clear()
            healthMap.clear()

            target = null
            targetList = emptySet()

            trackerSelf = null
            trackerTarget = null

            contextSelf = null
            contextTarget = null

            placeMap0.clear()
            crystalMap0.clear()

            placeList = emptyList()
            crystalList = emptyList()
        }

        safeListener<WorldEvent.Entity.Add> { event ->
            when (event.entity) {
                is Player -> {
                    damageReductions[event.entity] = DamageReduction(event.entity)
                }

                is EndCrystal -> {
                    val distSq = event.entity.distanceSqTo(player.eyePosition)
                    if (distSq > CRYSTAL_RANGE_SQ) return@safeListener

                    val contextSelf = contextSelf ?: return@safeListener
                    val contextTarget = CombatManager.contextTarget

                    BackgroundScope.launch {
                        val blockPos = event.entity.blockPos
                        val mutableBlockPos = BlockPos.MutableBlockPos()
                        val crystalDamage = placeMap0.computeIfAbsent(blockPos) {
                            calculateDamage(
                                contextSelf,
                                contextTarget,
                                mutableBlockPos,
                                blockPos.toVec3d(0.5, 1.0, 0.5),
                                blockPos,
                                sqrt(distSq)
                            )
                        }

                        crystalMap0[event.entity] = crystalDamage
                    }
                }
            }
        }

        listener<WorldEvent.Entity.Remove> {
            when (it.entity) {
                is LivingEntity -> {
                    damageReductions.remove(it.entity)
                }

                is EndCrystal -> {
                    crystalMap0.remove(it.entity)
                }
            }

            if (it.entity === target) target = null
            hurtTimeMap.remove(it.entity.id)
            healthMap.remove(it.entity.id)
        }

        safeParallelListener<TickEvent.Post> {
            val trackerSelf = trackerSelf?.takeIf { it.entity === player }
                ?: MotionTracker(player)

            trackerSelf.tick()
            CombatManager.trackerSelf = trackerSelf

            trackerTarget?.tick()

            val attacked = player.lastHurtMob
            if (attacked is Player && attacked.isAlive && !attacked.isFakeOrSelf) {
                playerAttackTimeMap[attacked] =
                    System.currentTimeMillis() - (player.tickCount - player.lastHurtMobTimestamp) * 50L
            }
        }

        safeListener<RunGameLoopEvent.Tick>(Int.MAX_VALUE) {
            val flag1 = damageReductionTimer.tickAndReset(CombatSetting.crystalUpdateDelay)
            val flag2 = crystalTimer.tickAndReset(CombatSetting.crystalUpdateDelay)

            if (flag1 || flag2) {
                if (flag1) {
                    EntityManager.players.forEach {
                        damageReductions[it] = DamageReduction(it)
                    }
                    target?.let {
                        damageReductions[it] = DamageReduction(it)
                    }
                }

                if (flag2) {
                    updateCrystalDamage()
                }
            }
        }
    }

    fun getPlayerAttackTime(player: Player): Long {
        return playerAttackTimeMap[player] ?: -1L
    }

    fun getCrystalDamage(crystal: EndCrystal) =
        crystalMap0[crystal] ?: getCrystalDamage(crystal.blockPos)

    fun getCrystalDamage(blockPos: BlockPos) =
        contextSelf?.let { contextSelf ->
            placeMap0.computeIfAbsent(blockPos) {
                val crystalPos = blockPos.toVec3d(0.5, 1.0, 0.5)
                val dist = contextSelf.entity.eyePosition.distanceTo(crystalPos)
                calculateDamage(contextSelf, contextTarget, BlockPos.MutableBlockPos(), crystalPos, it, dist)
            }
        }

    fun getDamageReduction(entity: LivingEntity) =
        damageReductions[entity]

    fun getHurtTime(entity: LivingEntity): Long {
        synchronized(hurtTimeMap) {
            var hurtTime = hurtTimeMap[entity.id]

            if (hurtTime == -1L) {
                val hurtTimeTicks = entity.hurtTime
                if (hurtTimeTicks != 0) {
                    hurtTime = System.currentTimeMillis() - hurtTimeTicks * 50L
                    hurtTimeMap[entity.id] = hurtTime
                }
            }

            return hurtTime
        }
    }

    private fun SafeClientEvent.updateCrystalDamage() {
        val flag1 = !placeJob.isActiveOrFalse
        val flag2 = !crystalJob.isActiveOrFalse

        if (flag1 || flag2) {
            val predictPosSelf = trackerSelf?.calcPosAhead(CombatSetting.getPredictTicksSelf()) ?: player.position()
            val contextSelf = CalcContext(this, player, predictPosSelf)

            val target = CombatManager.target
            val contextTarget = target?.let {
                val predictPos = trackerTarget?.calcPosAhead(CombatSetting.getPredictTicksTarget()) ?: it.position()
                CalcContext(this, it, predictPos)
            }

            val remove = removeTimer.tickAndReset(100)

            CombatManager.contextSelf = contextSelf
            CombatManager.contextTarget = contextTarget

            if (flag1) {
                placeJob = BackgroundScope.launch {
                    updatePlaceMap(contextSelf, contextTarget, remove)
                    updatePlaceList()
                }
            }
            if (flag2) {
                crystalJob = BackgroundScope.launch {
                    updateCrystalMap(contextSelf, contextTarget, remove)
                    updateCrystalList()
                }
            }
        }

        damageReductionTimer.reset((CombatSetting.crystalUpdateDelay / -4).toLong())
    }

    private fun SafeClientEvent.updatePlaceMap(contextSelf: CalcContext, contextTarget: CalcContext?, remove: Boolean) {
        val eyePos = player.eyePosition
        val flooredPos = player.flooredPosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        placeMap0.values.removeIf { crystalDamage ->
            remove && (crystalDamage.crystalPos.distanceSqTo(eyePos) > MAX_RANGE_SQ
                || !canPlaceCrystal(crystalDamage.blockPos, null)
                || contextTarget != null
                && (crystalDamage.crystalPos.distanceSqTo(contextTarget.predictPos) > MAX_RANGE_SQ
                || !contextTarget.checkColliding(crystalDamage.crystalPos)))
        }

        placeMap0.replaceAll { blockPos, crystalDamage ->
            calculateDamage(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                blockPos.toVec3d(0.5, 1.0, 0.5),
                blockPos,
                eyePos.distanceTo(crystalDamage.crystalPos)
            )
        }

        val blockPos = BlockPos.MutableBlockPos()

        for (x in -PLACE_RANGE..PLACE_RANGE) {
            for (y in -PLACE_RANGE..PLACE_RANGE) {
                for (z in -PLACE_RANGE..PLACE_RANGE) {
                    blockPos.setAndAdd(flooredPos, x, y, z)
                    if (world.isOutsideBuildHeight(blockPos)) continue

                    val crystalX = blockPos.x + 0.5
                    val crystalY = blockPos.y + 1.0
                    val crystalZ = blockPos.z + 0.5

                    val distSq = eyePos.distanceSqTo(crystalX, crystalY, crystalZ)
                    if (distSq > PLACE_RANGE_SQ) continue
                    if (placeMap0.containsKey(blockPos)) continue
                    if (!canPlaceCrystal(blockPos, null)) continue

                    val crystalPos = net.minecraft.world.phys.Vec3(crystalX, crystalY, crystalZ)
                    if (contextTarget != null) {
                        if (contextTarget.predictPos.distanceSqTo(crystalPos) > CRYSTAL_RANGE_SQ) continue
                        if (!contextTarget.checkColliding(crystalPos)) continue
                    }

                    val immutablePos = blockPos.immutable()
                    placeMap0[immutablePos] = calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        crystalPos,
                        immutablePos,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    private fun SafeClientEvent.updateCrystalMap(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        remove: Boolean
    ) {
        val eyePos = player.eyePosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        if (remove) {
            crystalMap0.keys.removeIf {
                it.distanceSqTo(eyePos) > MAX_RANGE_SQ
            }
        }

        crystalMap0.replaceAll { _, crystalDamage ->
            placeMap0.computeIfAbsent(crystalDamage.blockPos) {
                calculateDamage(
                    contextSelf,
                    contextTarget,
                    mutableBlockPos,
                    it.toVec3d(0.5, 1.0, 0.5),
                    it,
                    eyePos.distanceTo(crystalDamage.crystalPos)
                )
            }
        }

        for (entity in EntityManager.entity) {
            if (!entity.isAlive) continue
            if (entity !is EndCrystal) continue

            val distSq = entity.distanceSqTo(eyePos)
            if (distSq > CRYSTAL_RANGE_SQ) continue

            crystalMap0.computeIfAbsent(entity) {
                placeMap0.computeIfAbsent(entity.blockPos) {
                    calculateDamage(
                        contextSelf,
                        contextTarget,
                        mutableBlockPos,
                        it.toVec3d(0.5, 1.0, 0.5),
                        it,
                        sqrt(distSq)
                    )
                }
            }
        }
    }

    private fun calculateDamage(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        mutableBlockPos: BlockPos.MutableBlockPos,
        crystalPos: net.minecraft.world.phys.Vec3,
        blockPos: BlockPos,
        distance: Double
    ): CrystalDamage {
        val selfDamage = max(
            contextSelf.calcDamage(crystalPos, true, mutableBlockPos),
            contextSelf.calcDamage(crystalPos, false, mutableBlockPos)
        )
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos) ?: 0.0f
        return CrystalDamage(
            crystalPos,
            blockPos,
            selfDamage,
            targetDamage,
            distance,
            contextSelf.currentPos.distanceTo(crystalPos)
        )
    }

    private fun updatePlaceList() {
        placeList = placeMap.values.sortedWith(compareFloatByDescending { it.targetDamage })
    }

    private fun updateCrystalList() {
        crystalList = crystalMap.entries
            .map { it.key to it.value }
            .sortedWith(compareFloatByDescending { it.second.targetDamage })
    }

    fun isActiveAndTopPriority(module: AbstractModule) = module.isActive() && isOnTopPriority(module)

    fun isOnTopPriority(module: AbstractModule): Boolean {
        return getTopPriority() <= module.modulePriority
    }

    fun getTopPriority(): Int {
        return getTopModule()?.modulePriority ?: -1
    }

    fun getTopModule(): AbstractModule? {
        var topModule: AbstractModule? = null
        for (module in combatModules) {
            if (!module.isActive()) continue
            if (module.modulePriority < (topModule?.modulePriority ?: 0)) continue
            topModule = module
        }
        return topModule
    }

    /** Use to mark a module that should be added to [combatModules] */
    annotation class CombatModule

    init {
        val cacheList = ArrayList<AbstractModule>()
        val annotationClass = CombatModule::class.java
        for (module in ModuleManager.modules) {
            if (module.category != Category.COMBAT) continue
            if (!module.javaClass.isAnnotationPresent(annotationClass)) continue
            cacheList.add(module)
        }
        combatModules = cacheList
    }
}

/** Holds the synched-entity-data id of LivingEntity health, obtained via mixin accessor. */
object HealthManagerHelper {
    val healthDataId: Int by lazy {
        dev.luna5ama.trollhack.mixins.accessor.AccessorLivingEntity.`trollhack$getDataHealthId`().id
    }
}
