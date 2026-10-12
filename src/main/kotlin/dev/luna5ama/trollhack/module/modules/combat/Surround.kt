package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.StepEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.movement.AutoCenter
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.collections.EnumMap
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.HoleType
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import dev.luna5ama.trollhack.util.threads.runSynchronized
import dev.luna5ama.trollhack.util.world.PlaceInfo
import dev.luna5ama.trollhack.util.world.PlacementSearchOption
import dev.luna5ama.trollhack.util.world.checkPlaceRotation
import dev.luna5ama.trollhack.util.world.getCollisionBox
import dev.luna5ama.trollhack.util.world.getGroundPos
import dev.luna5ama.trollhack.util.world.getHitVec
import dev.luna5ama.trollhack.util.world.getHitVecOffset
import dev.luna5ama.trollhack.util.world.getPlacementSequence
import dev.luna5ama.trollhack.util.world.getVisibleSides
import dev.luna5ama.trollhack.util.world.toPlacePacket
import dev.luna5ama.trollhack.util.accessor.withPrediction
import it.unimi.dsi.fastutil.longs.Long2LongMaps
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSets
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Input
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

@CombatManager.CombatModule
internal object Surround : Module(
    name = "Surround",
    category = Category.COMBAT,
    description = "Surrounds you with obsidian to take less damage",
    modulePriority = 200
) {
    private val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.DEFAULT)
    private val placeDelay by setting("Place delay", 50, 0..1000, 1)
    private val multiPlace by setting("Multi Place", 2, 1..5, 1)
    private val placeTimeout by setting("Place Timeout", 100, 0..1000, 10)
    private val strictDirection by setting("Strict Direction", false)
    private val autoCenter by setting("Auto Center", true)
    private val autoDisable0 = setting("Auto Disable", AutoDisableMode.OUT_OF_HOLE)
    private val autoDisable by autoDisable0
    private val enableInHole0 = setting("Enable In Hole", false)
    private val enableInHole by enableInHole0
    private val inHoleTimeout by setting(
        "In Hole Timeout",
        50,
        1..100,
        5,
        enableInHole0.atTrue(),
        description = "Delay before enabling Surround when you are in hole, in ticks"
    )

    private enum class AutoDisableMode {
        NEVER, ONE_TIME, OUT_OF_HOLE
    }

    private val toggleTimer = TickTimer(TimeUnit.TICKS)
    private val placeTimer = TickTimer()

    private val placing = EnumMap<SurroundOffset, List<PlaceInfo>>().synchronized()
    private val placingSet = LongOpenHashSet()
    private val pendingPlacing = Long2LongMaps.synchronize(Long2LongOpenHashMap()).apply { defaultReturnValue(-1L) }
    private val placed = LongSets.synchronize(LongOpenHashSet())

    private var holePos: BlockPos? = null
    private var enableTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && placing.isNotEmpty()
    }

    init {
        onDisable {
            placeTimer.reset(-114514L)
            toggleTimer.reset()

            placing.clear()
            placingSet.clear()
            pendingPlacing.clear()
            placed.clear()

            holePos = null
            enableTicks = 0
        }

        safeListener<CrystalSetDeadEvent> { event ->
            if (event.crystals.none { it.distanceSqTo(player) < 6.0 }) return@safeListener
            var placeCount = 0

            placing.runSynchronized {
                val iterator = values.iterator()
                while (iterator.hasNext()) {
                    val list = iterator.next()
                    var allPlaced = true

                    loop@ for (placeInfo in list) {
                        if (event.crystals.none {
                                CrystalUtils.blockPlaceBoxIntersectsCrystalBox(
                                    placeInfo.placedPos,
                                    it
                                )
                            }) continue

                        val long = placeInfo.placedPos.asLong()
                        if (placed.contains(long)) continue
                        allPlaced = false

                        if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                        if (!checkRotation(placeInfo)) continue

                        placeBlock(placeInfo)
                        placeCount++
                        if (placeCount >= multiPlace) return@safeListener
                    }

                    if (allPlaced) iterator.remove()
                }
            }

            if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
                disable()
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            val pos = event.pos
            if (!event.newState.canBeReplaced()) {
                val long = pos.asLong()
                if (placingSet.contains(long)) {
                    pendingPlacing.remove(long)
                    placed.add(long)
                }
            } else {
                val relative = pos.subtract(player.betterPosition)
                if (SurroundOffset.entries.any { it.offset == relative } && checkColliding(pos)) {
                    getNeighbor(pos)?.let { placeInfo ->
                        if (checkRotation(placeInfo)) {
                            placingSet.add(placeInfo.placedPos.asLong())
                            placeBlock(placeInfo)
                        }
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (Bypass.blockPlaceRotation) {
                placing.runSynchronized {
                    for (list in values) {
                        for (placeInfo in list) {
                            val long = placeInfo.placedPos.asLong()
                            if (placed.contains(long)) {
                                continue
                            }

                            sendPlayerPacket {
                                var eyeHeight = player.eyeHeight
                                if (!player.isShiftKeyDown) eyeHeight -= 0.08f
                                rotate(
                                    getRotationTo(
                                        Vec3(player.x, player.y + eyeHeight, player.z),
                                        placeInfo.hitVec
                                    )
                                )
                            }
                            return@safeListener
                        }
                    }
                }
            }
        }

        safeListener<TickEvent.Pre> {
            enableTicks++
        }

        listener<StepEvent> {
            if (autoDisable == AutoDisableMode.NEVER) {
                placing.clear()
                placingSet.clear()
                pendingPlacing.clear()
                placed.clear()
                holePos = null
            } else {
                disable()
            }
        }

        safeListener<RunGameLoopEvent.Tick>(true) {
            if (!player.onGround()) {
                if (isEnabled) disable()
                return@safeListener
            }

            var playerPos = player.betterPosition
            val isInHole =
                player.onGround() && player.realSpeed < 0.1 && HoleManager.getHoleInfo(playerPos).type == HoleType.OBBY

            if (isDisabled) {
                enableInHoleCheck(isInHole)
                return@safeListener
            }

            if (world.getCollisionBox(playerPos.below()) == null) {
                playerPos = world.getGroundPos(player).above()
            }

            if (isInHole || holePos == null) {
                holePos = playerPos
            }

            updatePlacingMap(playerPos)

            if (placing.isNotEmpty() && placeTimer.tickAndReset(placeDelay.toLong())) {
                runPlacing()
            }
        }

        safeListener<PlayerMoveEvent.Pre> {
            if (autoCenter) {
                val holePos = holePos
                if (holePos != null) {
                    if (!player.isCentered(holePos) && (placing.isNotEmpty() || isSurroundPlaceable(holePos))) {
                        AutoCenter.centerPlayer(holePos)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.isSurroundPlaceable(holePos: BlockPos): Boolean {
        for (surroundOffset in SurroundOffset.entries) {
            val offsetPos = holePos.offset(surroundOffset.offset)
            if (world.getBlockState(offsetPos).canBeReplaced()) return true
        }

        return false
    }

    private fun enableInHoleCheck(isInHole: Boolean) {
        if (enableInHole && isInHole) {
            if (toggleTimer.tickAndReset(inHoleTimeout.toLong())) {
                enable()
            }
        } else {
            toggleTimer.reset()
        }
    }

    private fun SafeClientEvent.updatePlacingMap(playerPos: BlockPos) {
        pendingPlacing.runSynchronized {
            keys.removeIf {
                if (!world.getBlockState(BlockPos.of(it)).canBeReplaced()) {
                    placed.add(it)
                    true
                } else {
                    false
                }
            }
        }

        if (placing.isEmpty() && (pendingPlacing.isEmpty() || pendingPlacing.runSynchronized { values.all { System.currentTimeMillis() > it } })) {
            placing.clear()
            placed.clear()
        }

        for (surroundOffset in SurroundOffset.entries) {
            val offsetPos = playerPos.offset(surroundOffset.offset)
            if (!world.getBlockState(offsetPos).canBeReplaced()) continue

            getPlacementSequence(
                offsetPos,
                2,
                PlacementSearchOption.range(5.0),
                PlacementSearchOption.VISIBLE_SIDE.takeIf { strictDirection },
                PlacementSearchOption { _, _, to -> to != playerPos }
            )?.let { list ->
                placing[surroundOffset] = list
                list.forEach {
                    placingSet.add(it.placedPos.asLong())
                }
            }
        }
    }

    private fun SafeClientEvent.runPlacing() {
        var placeCount = 0

        placing.runSynchronized {
            val iterator = placing.values.iterator()
            while (iterator.hasNext()) {
                val list = iterator.next()
                var allPlaced = true
                var breakCrystal = false

                loop@ for (placeInfo in list) {
                    val long = placeInfo.placedPos.asLong()
                    if (placed.contains(long)) continue
                    allPlaced = false

                    if (System.currentTimeMillis() <= pendingPlacing[long]) continue
                    if (!checkRotation(placeInfo)) continue

                    for (entity in EntityManager.entity) {
                        if (breakCrystal && entity is EndCrystal) continue
                        if (!entity.blocksBuilding) continue
                        if (!entity.isAlive) continue
                        if (!entity.boundingBox.intersects(AABB(placeInfo.placedPos))) continue
                        if (entity !is EndCrystal) continue@loop

                        connection.send(ServerboundAttackPacket(entity.id))
                        connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
                        breakCrystal = true
                    }

                    placeBlock(placeInfo)
                    placeCount++
                    if (placeCount >= multiPlace) return
                }

                if (allPlaced) iterator.remove()
            }
        }

        if (autoDisable == AutoDisableMode.ONE_TIME && placing.isEmpty()) {
            disable()
        }
    }

    private fun SafeClientEvent.getNeighbor(pos: BlockPos): PlaceInfo? {
        for (side in Direction.entries) {
            val offsetPos = pos.relative(side)
            val oppositeSide = side.opposite

            if (strictDirection && !getVisibleSides(offsetPos, true).contains(oppositeSide)) continue
            if (world.getBlockState(offsetPos).canBeReplaced()) continue

            val hitVec = getHitVec(offsetPos, oppositeSide)
            val hitVecOffset = getHitVecOffset(oppositeSide)

            return PlaceInfo(offsetPos, oppositeSide, 0.0, hitVecOffset, hitVec, pos)
        }

        return null
    }

    private fun checkColliding(pos: BlockPos): Boolean {
        val box = AABB(pos)

        return EntityManager.entity.none {
            it.isAlive && it.blocksBuilding && it.boundingBox.intersects(box)
        }
    }

    private fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo) {
        val slot = getSlot() ?: run {
            disable()
            return
        }

        val sneak = !player.isShiftKeyDown
        if (sneak) sendSneakPacket(true)

        ghostSwitch(ghostSwitchBypass, slot) {
            world.withPrediction { sequence ->
                connection.send(placeInfo.toPlacePacket(InteractionHand.MAIN_HAND, sequence))
            }
        }
        connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))

        if (sneak) sendSneakPacket(false)

        onMainThreadSafe {
            val blockState = Blocks.OBSIDIAN.getStateForPlacement(
                BlockPlaceContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    Blocks.OBSIDIAN.asItem().defaultInstance,
                    BlockHitResult(placeInfo.hitVec, placeInfo.direction, placeInfo.pos, false)
                )
            ) ?: Blocks.OBSIDIAN.defaultBlockState()
            val soundType = blockState.soundType
            world.playSound(
                player,
                placeInfo.pos,
                soundType.placeSound,
                SoundSource.BLOCKS,
                (soundType.volume + 1.0f) / 2.0f,
                soundType.pitch * 0.8f
            )
        }

        pendingPlacing[placeInfo.placedPos.asLong()] = System.currentTimeMillis() + placeTimeout
    }

    private fun SafeClientEvent.sendSneakPacket(sneak: Boolean) {
        val keyPresses = player.input.keyPresses
        connection.send(
            ServerboundPlayerInputPacket(
                Input(
                    keyPresses.forward(),
                    keyPresses.backward(),
                    keyPresses.left(),
                    keyPresses.right(),
                    keyPresses.jump(),
                    sneak,
                    keyPresses.sprint()
                )
            )
        )
    }

    private fun SafeClientEvent.getSlot(): Slot? {
        val slot = player.allSlotsPrioritized.firstBlock(Blocks.OBSIDIAN)

        return if (slot == null) {
            NoSpamMessage.sendMessage("$chatName No obsidian in inventory!")
            null
        } else {
            slot
        }
    }

    private fun SafeClientEvent.checkRotation(placeInfo: PlaceInfo): Boolean {
        return !Bypass.blockPlaceRotation || checkPlaceRotation(placeInfo)
    }

    private enum class SurroundOffset(val offset: BlockPos) {
        DOWN(BlockPos(0, -1, 0)),
        NORTH(BlockPos(0, 0, -1)),
        EAST(BlockPos(1, 0, 0)),
        SOUTH(BlockPos(0, 0, 1)),
        WEST(BlockPos(-1, 0, 0))
    }
}
