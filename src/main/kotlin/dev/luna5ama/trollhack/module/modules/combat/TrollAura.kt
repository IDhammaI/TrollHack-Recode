package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.CircularArray
import dev.fastmc.common.collection.CircularArray.Companion.average
import dev.fastmc.common.floorToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.BlockBreakEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.events.player.InteractEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HealthManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.manager.managers.TotemPopManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.SwingMode
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CombatUtils.equipBestWeapon
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
import dev.luna5ama.trollhack.util.combat.CombatUtils.totalHealth
import dev.luna5ama.trollhack.util.combat.CrystalDamage
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.blockPos
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystalOn
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.inventory.duraPercentage
import dev.luna5ama.trollhack.util.inventory.isTool
import dev.luna5ama.trollhack.util.inventory.isWeapon
import dev.luna5ama.trollhack.util.inventory.operation.swapToSlot
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.countItem
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.math.isInSight
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.toLong
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.pause.HandPause
import dev.luna5ama.trollhack.util.pause.MainHandPause
import dev.luna5ama.trollhack.util.pause.withPause
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.TimerScope
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.FastRayTraceAction
import dev.luna5ama.trollhack.util.world.FastRayTraceFunction
import dev.luna5ama.trollhack.util.world.getHitVec
import dev.luna5ama.trollhack.util.world.getHitVecOffset
import dev.luna5ama.trollhack.util.world.getMiningSide
import dev.luna5ama.trollhack.util.world.rayTraceVisible
import dev.luna5ama.trollhack.util.accessor.withPrediction
import it.unimi.dsi.fastutil.ints.Int2LongMaps
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.launch
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@CombatManager.CombatModule
internal object TrollAura : Module(
    name = "Troll Aura",
    alias = arrayOf("CA", "AC", "CrystalAura", "AutoCrystal"),
    description = "We doing a massive amount of trolling",
    category = Category.COMBAT,
    modulePriority = 80
) {
    /* Settings */
    private val page = setting("Page", Page.GENERAL)

    /* General */
    private val noSuicide by setting("No Suicide", 8.0f, 0.0f..20.0f, 0.5f, page.atValue(Page.GENERAL))
    private val placeRotationRange by setting(
        "Place Rotation Range",
        45.0f,
        0.0f..180.0f,
        5.0f,
        page.atValue(Page.GENERAL)
    )
    private val breakRotationRange by setting(
        "Break Rotation Range",
        60.0f,
        0.0f..180.0f,
        5.0f,
        page.atValue(Page.GENERAL)
    )
    private val yawSpeed by setting("Yaw Speed", 30.0f, 5.0f..90.0f, 5.0f, page.atValue(Page.GENERAL))
    private val swingMode by setting("Swing Mode", SwingMode.CLIENT, page.atValue(Page.GENERAL))
    private val swingHand by setting("Swing Hand", SwingHand.AUTO, page.atValue(Page.GENERAL))
    private val countAllCrystals by setting("Count All Crystals", true, page.atValue(Page.GENERAL))

    /* Force place */
    private val bindForcePlace by setting("Bind Force Place", Bind(), {
        if (isEnabled && it) {
            forcePlacing = !forcePlacing
            dev.luna5ama.trollhack.util.text.NoSpamMessage.sendMessage(
                TrollAura,
                "$chatName Force placing" + if (forcePlacing) " §aenabled" else " §cdisabled"
            )
        }
    }, page.atValue(Page.FORCE_PLACE))
    private val forcePlaceHealth by setting(
        "Force Place Health",
        5.0f,
        0.0f..20.0f,
        0.5f,
        page.atValue(Page.FORCE_PLACE)
    )
    private val forcePlaceArmorDura by setting("Force Place Armor Dura", 3, 0..50, 1, page.atValue(Page.FORCE_PLACE))
    private val forcePlaceMinDamage by setting(
        "Force Place Min Damage",
        1.5f,
        0.0f..10.0f,
        0.25f,
        page.atValue(Page.FORCE_PLACE)
    )
    private val forcePlaceDamageBalance by setting(
        "Force Place Damage Balance",
        0.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.FORCE_PLACE)
    )
    private val armorDdos by setting("Armor Ddos", false, page.atValue(Page.FORCE_PLACE))
    private val ddosQueueSize by setting("Ddos Queue Size", 5, 0..10, 1, page.atValue(Page.FORCE_PLACE) and ::armorDdos)
    private val ddosMinDamage by setting(
        "Ddos Min Damage",
        1.5f,
        0.0f..10.0f,
        0.1f,
        page.atValue(Page.FORCE_PLACE) and ::armorDdos
    )
    private val ddosDamageStep by setting(
        "Ddos Damage Step",
        0.1f,
        0.1f..5.0f,
        0.1f,
        page.atValue(Page.FORCE_PLACE) and ::armorDdos
    )

    /* Slow mode */
    private val slowMode0 = setting("Slow Mode", true, page.atValue(Page.SLOW_MODE))
    private val slowMode by slowMode0
    private val slowDamage by setting(
        "Slow Damage",
        4.0f,
        0.0f..10.0f,
        0.25f,
        page.atValue(Page.SLOW_MODE) and slowMode0.atTrue()
    )
    private val slowPlaceDelay by setting(
        "Slow Place Delay",
        250,
        0..1000,
        5,
        page.atValue(Page.SLOW_MODE) and slowMode0.atTrue()
    )
    private val slowBreakDelay by setting(
        "Slow Break Delay",
        50,
        0..1000,
        5,
        page.atValue(Page.SLOW_MODE) and slowMode0.atTrue()
    )

    /* Motion detect */
    private val motionDetect by setting("Motion Detect", true, page.atValue(Page.MOTION_DETECT))
    private val targetMotion by setting(
        "Target Motion",
        0.15f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val selfMotion by setting(
        "Self Motion",
        0.22f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionPlaceMinDamage by setting(
        "Motion Place Min Damage",
        3.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionPlaceMaxSelfDamage by setting(
        "Motion Place Max Self Damage",
        8.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionPlaceBalance by setting(
        "Motion Place Balance",
        -5.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionBreakMinDamage by setting(
        "Motion Break Min Damage",
        2.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionBreakMaxSelfDamage by setting(
        "Motion Break Max Self Damage",
        10.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionBreakBalance by setting(
        "Motion Break Balance",
        -6.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })

    /* Place */
    private val doPlace by setting("Place", true, page.atValue(Page.PLACE))
    private val placeMode0 = setting("Place Mode", PlaceSyncMode.SPAM, page.atValue(Page.PLACE))
    private val placeMode by placeMode0
    private val spamTimeout by setting(
        "Spam Timeout",
        1000,
        0..2000,
        10,
        page.atValue(Page.PLACE) and placeMode0.atValue(PlaceSyncMode.SPAM, PlaceSyncMode.IGNORE)
    )
    private val placeTimeout by setting("Place Timeout", 100, 0..2000, 10, page.atValue(Page.PLACE))
    private val spawnTimeout by setting(
        "Spawn Timeout",
        100,
        0..1000,
        5,
        page.atValue(Page.PLACE) and placeMode0.atValue(PlaceSyncMode.IGNORE)
    )
    private val autoSwap by setting("Auto Swap", false, page.atValue(Page.PLACE))
    private val spoofHotbar by setting("Spoof Hotbar", false, page.atValue(Page.PLACE) and ::autoSwap)
    private val manualOverride by setting("Manual Override", true, page.atValue(Page.PLACE))
    private val strictDirection by setting("Strict Direction", false, page.atValue(Page.PLACE))
    private val buildLimitBypass by setting("Build Limit Bypass", false, page.atValue(Page.PLACE))
    private val placeSwing by setting("Place Swing", true, page.atValue(Page.PLACE))
    private val lethalPlace by setting("Lethal Place", true, page.atValue(Page.PLACE))
    private val placeMinDamage by setting("Place Min Damage", 5.25f, 0.0f..20.0f, 0.25f, page.atValue(Page.PLACE))
    private val placeMaxSelfDamage by setting(
        "Place Max Self Damage",
        4.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.PLACE)
    )
    private val placeBalance by setting("Place Balance", -2.0f, -10.0f..10.0f, 0.25f, page.atValue(Page.PLACE))
    private val maxCrystal by setting("Max Crystal", 2, 1..5, 1, page.atValue(Page.PLACE))
    private val placeDelay by setting("Place Delay", 50, 0..500, 1, page.atValue(Page.PLACE))
    private val placeRange by setting("Place Range", 5.0f, 0.0f..6.0f, 0.25f, page.atValue(Page.PLACE))
    private val wallPlaceRange by setting("Wall Place Range", 3.0f, 0.0f..6.0f, 0.25f, page.atValue(Page.PLACE))

    /* Break */
    private val doBreak by setting("Break", true, page.atValue(Page.BREAK))
    private val packetBreak by setting("Packet Break", true, page.atValue(Page.BREAK))
    private val breakTimeout by setting("Break Timeout", 250, 0..1000, 5, page.atValue(Page.BREAK))
    private val spawnDelay by setting("Spawn Delay", 0, 0..1000, 5, page.atValue(Page.BREAK))
    private val predictBreak by setting("Predict Break", 0, 0..20, 1, page.atValue(Page.BREAK))
    private val predictTimeout by setting(
        "Predict Timeout",
        50,
        0..1000,
        5,
        page.atValue(Page.BREAK) and { predictBreak > 0 })
    private val predictBreakDelay by setting(
        "Predict Delay",
        0,
        0..500,
        1,
        page.atValue(Page.BREAK) and { predictBreak > 0 })
    private val ownCrystal by setting("Own Crystal", false, page.atValue(Page.BREAK))
    private val lethalBreak by setting("Lethal Break", true, page.atValue(Page.BREAK))
    private val antiWeakness by setting("Anti Weakness", true, page.atValue(Page.BREAK))
    private val breakMinDamage by setting("Break Min Damage", 4.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.BREAK))
    private val breakMaxSelfDamage by setting(
        "Break Max Self Damage",
        5.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.BREAK)
    )
    private val breakBalance by setting("Break Balance", -3.0f, -10.0f..10.0f, 0.25f, page.atValue(Page.BREAK))
    private val swapDelay by setting(
        "Swap Delay",
        8,
        0..50,
        1,
        page.atValue(Page.BREAK) and { !autoSwap || !spoofHotbar })
    private val breakDelay by setting("Break Delay", 50, 0..500, 1, page.atValue(Page.BREAK))
    private val breakAttempts by setting("Break Attempts", 8, 0..16, 1, page.atValue(Page.BREAK))
    private val retryTimeout by setting(
        "Retry Timeout",
        750,
        0..5000,
        50,
        page.atValue(Page.BREAK) and { breakAttempts > 0 })
    private val breakRange by setting("Break Range", 5.0f, 0.0f..6.0f, 0.25f, page.atValue(Page.BREAK))
    private val wallBreakRange by setting("Wall Break Range", 3.0f, 0.0f..6.0f, 0.25f, page.atValue(Page.BREAK))
    /* End of settings */

    private enum class Page {
        GENERAL, SLOW_MODE, MOTION_DETECT, FORCE_PLACE, PLACE, BREAK
    }

    @Suppress("UNUSED")
    private enum class SwingHand {
        AUTO, OFF_HAND, MAIN_HAND
    }

    private enum class PlaceSyncMode {
        NORMAL, SPAM, IGNORE, MULTI
    }

    private val antiSurroundOffset = arrayOf(
        BlockPos(0, -1, -1),
        BlockPos(-1, -1, 0),
        BlockPos(1, -1, 0),
        BlockPos(0, -1, 1),

        BlockPos(-1, -1, -1),
        BlockPos(1, -1, -1),
        BlockPos(-1, -1, 1),
        BlockPos(1, -1, 1),

        BlockPos(0, -2, -1),
        BlockPos(-1, -2, 0),
        BlockPos(1, -2, 0),
        BlockPos(0, -2, 1),

        BlockPos(0, -2, 0),

        BlockPos(-1, -2, -1),
        BlockPos(1, -2, -1),
        BlockPos(-1, -2, 1),
        BlockPos(1, -2, 1),
    )

    private val placedBBMap =
        Object2ObjectMaps.synchronize(Object2ObjectOpenHashMap<BlockPos, Pair<AABB, Long>>()) // <CrystalBoundingBox, Added Time>
    private val placedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val explodedPosMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val attackedCrystalMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val ignoredCrystalMap = Int2LongMaps.synchronize(Int2LongOpenHashMap())
    private val spawnTimeMap = Long2LongMaps.synchronize(Long2LongOpenHashMap())
    private val ddosQueue = ConcurrentLinkedDeque<CrystalDamage>()

    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val predictBreakTimer = TickTimer()

    private var selfMoving = false
    private var targetMoving = false
    private var forcePlacing = false
    private var ddosArmor = false
    private var lastRotation: BlockPos? = null
    private var lastPlaced: BlockPos? = null

    private var overridePos: BlockPos? = null
    private var overrideTime = 0L

    private val crystalID = AtomicInteger(-1)
    private var lastCrystalID = -1
    private var breakCount = 0

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray(8, 0.0f)
    private var explosionCount = 0

    var inactiveTicks = 10; private set
    val minDamage get() = max(placeMinDamage, breakMinDamage)
    val maxSelfDamage get() = min(placeMaxSelfDamage, breakMaxSelfDamage)

    override fun isActive() = isEnabled && inactiveTicks <= 20

    override fun getHudInfo(): String {
        return "%.1f".format(explosionCountArray.average() * 4.0)
    }

    init {
        spawnTimeMap.defaultReturnValue(Long.MAX_VALUE)

        onDisable {
            placedBBMap.clear()
            placedPosMap.clear()
            explodedPosMap.clear()
            attackedCrystalMap.clear()
            ignoredCrystalMap.clear()
            spawnTimeMap.clear()
            ddosQueue.clear()

            placeTimer.reset(-69420L)
            breakTimer.reset(-69420L)
            predictBreakTimer.reset(-69420L)

            selfMoving = false
            targetMoving = false
            forcePlacing = false
            lastRotation = null
            lastPlaced = null

            forcePlacing = false
            overridePos = null
            overrideTime = 0L

            crystalID.set(-1)
            lastCrystalID = -1
            breakCount = 0

            explosionTimer.reset(-69420L)
            explosionCount = 0

            inactiveTicks = 10
        }

        safeListener<InteractEvent.Block.RightClick> {
            if (manualOverride && player.getItemInHand(it.hand).item == Items.END_CRYSTAL) {
                overridePos = it.pos
                overrideTime = System.currentTimeMillis() + 1000L
            }
        }

        safeListener<CrystalSetDeadEvent> { event ->
            val time = System.currentTimeMillis() + spamTimeout
            event.crystals.forEach {
                explodedPosMap[it.blockPos.asLong()] = time
            }

            if (countAllCrystals && player.eyePosition.distanceSqTo(event.x, event.y, event.z) < breakRange.sq
                || placedPosMap.containsKey(
                    toLong(
                        event.x.floorToInt(),
                        (event.y - 1.0).floorToInt(),
                        event.z.floorToInt()
                    )
                )
            ) {
                explosionCount++
            }

            placedBBMap.clear()
            ignoredCrystalMap.clear()
            spawnTimeMap.clear()
            breakCount = 0
        }

        safeListener<CrystalSpawnEvent> { event ->
            if (packetBreak) {
                if (event.crystalDamage.eyeDistance > breakRange) return@safeListener
                if (!checkSelfCrystal(event.crystalDamage)) return@safeListener
                if (!noSuicideCheck(event.crystalDamage)) return@safeListener
                if (!checkDamageBreak(event.crystalDamage, BlockPos.MutableBlockPos())) return@safeListener
                if (!checkCrystalRotation(event.crystalDamage)) return@safeListener
                if (checkSlowMode(event.crystalDamage) && !breakTimer.tick(slowBreakDelay.toLong())) return@safeListener
                if (!preBreak(event.entityID)) return@safeListener

                breakDirect(event.entityID)
            }

            ddosQueue.peekFirst()?.let {
                if (event.crystalDamage.blockPos == it.blockPos) {
                    ddosQueue.pollFirst()
                }
            }

            placedBBMap.remove(event.crystalDamage.blockPos)
            spawnTimeMap[event.crystalDamage.blockPos.asLong()] = System.currentTimeMillis()
        }

        safeConcurrentListener<PacketEvent.PostSend> {
            val packet = it.packet
            if (packet is ServerboundPlayerActionPacket && packet.action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                val target = CombatManager.target ?: return@safeConcurrentListener
                val holeInfo = HoleManager.getHoleInfo(target)

                if (holeInfo.isHole && holeInfo.surroundPos.contains(packet.pos)) {
                    antiSurround(packet.pos, canSwap = false, placeOn = true, breakCrystal = false)
                }
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            if (!event.newState.isAir) return@safeListener

            val target = CombatManager.target ?: return@safeListener
            val holeInfo = HoleManager.getHoleInfo(target)

            if (holeInfo.isHole && holeInfo.surroundPos.contains(event.pos)) {
                antiSurround(event.pos, canSwap = true, placeOn = false, breakCrystal = true)
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet is ClientboundAddEntityPacket) {
                if (packet.type == EntityTypes.END_CRYSTAL
                    || packet.type == EntityTypes.PLAYER
                    || packet.type.category != net.minecraft.world.entity.MobCategory.MISC
                ) {
                    crystalID.getAndUpdate {
                        max(it, packet.id)
                    }
                } else {
                    crystalID.set(-1)
                    predictBreakTimer.reset(250L)
                }
            }
        }

        safeConcurrentListener<BlockBreakEvent> {
            if (it.progress < 6) return@safeConcurrentListener
            val target = CombatManager.target ?: return@safeConcurrentListener
            val holeInfo = HoleManager.getHoleInfo(target)

            if (holeInfo.isHole && holeInfo.surroundPos.contains(it.position)) {
                antiSurround(it.position, false, it.progress < 9, false)
            }
        }

        TimerScope.launchLooping(rootName, 5L) {
            if (isEnabled) {
                runSafe {
                    runLoop()
                }
            }
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (explosionTimer.tickAndReset(250L)) {
                val count = explosionCount
                explosionCount = 0
                explosionCountArray.add(count.toFloat())
            }

            runLoop()
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (!CombatManager.isOnTopPriority(TrollAura) || CombatSetting.pause) return@safeListener

            if (Bypass.crystalRotation) {
                doRotation()
            }
        }

        safeParallelListener<TickEvent.Pre> {
            inactiveTicks++
            updateTimeouts()
            updateDdosQueue()

            selfMoving = motionDetect && CombatManager.trackerSelf?.let { it.motion.length() > selfMotion } ?: false
            targetMoving =
                motionDetect && CombatManager.trackerTarget?.let { it.motion.length() > targetMotion } ?: false
            runLoop()
        }

        safeParallelListener<TickEvent.Post> {
            selfMoving = motionDetect && CombatManager.trackerSelf?.let { it.motion.length() > selfMotion } ?: false
            targetMoving =
                motionDetect && CombatManager.trackerTarget?.let { it.motion.length() > targetMotion } ?: false

            CombatManager.target?.let { target ->
                PacketMine.miningInfo?.let {
                    val holeInfo = HoleManager.getHoleInfo(target)
                    if (holeInfo.isHole && holeInfo.surroundPos.contains(it.pos)) {
                        antiSurround(it.pos, canSwap = false, placeOn = true, breakCrystal = false)
                    }
                }
            }

            runLoop()
        }
    }

    private fun SafeClientEvent.updateDdosQueue() {
        val target = CombatManager.target
        val mutableBlockPos = BlockPos.MutableBlockPos()

        ddosArmor = armorDdos && target != null && getNormalPos(false, mutableBlockPos).let {
            (it == null || it.blockPos != overridePos && it.targetDamage < placeMinDamage)
        }

        if (target == null || !ddosArmor) {
            ddosQueue.clear()
            return
        }

        val diff = System.currentTimeMillis() - CombatManager.getHurtTime(target)

        if (diff > 500L) {
            if (ddosArmor && ddosQueue.isEmpty() && (forcePlacing || checkForcePlaceArmor())) {
                val last = lastPlaced?.let { CombatManager.getCrystalDamage(it)?.targetDamage } ?: 0.0f

                if (last < placeMinDamage) {
                    val contextSelf = CombatManager.contextSelf ?: return
                    val contextTarget = CombatManager.contextTarget ?: return
                    val eyePos = player.eyePosition

                    val sequence = CombatManager.placeList.asSequence()
                        .filter { it.targetDamage > ddosMinDamage }
                        .filter { canPlaceCrystal(it.blockPos, contextTarget.entity as? net.minecraft.world.entity.LivingEntity) }
                        .filter {
                            checkPos(
                                contextSelf,
                                contextTarget,
                                mutableBlockPos,
                                eyePos,
                                it,
                                checkRotation = false,
                                checkColliding = false
                            )
                        }

                    ddosQueue.clear()
                    var lastDamage = Int.MAX_VALUE

                    for (crystalDamage in sequence) {
                        val roundedDamage = (crystalDamage.targetDamage / ddosDamageStep).roundToInt()
                        if (lastDamage == roundedDamage || lastDamage - roundedDamage < ddosDamageStep) continue
                        ddosQueue.addFirst(crystalDamage)
                        lastDamage = roundedDamage

                        if (ddosQueue.size >= ddosQueueSize) break
                    }
                }
            }
        } else if (diff > 450L) {
            ddosQueue.clear()
        }
    }

    private fun SafeClientEvent.doRotation() {
        var placing = inactiveTicks <= 5
        getPlacingPos(false, BlockPos.MutableBlockPos())?.blockPos?.let {
            lastRotation = it
            placing = true
        }

        if (placing) {
            lastRotation?.let {
                val side = getPlaceSide(it)
                val hitVec = getHitVec(it, side)
                val rotation = getRotationTo(hitVec)
                val diff = RotationUtils.calcAngleDiff(rotation.x, PlayerPacketManager.rotation.x)

                if (abs(diff) <= yawSpeed) {
                    sendPlayerPacket {
                        rotate(getRotationTo(it.toVec3d(0.5, 1.0, 0.5)))
                    }
                } else {
                    val clamped = diff.coerceIn(-yawSpeed, yawSpeed)
                    val newYaw = RotationUtils.normalizeAngle(PlayerPacketManager.rotation.x + clamped)

                    sendPlayerPacket {
                        rotate(Vec2f(newYaw, rotation.y))
                    }
                }
            }
        } else {
            lastRotation = null
        }
    }

    private fun updateTimeouts() {
        val removeTime = System.currentTimeMillis()

        if (System.currentTimeMillis() > overrideTime) {
            overridePos = null
            overrideTime = 0L
        }

        synchronized(placedBBMap) {
            placedBBMap.values.removeIf {
                it.second < removeTime
            }
        }

        synchronized(placedPosMap) {
            placedPosMap.values.removeIf {
                it < removeTime
            }
        }

        synchronized(explodedPosMap) {
            explodedPosMap.values.removeIf {
                it < removeTime
            }
        }

        synchronized(attackedCrystalMap) {
            attackedCrystalMap.values.removeIf {
                it < removeTime
            }
        }

        synchronized(ignoredCrystalMap) {
            ignoredCrystalMap.values.removeIf {
                it < removeTime
            }
        }
    }

    private fun SafeClientEvent.runLoop() {
        CombatManager.target?.let { target ->
            if (!CombatSetting.pause && target.isAlive && (!ddosArmor || System.currentTimeMillis() - CombatManager.getHurtTime(
                    target
                ) !in 450L..500L)
            ) {
                doBreak()
                doPlace()
            }
        }
    }

    private fun SafeClientEvent.doBreak() {
        if (doBreak && breakTimer.tick(breakDelay.toLong())) {
            getBreakCrystal()?.let { (crystal, crystalDamage) ->
                if (checkSlowMode(crystalDamage) && !breakTimer.tick(slowBreakDelay.toLong())) return
                if (!preBreak(crystal.id)) return
                breakDirect(crystal.id)
            }
        }
    }

    private fun SafeClientEvent.preBreak(entityID: Int): Boolean {
        if (antiWeakness && player.isWeaknessActive() && !isHoldingTool()) {
            if (HandPause[InteractionHand.MAIN_HAND].requestPause(TrollAura, (swapDelay + 1) * 50)) {
                equipBestWeapon(allowTool = true)
            }
            return false
        }

        // Anticheat doesn't allow you attack right after changing item
        if ((!autoSwap || !spoofHotbar) && System.currentTimeMillis() - HotbarSwitchManager.swapTime < swapDelay * 50L) {
            return false
        }

        if (breakAttempts != 0 && entityID == lastCrystalID) {
            if (breakCount > breakAttempts) {
                ignoredCrystalMap[entityID] = System.currentTimeMillis() + retryTimeout
                attackedCrystalMap.remove(entityID)
                lastCrystalID = -1
                breakCount = 0
                return false
            }
        } else {
            breakCount = 0
        }

        lastCrystalID = entityID
        breakCount++

        return true
    }

    private fun LocalPlayer.isWeaknessActive(): Boolean {
        return this.hasEffect(MobEffects.WEAKNESS)
            && this.getEffect(MobEffects.STRENGTH)?.let {
            it.amplifier > 0
        } != true
    }

    private fun SafeClientEvent.breakDirect(entityID: Int) {
        breakTimer.reset()
        inactiveTicks = 0
        CombatManager.target?.let { target -> player.setLastHurtMob(target) }
        attackedCrystalMap[entityID] = System.currentTimeMillis() + breakTimeout

        connection.send(ServerboundAttackPacket(entityID))
        swingHand()
    }

    private fun SafeClientEvent.swingHand() {
        val hand = when (swingHand) {
            SwingHand.AUTO -> if (player.offhandItem.item == Items.END_CRYSTAL) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
            SwingHand.OFF_HAND -> InteractionHand.OFF_HAND
            SwingHand.MAIN_HAND -> InteractionHand.MAIN_HAND
        }

        swingMode.swingHand(this, hand)
    }
    /* End of main functions */

    /* Placing */
    private fun SafeClientEvent.antiSurround(
        surroundPos: BlockPos,
        canSwap: Boolean,
        placeOn: Boolean,
        breakCrystal: Boolean
    ) {
        if (!canPlace()) return

        ConcurrentScope.launch {
            val mutableBlockPos = BlockPos.MutableBlockPos()
            var pos = getAntiSurroundPos(surroundPos, mutableBlockPos)

            if (pos != null) {
                if (breakCrystal) antiSurroundBreakCrystal(pos)
            } else {
                pos = getAlternativePos(surroundPos, placeOn, mutableBlockPos)
            }

            if (pos != null) {
                antiSurroundDirect(pos, canSwap)
            }
        }
    }

    private fun SafeClientEvent.getAntiSurroundPos(
        surroundPos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): BlockPos? {
        return antiSurroundOffset.asSequence()
            .map(surroundPos::offset)
            .filter { CrystalUtils.isValidMaterial(world.getBlockState(mutableBlockPos.setAndAdd(it, 0, 1, 0))) }
            .mapNotNull { checkAntiSurroundPos(surroundPos, it, mutableBlockPos) }
            .maxByOrNull { it.targetDamage }
            ?.blockPos
    }

    private fun SafeClientEvent.getAlternativePos(
        surroundPos: BlockPos,
        placeOn: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): BlockPos? {
        if (placeOn) {
            if (CrystalUtils.isValidMaterial(world.getBlockState(mutableBlockPos.setAndAdd(surroundPos, 0, 1, 0)))
                && checkAntiSurroundPos(surroundPos, surroundPos, mutableBlockPos) != null
            ) {
                return surroundPos
            }
        } else {
            val posDown = surroundPos.below()
            if (checkAntiSurroundPos(surroundPos, posDown, mutableBlockPos) != null) {
                return posDown
            }
        }
        return null
    }

    private fun SafeClientEvent.antiSurroundBreakCrystal(pos: BlockPos) {
        for ((crystal, crystalDamage) in CombatManager.crystalList) {
            if (abs(crystalDamage.blockPos.x - pos.x) > 1
                || abs(crystalDamage.blockPos.y + 1 - pos.y) > 1
                || abs(crystalDamage.blockPos.z - pos.z) > 1
            ) continue

            if (!checkCrystalRotation(crystalDamage)) continue
            if (!preBreak(crystal.id)) return
            breakDirect(crystal.id)

            return
        }
    }

    private fun SafeClientEvent.checkAntiSurroundPos(
        surroundPos: BlockPos,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage? {
        if (!canPlaceCrystalOn(pos)) return null
        if (!CrystalUtils.isValidMaterial(world.getBlockState(pos.above(2)))) return null

        val function = FastRayTraceFunction { rayTracePos, blockState ->
            if (rayTracePos != surroundPos && !blockState.isAir && CrystalUtils.isResistant(blockState)) {
                FastRayTraceAction.CALC
            } else {
                FastRayTraceAction.SKIP
            }
        }

        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null

        val crystalDamage = CombatManager.getCrystalDamage(pos) ?: return null
        val selfDamage = contextSelf.calcDamage(crystalDamage.crystalPos, false, mutableBlockPos, function)
        val targetDamage = contextTarget.calcDamage(crystalDamage.crystalPos, true, mutableBlockPos, function)
        val newCrystalDamage = CrystalDamage(
            crystalDamage.crystalPos,
            crystalDamage.blockPos,
            selfDamage,
            targetDamage,
            crystalDamage.eyeDistance,
            crystalDamage.feetDistance
        )

        return newCrystalDamage.takeIf {
            checkPos(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                player.eyePosition,
                it,
                checkRotation = false,
                checkColliding = false
            )
                && checkDamagePlace(it)
        }
    }

    private fun SafeClientEvent.antiSurroundDirect(pos: BlockPos, canSwap: Boolean) {
        placeDirect(canSwap || (autoSwap && spoofHotbar), pos)

        overridePos = pos
        overrideTime = System.currentTimeMillis() + 1000L
    }

    private fun SafeClientEvent.doPlace() {
        if (!canPlace()) return
        val mutableBlockPos = BlockPos.MutableBlockPos()
        val crystalDamage = getPlacingPos(Bypass.crystalRotation, mutableBlockPos) ?: return
        if (crystalDamage.blockPos != overridePos
            && checkSlowMode(crystalDamage)
            && !placeTimer.tick(slowPlaceDelay.toLong())
        ) return

        if (!prePlace(crystalDamage.blockPos, mutableBlockPos)) return

        placeDirect(true, crystalDamage.blockPos)
    }

    private fun SafeClientEvent.placeDirect(canSwap: Boolean, pos: BlockPos) {
        val hand = getHandNullable()

        if (hand == null) {
            if (canSwap && autoSwap) {
                val slot = player.getCrystalSlot() ?: return
                if (!spoofHotbar) {
                    MainHandPause.withPause(TrollAura, placeDelay * 2) {
                        swapToSlot(slot)
                        sendPlacePacket(pos, InteractionHand.MAIN_HAND)
                    }
                } else {
                    ghostSwitch(slot) {
                        sendPlacePacket(pos, InteractionHand.MAIN_HAND)
                    }
                }
            } else {
                return
            }
        } else {
            HandPause[hand].withPause(TrollAura, 1000L) {
                sendPlacePacket(pos, hand)
            }
        }

        inactiveTicks = 0
        placeTimer.reset()
        lastPlaced = pos
        if (placeSwing) swingHand()
        if (predictBreak > 0) predictBreak(pos)

        val current = System.currentTimeMillis()
        if (!spawnTimeMap.containsKey(pos.asLong())) placedBBMap[pos] =
            CrystalUtils.getCrystalBB(pos) to current + placeTimeout
        placedPosMap[pos.asLong()] = current + 1000L
    }

    private fun SafeClientEvent.sendPlacePacket(pos: BlockPos, hand: InteractionHand) {
        val hitResult = if (buildLimitBypass) {
            BlockHitResult(
                Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5),
                Direction.DOWN,
                pos,
                false
            )
        } else {
            val side = getPlaceSide(pos)
            val hitVecOffset = getHitVecOffset(side)
            BlockHitResult(
                Vec3(
                    pos.x + hitVecOffset.x.toDouble(),
                    pos.y + hitVecOffset.y.toDouble(),
                    pos.z + hitVecOffset.z.toDouble()
                ),
                side,
                pos,
                false
            )
        }

        world.withPrediction { sequence ->
            connection.send(ServerboundUseItemOnPacket(hand, hitResult, sequence))
        }
    }

    private fun SafeClientEvent.predictBreak(pos: BlockPos) {
        val id = crystalID.get()
        if (id != -1 && predictBreakTimer.tick(predictBreakDelay.toLong())) {
            val spawnTime = getSpawnTime(pos)
            if (predictTimeout == 0 || spawnTime < predictTimeout) {
                for (i in 0 until predictBreak) {
                    connection.send(ServerboundAttackPacket(id + i + 1))
                }
                predictBreakTimer.reset()
            }
        }
    }

    private fun SafeClientEvent.canPlace() =
        doPlace && placeTimer.tick(placeDelay.toLong()) && player.allSlots.countItem(Items.END_CRYSTAL) > 0

    private fun LocalPlayer.getCrystalSlot() =
        this.hotbarSlots.firstItem(Items.END_CRYSTAL)

    private fun SafeClientEvent.getPlacingPos(
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage? {
        val override = getOverridePos(checkRotation, mutableBlockPos)
        if (override != null) return override

        val normal = getNormalPos(checkRotation, mutableBlockPos)
        return if (armorDdos && (normal == null || normal.blockPos != overridePos && normal.targetDamage < placeMinDamage)) {
            ddosQueue.peekFirst()
        } else {
            normal
        }
    }

    private fun SafeClientEvent.getOverridePos(
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage? {
        val pos = overridePos ?: return null
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null

        return CombatManager.placeMap[pos]?.takeIf {
            canPlaceCrystal(it.blockPos, null)
                && checkPos(contextSelf, contextTarget, mutableBlockPos, player.eyePosition, it, checkRotation, true)
        }
    }

    private fun SafeClientEvent.getNormalPos(
        checkRotation: Boolean,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage? {
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null
        val list = CombatManager.placeList
        if (list.isEmpty()) return null

        val eyePos = player.eyePosition

        for (crystalDamage in list) {
            // Damage check
            if (!checkDamagePlace(crystalDamage)) continue

            if (contextTarget.entity is Player) {
                val current = System.currentTimeMillis()
                val totemPopTracker = TotemPopManager.getTracker(contextTarget.entity)
                val healthTracker = HealthManager.getTracker(contextTarget.entity as Player)

                if (totemPopTracker != null
                    && current - totemPopTracker.popTime < 2000L
                    && current - healthTracker.hurtTime > 300L
                ) {
                    if (crystalDamage.targetDamage - contextTarget.entity.totalHealth > 0.25f) {
                        continue
                    }
                }
            }

            if (!checkPos(
                    contextSelf,
                    contextTarget,
                    mutableBlockPos,
                    eyePos,
                    crystalDamage,
                    checkRotation,
                    true
                )
            ) continue

            return crystalDamage
        }

        return null
    }

    private fun SafeClientEvent.checkPos(
        contextSelf: CalcContext,
        contextTarget: CalcContext,
        mutableBlockPos: BlockPos.MutableBlockPos,
        eyePos: Vec3,
        crystalDamage: CrystalDamage,
        checkRotation: Boolean,
        checkColliding: Boolean
    ): Boolean {
        if (!noSuicideCheck(crystalDamage)) return false
        if (!placeDistCheck(eyePos, crystalDamage, mutableBlockPos)) return false

        if (checkRotation) {
            val box = AABB(crystalDamage.blockPos)
            val eyePos2 = PlayerPacketManager.position.add(0.0, player.eyeHeight.toDouble(), 0.0)

            if (box.isInSight(eyePos2, PlayerPacketManager.rotation, 8.0)
                && (placeRotationRange == 0.0f || RotationUtils.getRotationDiff(
                    getRotationTo(
                        eyePos2,
                        crystalDamage.crystalPos
                    ), PlayerPacketManager.rotation
                ) > placeRotationRange)
            ) {
                return false
            }
        }

        if (!contextSelf.checkColliding(crystalDamage.crystalPos)) return false
        if (!contextTarget.checkColliding(crystalDamage.crystalPos)) return false
        return !(checkColliding && !placeSyncCheck(crystalDamage.blockPos))
    }

    private fun SafeClientEvent.placeDistCheck(
        eyePos: Vec3,
        crystalDamage: CrystalDamage,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return crystalDamage.eyeDistance <= placeRange && (crystalDamage.feetDistance <= wallPlaceRange
            || world.rayTraceVisible(eyePos, crystalDamage.crystalPos.add(0.0, 1.7, 0.0), 20, mutableBlockPos))
    }

    private fun placeSyncCheck(pos: BlockPos): Boolean {
        return pos == overridePos || when (placeMode) {
            PlaceSyncMode.NORMAL -> {
                CrystalUtils.placeCollideCheck(pos)
            }
            PlaceSyncMode.SPAM, PlaceSyncMode.IGNORE -> {
                val empty = inactiveTicks > 5 && explodedPosMap.isEmpty()
                CrystalUtils.placeCollideCheck(pos) { entity ->
                    entity is EndCrystal
                        && !ignoredCrystalMap.containsKey(entity.id)
                        && entity.blockPos.let {
                        it == pos
                            && (empty
                            || attackedCrystalMap.containsKey(entity.id)
                            || explodedPosMap.containsKey(it.asLong()))
                    }
                }
            }
            PlaceSyncMode.MULTI -> {
                !placedBBMap.containsKey(pos) && synchronized(placedBBMap) {
                    val box = CrystalUtils.getCrystalPlacingBB(pos)
                    placedBBMap.values.all { !it.first.intersects(box) }
                } && CrystalUtils.placeCollideCheck(pos)
            }
        }
    }

    /**
     * @return True if passed placing damage check
     */
    private fun checkDamagePlace(crystalDamage: CrystalDamage): Boolean {
        return lethalPlace && lethalCheck(crystalDamage)
            || compareLessEqual(selfMoving, crystalDamage.selfDamage, placeMaxSelfDamage, motionPlaceMaxSelfDamage)
            && (compareGreatEqual(targetMoving, crystalDamage.targetDamage, placeMinDamage, motionPlaceMinDamage)
            && compareGreatEqual(
            targetMoving && selfMoving,
            crystalDamage.damageBalance,
            placeBalance,
            motionPlaceBalance
        )
            || shouldFacePlace(crystalDamage))
    }

    private fun SafeClientEvent.prePlace(pos: BlockPos, mutableBlockPos: BlockPos.MutableBlockPos): Boolean {
        val mode = placeMode
        val spam = mode == PlaceSyncMode.SPAM
        val ignore = mode == PlaceSyncMode.IGNORE

        val exploded = explodedPosMap.containsKey(pos.asLong())
        val eyePos = player.eyePosition
        var count = 0

        for ((crystal, crystalDamage) in CombatManager.crystalList) {
            if (!crystal.isAlive) continue
            if (ignoredCrystalMap.containsKey(crystal.id)) continue

            if (crystalDamage.blockPos == pos) {
                if (spam) {
                    return true
                } else if (ignore) {
                    return attackedCrystalMap.containsKey(crystal.id)
                        || getSpawnTime(crystal) >= spawnTimeout
                }
            }

            if (!checkDamagePlace(crystalDamage)) continue
            if (!placeDistCheck(eyePos, crystalDamage, mutableBlockPos)) continue
            if (!checkCrystalRotation(crystalDamage)) continue

            count++
        }

        synchronized(placedBBMap) {
            for ((placedPos, _) in placedBBMap) {
                if (placedPos == pos && exploded && mode != PlaceSyncMode.MULTI) continue
                val crystalDamage = CombatManager.placeMap[placedPos] ?: continue
                if (!checkDamagePlace(crystalDamage)) continue
                if (!placeDistCheck(eyePos, crystalDamage, mutableBlockPos)) continue
                if (!checkCrystalRotation(crystalDamage)) continue

                count++
            }
        }

        return count < maxCrystal
    }

    private fun SafeClientEvent.getPlaceSide(pos: BlockPos): Direction {
        return if (strictDirection) {
            getMiningSide(pos) ?: Direction.UP
        } else {
            Direction.UP
        }
    }

    private fun getSpawnTime(crystal: EndCrystal): Long {
        return getSpawnTime(crystal.blockPos)
    }

    private fun getSpawnTime(pos: BlockPos): Long {
        return System.currentTimeMillis() - spawnTimeMap[pos.asLong()]
    }
    /* End of placing */

    private fun SafeClientEvent.getBreakCrystal(): Pair<EndCrystal, CrystalDamage>? {
        val eyePos = player.eyePosition
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return CombatManager.crystalList.asSequence()
            .filter { it.first.isAlive }
            .filter { checkSelfCrystal(it.second) }
            .filterNot { ignoredCrystalMap.containsKey(it.first.id) }
            .filter { checkDamageBreak(it.second, mutableBlockPos) }
            .filter { checkCrystalRotation(it.second) }
            .filterNot { spawnDelay > 0 && getSpawnTime(it.first) < spawnDelay }
            .filter { it.second.eyeDistance <= breakRange }
            .filter {
                it.second.feetDistance <= wallBreakRange || world.rayTraceVisible(
                    eyePos,
                    it.first.eyePosition,
                    20,
                    mutableBlockPos
                )
            }
            .firstOrNull()
    }

    private fun SafeClientEvent.checkCrystalRotation(crystalDamage: CrystalDamage): Boolean {
        if (!Bypass.crystalRotation) return true

        val box = CrystalUtils.getCrystalBB(crystalDamage.blockPos)
        val eyePos = PlayerPacketManager.position.add(0.0, player.eyeHeight.toDouble(), 0.0)
        val rotationToCenter = getRotationTo(PlayerPacketManager.position, box.center)

        return box.isInSight(eyePos, PlayerPacketManager.rotation, 8.0)
            || RotationUtils.getRotationDiff(rotationToCenter, PlayerPacketManager.rotation) <= breakRotationRange
    }

    private fun checkSelfCrystal(crystalDamage: CrystalDamage): Boolean {
        return !ownCrystal || (placedPosMap.isEmpty() || placedPosMap.containsKey(crystalDamage.blockPos.asLong()))
    }

    private fun SafeClientEvent.checkDamageBreak(
        crystalDamage: CrystalDamage,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        return lethalBreak && lethalCheck(crystalDamage)
            || compareLessEqual(selfMoving, crystalDamage.selfDamage, breakMaxSelfDamage, motionBreakMaxSelfDamage)
            && (compareGreatEqual(targetMoving, crystalDamage.targetDamage, breakMinDamage, motionBreakMinDamage)
            && compareGreatEqual(
            targetMoving && selfMoving,
            crystalDamage.damageBalance,
            breakBalance,
            motionBreakBalance
        )
            || shouldFacePlace(crystalDamage)
            || shouldForceBreak(crystalDamage, mutableBlockPos))
    }

    private fun SafeClientEvent.shouldForceBreak(
        crystalDamage: CrystalDamage,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Boolean {
        val contextSelf = CombatManager.contextSelf ?: return false
        val contextTarget = CombatManager.contextTarget ?: return false

        val crystalBB = CrystalUtils.getCrystalBB(crystalDamage.blockPos)
        val eyePos = player.eyePosition
        return CombatManager.placeList.any {
            checkDamagePlace(it)
                && crystalBB.intersects(CrystalUtils.getCrystalPlacingBB(it.blockPos))
                && checkPos(
                contextSelf,
                contextTarget,
                mutableBlockPos,
                eyePos,
                it,
                checkRotation = false,
                checkColliding = false
            )
        }
    }
    /* End of exploding */

    /* General */
    private fun SafeClientEvent.getHandNullable(): InteractionHand? {
        return when (Items.END_CRYSTAL) {
            player.offhandItem.item -> InteractionHand.OFF_HAND
            player.serverSideItem.item -> InteractionHand.MAIN_HAND
            else -> null
        }
    }

    private fun SafeClientEvent.noSuicideCheck(crystalDamage: CrystalDamage): Boolean {
        return player.scaledHealth - crystalDamage.selfDamage > noSuicide
    }

    private fun lethalCheck(crystalDamage: CrystalDamage): Boolean {
        return CombatManager.target?.let { crystalDamage.targetDamage > it.scaledHealth } ?: false
    }

    private fun SafeClientEvent.isHoldingTool(): Boolean {
        val item = player.mainHandItem.item
        return item.isTool || item.isWeapon
    }

    private fun shouldFacePlace(crystalDamage: CrystalDamage) =
        crystalDamage.targetDamage >= forcePlaceMinDamage
            && crystalDamage.damageBalance >= forcePlaceDamageBalance
            && (forcePlacing || checkForcePlaceTotemPop() || checkForcePlaceArmorHealth())

    private fun checkForcePlaceArmorHealth(): Boolean {
        return (forcePlaceHealth > 0.0f && CombatManager.target?.let { it.scaledHealth <= forcePlaceHealth } ?: false)
            || checkForcePlaceArmor()
    }

    private fun checkForcePlaceTotemPop(): Boolean {
        return CombatManager.target?.let { target ->
            TotemPopManager.getTracker(target)?.let {
                System.currentTimeMillis() - it.popTime < 2000L
            }
        } ?: false
    }

    private fun checkForcePlaceArmor(): Boolean {
        return forcePlaceArmorDura > 0.0f && getMinArmorDura() <= forcePlaceArmorDura
    }

    private val armorEquipmentSlots = arrayOf(
        net.minecraft.world.entity.EquipmentSlot.FEET,
        net.minecraft.world.entity.EquipmentSlot.LEGS,
        net.minecraft.world.entity.EquipmentSlot.CHEST,
        net.minecraft.world.entity.EquipmentSlot.HEAD
    )

    private fun getMinArmorDura(): Int {
        val target = CombatManager.target ?: return 100
        return armorEquipmentSlots.asSequence()
            .map { target.getItemBySlot(it) }
            .filter { it.isDamageableItem }
            .maxByOrNull { it.damageValue }
            ?.duraPercentage
            ?: 100
    }

    private fun checkSlowMode(crystalDamage: CrystalDamage): Boolean {
        return slowMode
            && !ddosArmor
            && !selfMoving
            && !targetMoving
            && crystalDamage.targetDamage < slowDamage && !checkForcePlaceArmorHealth()
    }

    private fun compareLessEqual(allowB2: Boolean, a: Float, b1: Float, b2: Float): Boolean {
        return a <= b1 || allowB2 && a <= b2
    }

    private fun compareGreatEqual(allowB2: Boolean, a: Float, b1: Float, b2: Float): Boolean {
        return a >= b1 || allowB2 && a >= b2
    }
    /* End of general */
}
