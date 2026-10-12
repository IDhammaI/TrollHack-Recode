package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.collection.CircularArray
import dev.fastmc.common.collection.CircularArray.Companion.average
import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.CombatEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.EntityUtils.spoofUnSneak
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.craftingSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstByStack
import dev.luna5ama.trollhack.util.inventory.slot.hasItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.inventory.slot.storageSlots
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.RotationUtils.yaw
import dev.luna5ama.trollhack.util.math.VectorUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.math.vector.toVec3dCenter
import dev.luna5ama.trollhack.util.pause.OffhandPause
import dev.luna5ama.trollhack.util.pause.withPause
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.TimerScope
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.FastRayTraceAction
import dev.luna5ama.trollhack.util.world.FastRayTraceFunction
import dev.luna5ama.trollhack.util.world.getBlock
import dev.luna5ama.trollhack.util.world.getMiningSide
import dev.luna5ama.trollhack.util.world.getHitVecOffset
import dev.luna5ama.trollhack.util.world.isFullBox
import dev.luna5ama.trollhack.util.accessor.withPrediction
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BedItem
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@CombatManager.CombatModule
internal object BedAura : Module(
    name = "Bed Aura",
    description = "Place bed and kills enemies",
    category = Category.COMBAT,
    modulePriority = 70
) {
    private val page = setting("Page", Page.GENERAL)

    private val handMode by setting("Hand Mode", InteractionHand.OFF_HAND, page.atValue(Page.GENERAL))
    private val rotationPitch by setting("Rotation Pitch", 90, -90..90, 1, page.atValue(Page.GENERAL))
    private val ghostSwitchBypass by setting(
        "Ghost Switch Bypass",
        HotbarSwitchManager.Override.NONE,
        page.atValue(Page.GENERAL) and ::handMode.atValue(InteractionHand.MAIN_HAND)
    )
    private val bedSlot by setting(
        "Bed Slot",
        3,
        1..9,
        1,
        page.atValue(Page.GENERAL) and { handMode == InteractionHand.MAIN_HAND })
    private val assumeInstantMine by setting("Assume Instant Mine", true, page.atValue(Page.GENERAL))
    private val antiBlocker by setting("Anti Blocker", true, page.atValue(Page.GENERAL))
    private val antiBlockerSwitch by setting(
        "Anti Blocker Switch",
        200,
        0..500,
        10,
        page.atValue(Page.GENERAL) and ::antiBlocker
    )
    private val strictDirection by setting("Strict Direction", false, page.atValue(Page.GENERAL))
    private val newPlacement by setting("1.13 Placement", true, page.atValue(Page.GENERAL))
    private val smartDamage by setting("Smart Damage", true, page.atValue(Page.GENERAL))
    private val damageStep by setting(
        "Damage Step",
        2.0f,
        0.0f..5.0f,
        0.1f,
        page.atValue(Page.GENERAL) and ::smartDamage
    )
    private val noSuicide by setting("No Suicide", 8.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    private val minDamage by setting("Min Damage", 6.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    private val maxSelfDamage by setting("Max Self Damage", 6.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    private val damageBalance by setting("Damage Balance", -2.5f, -10.0f..10.0f, 0.25f, page.atValue(Page.GENERAL))
    private val range by setting("Range", 5.4f, 0.0f..6.0f, 0.25f, page.atValue(Page.GENERAL))

    private val updateDelay by setting("Update Delay", 50, 5..250, 1, page.atValue(Page.TIMING))
    private val timingMode by setting("Timing Mode", TimingMode.INSTANT, page.atValue(Page.TIMING))
    private val delay by setting(
        "Delay",
        75,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { timingMode != TimingMode.SWITCH })
    private val placeDelay by setting(
        "Place Delay",
        25,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { timingMode == TimingMode.SWITCH })
    private val breakDelay by setting(
        "Break Delay",
        50,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { timingMode == TimingMode.SWITCH })
    private val slowMode by setting("Slow Mode", true, page.atValue(Page.TIMING))
    private val slowModeDamage by setting(
        "Slow Mode Damage",
        4.0f,
        0.0f..10.0f,
        0.25f,
        page.atValue(Page.TIMING) and ::slowMode
    )
    private val slowDelay by setting(
        "Slow Delay",
        250,
        0..1000,
        5,
        page.atValue(Page.TIMING) and ::slowMode and { timingMode != TimingMode.SWITCH })
    private val slowPlaceDelay by setting(
        "Slow Place Delay",
        250,
        0..1000,
        5,
        page.atValue(Page.TIMING) and ::slowMode and { timingMode == TimingMode.SWITCH })
    private val slowBreakDelay by setting(
        "Slow Break Delay",
        50,
        0..1000,
        5,
        page.atValue(Page.TIMING) and ::slowMode and { smartDamage || timingMode == TimingMode.SWITCH })

    private val forcePlaceBind by setting("Force Place Bind", Bind(), {
        if (isEnabled && it) {
            toggleForcePlace = !toggleForcePlace
            NoSpamMessageProxy.send("$chatName Force placing" + if (toggleForcePlace) " §aenabled" else " §cdisabled")
        }
    }, page.atValue(Page.FORCE_PLACE))
    private val forcePlaceHealth by setting(
        "Force Place Health",
        8.0f,
        0.0f..20.0f,
        0.5f,
        page.atValue(Page.FORCE_PLACE)
    )
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
    private val motionMinDamage by setting(
        "Motion Min Damage",
        3.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionMaxSelfDamage by setting(
        "Motion Max Self Damage",
        8.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionDamageBalance by setting(
        "Motion Damage Balance",
        -5.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })

    private val renderFoot by setting("Render Foot", true, page.atValue(Page.RENDER))
    private val renderHead by setting("Render Head", true, page.atValue(Page.RENDER))
    private val footColor by setting(
        "Foot Color",
        ColorRGB(255, 160, 255),
        false,
        page.atValue(Page.RENDER) and ::renderFoot
    )
    private val headColor by setting(
        "Head Color",
        ColorRGB(255, 32, 64),
        false,
        page.atValue(Page.RENDER) and ::renderHead
    )
    private val fadeLength by setting("Fade Length", 250, 0..1000, 50, page.atValue(Page.RENDER))

    private enum class Page {
        GENERAL, TIMING, FORCE_PLACE, MOTION_DETECT, RENDER
    }

    private enum class TimingMode {
        INSTANT, SYNC, SWITCH
    }

    private object NoSpamMessageProxy {
        fun send(message: String) {
            dev.luna5ama.trollhack.util.text.NoSpamMessage.sendMessage(BedAura, message)
        }
    }

    private val updateTimer = TickTimer()
    private val timer = TickTimer()
    private val blockerExists = AtomicBoolean(true)
    private val blockerSwitch = AtomicBoolean(false)
    private val blockerTimer = TickTimer()

    private var switchPlacing = false
    private var placeInfo: PlaceInfo? = null
    private var selfMoving = false
    private var targetMoving = false
    private var toggleForcePlace = false
    private var shouldForcePlace = false
    private var lastDamage = 0.0f
    private var lastTask: InventoryTask? = null

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray(8, 0.0f)
    private var explosionCount = 0

    private var inactiveTicks = 10
    var needOffhandBed = false; private set

    private var renderStartTime = 0L

    private val function = FastRayTraceFunction { pos, blockState ->
        val block = blockState.block
        if (blockState.isAir
            || block is BedBlock
            || assumeInstantMine && PacketMine.isInstantMining(pos)
            || !CrystalUtils.isResistant(blockState)
        ) {
            FastRayTraceAction.SKIP
        } else {
            FastRayTraceAction.CALC
        }
    }

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks < 10
    }

    override fun getHudInfo(): String {
        return "%.1f".format(explosionCountArray.average() * 4.0)
    }

    init {
        onDisable {
            reset()
        }

        safeListener<PacketEvent.Receive>(114514) {
            val target = CombatManager.target ?: return@safeListener
            when (val packet = it.packet) {
                is ClientboundSoundPacket -> {
                    val placeInfo = placeInfo ?: return@safeListener
                    if (packet.source != SoundSource.BLOCKS) return@safeListener
                    if (packet.sound.value() != SoundEvents.GENERIC_EXPLODE.value()) return@safeListener
                    if (placeInfo.center.distanceSqTo(packet.x, packet.y, packet.z) > 4.0) return@safeListener

                    explosionCount++
                }
                is ClientboundBlockUpdatePacket -> {
                    val targetPos = target.betterPosition
                    if (packet.pos != targetPos) return@safeListener
                    if (CrystalUtils.isResistant(packet.blockState)) return@safeListener

                    blockerExists.set(true)
                }
            }
        }

        listener<Render3DEvent> { event ->
            val placeInfo = placeInfo

            if (placeInfo != null && renderStartTime == 0L) {
                renderStartTime = System.currentTimeMillis()
            } else if (placeInfo == null) {
                renderStartTime = 0L
            }

            if (placeInfo != null && (renderFoot || renderHead)) {
                val scale = Easing.OUT_CUBIC.inc(Easing.toDelta(renderStartTime, fadeLength))
                val aFilled = (32.0f * scale).toInt()
                val aOutline = (233.0f * scale).toInt()

                if (renderFoot) {
                    val box = AABB(placeInfo.bedPosFoot).contract(0.0, 0.4375, 0.0)
                    event.renderer.boxFill(box, footColor.alpha(aFilled))
                    event.renderer.boxOutline(box, footColor.alpha(aOutline), 2.0f)
                }
                if (renderHead) {
                    val box = AABB(placeInfo.bedPosHead).contract(0.0, 0.4375, 0.0)
                    event.renderer.boxFill(box, headColor.alpha(aFilled))
                    event.renderer.boxOutline(box, headColor.alpha(aOutline), 2.0f)
                }
            }
        }

        listener<EntityEvent.UpdateHealth> {
            if (it.entity == CombatManager.target) {
                val diff = it.prevHealth - it.health
                if (diff > 0.0f) {
                    lastDamage += diff
                }
            }
        }

        listener<CombatEvent.UpdateTarget> {
            lastDamage = 0.0f
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            placeInfo?.let {
                val rotation = if (Bypass.blockPlaceRotation) {
                    getRotationTo(it.hitVec)
                } else {
                    Vec2f(it.direction.yaw, rotationPitch.toFloat())
                }

                sendPlayerPacket {
                    rotate(rotation)
                }
            }
        }

        safeListener<TickEvent.Post> {
            inactiveTicks++
            update()
            runLoop()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (explosionTimer.tickAndReset(250L)) {
                val count = explosionCount
                explosionCount = 0
                explosionCountArray.add(count.toFloat())
            }

            update()
            runLoop()
        }

        TimerScope.launchLooping(rootName, 5L) {
            if (isEnabled) {
                runSafe {
                    runLoop()
                }
            }
        }
    }

    private fun SafeClientEvent.runLoop() {
        val placeInfo = placeInfo
        if (placeInfo == null || CombatSetting.pause) {
            needOffhandBed = false
            return
        } else {
            needOffhandBed = handMode == InteractionHand.OFF_HAND
        }

        if (handMode == InteractionHand.MAIN_HAND) {
            if (!lastTask.executedOrTrue) return
            val hotbarSlot = player.hotbarSlots[bedSlot - 1]
            if (hotbarSlot.item.item !is BedItem) {
                refillBed(hotbarSlot)
                return
            }
        } else {
            if (player.offhandItem.item !is BedItem) return
        }

        val validDamage = !smartDamage || shouldForcePlace || placeInfo.targetDamage - lastDamage >= damageStep

        when (timingMode) {
            TimingMode.INSTANT -> instantTiming(placeInfo, validDamage)
            TimingMode.SYNC -> syncTiming(placeInfo, validDamage)
            TimingMode.SWITCH -> switchTiming(placeInfo, validDamage)
        }
    }

    private fun SafeClientEvent.refillBed(hotbarSlot: HotbarSlot) {
        (player.storageSlots.firstByStack { it.item is BedItem }
            ?: player.craftingSlots.firstByStack { it.item is BedItem })?.let {
            lastTask = inventoryTask {
                swapWith(it, hotbarSlot)
            }
        }
    }

    private fun SafeClientEvent.instantTiming(placeInfo: PlaceInfo, validDamage: Boolean) {
        if (validDamage) {
            if (timer.tick(getDelay(placeInfo, delay, slowDelay).toLong())) {
                placeBed(placeInfo)
                breakBed(placeInfo)
            }
        } else {
            breakIfPlaced(placeInfo, getDelay(placeInfo, delay, slowDelay))
        }
    }

    private fun SafeClientEvent.syncTiming(placeInfo: PlaceInfo, validDamage: Boolean) {
        if (validDamage) {
            if (timer.tick(getDelay(placeInfo, delay, slowDelay).toLong())) {
                if (isBedPlaced(placeInfo)) {
                    breakBed(placeInfo)
                } else {
                    placeBed(placeInfo)
                    breakBed(placeInfo)
                }
            }
        } else {
            breakIfPlaced(placeInfo, getDelay(placeInfo, delay, slowDelay))
        }
    }

    private fun SafeClientEvent.switchTiming(placeInfo: PlaceInfo, validDamage: Boolean) {
        if (validDamage) {
            if (switchPlacing) {
                if (timer.tick(getDelay(placeInfo, placeDelay, slowPlaceDelay).toLong())) {
                    breakBed(placeInfo)
                    switchPlacing = !switchPlacing
                }
            } else {
                if (timer.tick(getDelay(placeInfo, breakDelay, slowBreakDelay).toLong())) {
                    placeBed(placeInfo)
                    switchPlacing = !switchPlacing
                }
            }
        } else {
            breakIfPlaced(placeInfo, getDelay(placeInfo, breakDelay, slowBreakDelay))
        }
    }

    private fun SafeClientEvent.breakIfPlaced(placeInfo: PlaceInfo, delay: Int) {
        if (timer.tick(delay.toLong())) {
            if (isBedPlaced(placeInfo)) {
                breakBed(placeInfo)
            }
        }
    }

    private fun SafeClientEvent.isBedPlaced(placeInfo: PlaceInfo): Boolean {
        return world.getBlock(placeInfo.bedPosFoot) is BedBlock
            || world.getBlock(placeInfo.bedPosHead) is BedBlock
    }

    private fun SafeClientEvent.breakBed(placeInfo: PlaceInfo) {
        val side = getMiningSide(placeInfo.bedPosFoot) ?: Direction.UP
        val hitVecOffset = getHitVecOffset(side)
        val hitVec = Vec3(
            placeInfo.bedPosFoot.x + hitVecOffset.x.toDouble(),
            placeInfo.bedPosFoot.y + hitVecOffset.y.toDouble(),
            placeInfo.bedPosFoot.z + hitVecOffset.z.toDouble()
        )

        player.spoofUnSneak {
            world.withPrediction { sequence ->
                connection.send(
                    ServerboundUseItemOnPacket(
                        handMode,
                        BlockHitResult(hitVec, side, placeInfo.bedPosFoot, false),
                        sequence
                    )
                )
            }
        }
        connection.send(ServerboundSwingPacket(handMode))

        blockerExists.set(false)
        timer.reset()
        inactiveTicks = 0
    }

    private fun SafeClientEvent.placeBed(placeInfo: PlaceInfo) {
        player.spoofSneak {
            val hitVec = Vec3(placeInfo.basePos.x + 0.5, placeInfo.basePos.y + 1.0, placeInfo.basePos.z + 0.5)

            if (player.offhandItem.item is BedItem) {
                OffhandPause.withPause(BedAura) {
                    world.withPrediction { sequence ->
                        connection.send(
                            ServerboundUseItemOnPacket(
                                InteractionHand.OFF_HAND,
                                BlockHitResult(hitVec, Direction.UP, placeInfo.basePos, false),
                                sequence
                            )
                        )
                    }
                }
            } else {
                ghostSwitch(ghostSwitchBypass, bedSlot - 1) {
                    world.withPrediction { sequence ->
                        connection.send(
                            ServerboundUseItemOnPacket(
                                InteractionHand.MAIN_HAND,
                                BlockHitResult(hitVec, Direction.UP, placeInfo.basePos, false),
                                sequence
                            )
                        )
                    }
                }
            }
        }

        connection.send(ServerboundSwingPacket(handMode))

        CombatManager.target?.let { player.setLastHurtMob(it) }
        timer.reset()
        inactiveTicks = 0
    }

    private fun getDelay(placeInfo: PlaceInfo, delay: Int, slowDelay: Int): Int {
        return if (slowMode
            && !targetMoving
            && !selfMoving
            && CombatManager.target?.let { it.health > forcePlaceHealth } == true
            && placeInfo.targetDamage < slowModeDamage
        ) {
            slowDelay
        } else {
            delay
        }
    }

    private fun SafeClientEvent.update() {
        if (world.dimension() == Level.OVERWORLD || !player.allSlotsPrioritized.hasItem<BedItem>()) {
            reset()
        } else if (updateTimer.tickAndReset(updateDelay.toLong())) {
            ConcurrentScope.launch {
                placeInfo = calcPlaceInfo()
            }
        }


        selfMoving = motionDetect && CombatManager.trackerSelf?.let { it.motion.length() > selfMotion } ?: false
        targetMoving = motionDetect && CombatManager.trackerTarget?.let { it.motion.length() > targetMotion } ?: false

        shouldForcePlace = CombatManager.target?.let {
            it.health <= forcePlaceHealth
        } ?: false

        CombatManager.target?.let {
            if (System.currentTimeMillis() - CombatManager.getHurtTime(it) > 500L) {
                lastDamage = 0.0f
            }
        }
    }

    private fun SafeClientEvent.calcPlaceInfo(): PlaceInfo? {
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null
        val eyePos = player.eyePosition
        val map = Long2ObjectOpenHashMap<Vec2f>()
        val mutableBlockPos = BlockPos.MutableBlockPos()

        var ignoreNonFullBox = false
        if (antiBlocker) {
            if (blockerTimer.tickAndReset(antiBlockerSwitch.toLong())) {
                ignoreNonFullBox = blockerSwitch.getAndSet(!blockerSwitch.get())
            }
            ignoreNonFullBox = antiBlocker && !blockerExists.get()
        }

        return VectorUtils.getBlockPosInSphere(eyePos, range)
            .filter { !strictDirection || eyePos.y > it.y + 1.0 }
            .mapToCalcInfo(eyePos)
            .filterNot { contextTarget.entity.distanceSqToCenter(it.bedPosHead) > 100.0 }
            .filter { isValidBasePos(it.basePosFoot) && (newPlacement || isValidBasePos(it.basePosHead)) }
            .filter { isValidBedPos(ignoreNonFullBox, it) }
            .mapNotNull { checkDamage(map, contextSelf, contextTarget, it, mutableBlockPos) }
            .maxWithOrNull(
                compareBy<DamageInfo> { it.targetDamage }
                    .thenByDescending { eyePos.distanceSqToCenter(it.basePos) }
            )
            ?.toPlaceInfo()
    }

    private val HORIZONTALS = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

    private fun Sequence<BlockPos>.mapToCalcInfo(eyePos: Vec3): Sequence<CalcInfo> {
        return if (Bypass.blockPlaceRotation) {
            map {
                val bedPos = it.above()
                val hitVec = it.toVec3d(0.5, 1.0, 0.5)
                val side = calcDirection(eyePos, hitVec)

                CalcInfo(
                    side,
                    hitVec,
                    it,
                    it.relative(side),
                    bedPos,
                    bedPos.relative(side)
                )
            }
        } else {
            flatMap {
                val hitVec = it.toVec3d(0.5, 1.0, 0.5)

                sequenceOf(
                    newCalcInfo(Direction.NORTH, it, hitVec),
                    newCalcInfo(Direction.SOUTH, it, hitVec),
                    newCalcInfo(Direction.WEST, it, hitVec),
                    newCalcInfo(Direction.EAST, it, hitVec)
                )
            }
        }
    }

    private fun calcDirection(eyePos: Vec3, hitVec: Vec3): Direction {
        val x = hitVec.x - eyePos.x
        val z = hitVec.z - eyePos.z

        return HORIZONTALS.maxByOrNull {
            x * it.unitVec3i.x + z * it.unitVec3i.z
        } ?: Direction.NORTH
    }

    private fun newCalcInfo(side: Direction, pos: BlockPos, hitVec: Vec3): CalcInfo {
        val bedPos = pos.above()

        return CalcInfo(
            side,
            hitVec,
            pos,
            pos.relative(side),
            bedPos,
            bedPos.relative(side)
        )
    }

    private fun SafeClientEvent.isValidBasePos(basePos: BlockPos): Boolean {
        return world.getBlockState(basePos).isFaceSturdy(world, basePos, Direction.UP)
    }

    private fun SafeClientEvent.isValidBedPos(ignoreNonFullBox: Boolean, calcInfo: CalcInfo): Boolean {
        val headState = world.getBlockState(calcInfo.bedPosHead)
        val footState = world.getBlockState(calcInfo.bedPosFoot)

        val headBlock = headState.block
        val footBlock = footState.block

        return (checkBedBlock(ignoreNonFullBox, calcInfo.bedPosFoot, footState) || footBlock is BedBlock)
            && (checkBedBlock(ignoreNonFullBox, calcInfo.bedPosHead, headState)
            || headBlock is BedBlock
            && headState.getValue(BedBlock.PART) == BedPart.HEAD
            && headState.getValue(BedBlock.FACING) == calcInfo.side)
    }

    private fun SafeClientEvent.checkBedBlock(
        ignoreNonFullBox: Boolean,
        pos: BlockPos,
        state: BlockState,
    ): Boolean {
        val block = state.block
        return state.isAir
            || assumeInstantMine && PacketMine.isInstantMining(pos)
            || ignoreNonFullBox && !blockBlacklist.contains(block) && block !is BedBlock && !world.isFullBox(pos)
    }

    private fun checkDamage(
        map: Long2ObjectOpenHashMap<Vec2f>,
        contextSelf: CalcContext,
        contextTarget: CalcContext,
        calcInfo: CalcInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): DamageInfo? {
        val scaledHealth = contextSelf.entity.scaledHealth
        val (targetDamage, selfDamage) = map.computeIfAbsent(calcInfo.basePosHead.asLong()) {
            val center = calcInfo.bedPosHead.toVec3dCenter()
            Vec2f(
                contextTarget.calcDamage(center, true, 5.0f, mutableBlockPos, function),
                max(
                    contextSelf.calcDamage(center, false, 5.0f, mutableBlockPos, function),
                    contextSelf.calcDamage(center, true, 5.0f, mutableBlockPos, function)
                )
            )
        }

        val diff = targetDamage - selfDamage
        return if (scaledHealth - selfDamage > noSuicide
            && checkSelfDamage(selfDamage)
            && (checkDamage(targetDamage, diff)
                || checkForcePlaceDamage(targetDamage, diff))
        ) {
            DamageInfo(
                calcInfo.side,
                calcInfo.hitVec,
                calcInfo.basePosFoot,
                calcInfo.bedPosFoot,
                calcInfo.bedPosHead,
                targetDamage,
                selfDamage
            )
        } else {
            null
        }
    }

    private fun checkSelfDamage(selfDamage: Float): Boolean {
        return selfMoving && selfDamage <= motionMaxSelfDamage || selfDamage <= maxSelfDamage
    }

    private fun checkDamage(targetDamage: Float, diff: Float): Boolean {
        return (targetMoving && targetDamage >= motionMinDamage || targetDamage >= minDamage)
            && (targetMoving && diff >= motionDamageBalance || diff >= damageBalance)
    }

    private fun checkForcePlaceDamage(targetDamage: Float, diff: Float): Boolean {
        return (toggleForcePlace || shouldForcePlace) && targetDamage >= forcePlaceMinDamage && diff >= forcePlaceDamageBalance
    }

    private fun DamageInfo.toPlaceInfo(): PlaceInfo {
        val directionVec = side.unitVec3i
        val center = Vec3(
            bedPosFoot.x + 0.5 + directionVec.x * 0.5,
            bedPosFoot.y.toDouble(),
            bedPosFoot.z + 0.5 + directionVec.z * 0.5
        )

        return PlaceInfo(
            side,
            hitVec,
            basePos,
            bedPosFoot,
            bedPosHead,
            targetDamage,
            selfDamage,
            center,
            "${"%.1f".format(targetDamage)}/${"%.1f".format(selfDamage)}"
        )
    }

    private fun reset() {
        blockerExists.set(true)

        updateTimer.reset(-69420L)
        timer.reset(-69420L)

        switchPlacing = false
        placeInfo = null
        selfMoving = false
        targetMoving = false
        toggleForcePlace = false
        shouldForcePlace = false
        lastDamage = 0.0f
        lastTask = null

        inactiveTicks = 10
        needOffhandBed = false

        renderStartTime = 0L
    }

    private class CalcInfo(
        val side: Direction,
        val hitVec: Vec3,
        val basePosFoot: BlockPos,
        val basePosHead: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
    )

    private class DamageInfo(
        val side: Direction,
        val hitVec: Vec3,
        val basePos: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
        val targetDamage: Float,
        val selfDamage: Float
    )

    private class PlaceInfo(
        val direction: Direction,
        val hitVec: Vec3,
        val basePos: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
        val targetDamage: Float,
        val selfDamage: Float,
        val center: Vec3,
        val string: String
    )
}
