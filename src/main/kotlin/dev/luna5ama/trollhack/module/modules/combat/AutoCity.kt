package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.accessor.withPrediction
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystalOn
import dev.luna5ama.trollhack.util.combat.CrystalUtils.isValidMaterial
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.world.FastRayTraceAction
import dev.luna5ama.trollhack.util.world.fastRayTrace
import dev.luna5ama.trollhack.util.world.getBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.AnvilBlock
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

@CombatManager.CombatModule
internal object AutoCity : Module(
    name = "Auto City",
    category = Category.COMBAT,
    description = "Trolling",
    modulePriority = 100
) {
    private val placeAnvil by setting("Place Anvil", false)
    private val placeCrystal by setting("Place Crystal", true)
    private val breakCrystal by setting("Break Crystal", true)
    private val placeDelay by setting("Place Delay", 100, 0..1000, 5)
    private val breakDelay by setting("Break Delay", 100, 0..1000, 5)
    private val packetBreakDelay by setting("Packet Break Delay", 100, 0..1000, 5)
    private val minInitDamage by setting("Min Init Damage", 4.0f, 1.0f..20.0f, 0.1f)
    private val minUpdateDamage by setting("Min Update Damage", 8.0f, 1.0f..20.0f, 0.1f)
    private val minDamageIncrease by setting("Min Damage Increase", 5.0f, 0.0f..20.0f, 0.1f)
    private val minEffectiveDamage by setting("Min Effective Damage", 2.0f, -20.0f..20.0f, 0.1f)
    private val updateDelay by setting("Update Delay", 200, 0..1000, 5)
    private val wallRange by setting("Wall Range", 3.0f, 1.0f..6.0f, 0.1f)
    private val range by setting("Range", 4.5f, 1.0f..6.0f, 0.1f)

    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val updateTimer = TickTimer()
    private val packetBreakTimer = TickTimer()

    private var targetInfo: TargetInfo? = null

    override fun isActive(): Boolean {
        return isEnabled && targetInfo != null
    }

    init {
        onDisable {
            placeTimer.reset(-114514L)
            breakTimer.reset(-114514L)

            targetInfo = null
        }

        safeParallelListener<TickEvent.Post> {
            updateTargetInfo()

            val targetInfo = targetInfo

            if (targetInfo == null) {
                PacketMine.reset(AutoCity)
                return@safeParallelListener
            }

            val block = world.getBlock(targetInfo.surroundPos)
            targetInfo.anvilPlaced = block != Blocks.AIR

            targetInfo.crystalID = CombatManager.crystalList.asSequence()
                .map { it.first }
                .filter { it.isAlive }
                .find { CrystalUtils.crystalIntersects(it, targetInfo.crystalPos) }?.id ?: -1

            if (block != Blocks.AIR) {
                PacketMine.mineBlock(AutoCity, targetInfo.surroundPos, AutoCity.modulePriority)
            }
        }

        safeListener<CrystalSpawnEvent> {
            val targetInfo = targetInfo ?: return@safeListener
            if (!CrystalUtils.crystalIntersects(targetInfo.crystalPos, it.crystalDamage.blockPos)) return@safeListener

            targetInfo.crystalID = it.entityID
            if (!targetInfo.anvilPlaced && breakTimer.tick(0L)) {
                breakCrystal(it.entityID)
                placeAnvil(targetInfo.surroundDown)
                placeCrystal(targetInfo.crystalPos)
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> {
            val targetInfo = targetInfo ?: return@safeListener

            if (it.pos != targetInfo.surroundPos) return@safeListener

            if (it.newState.block != Blocks.AIR) {
                PacketMine.mineBlock(AutoCity, targetInfo.surroundPos, AutoCity.modulePriority)
                return@safeListener
            }

            targetInfo.anvilPlaced = false
            val crystalID = targetInfo.crystalID

            if (crystalID != -1
                && packetBreakTimer.tick(packetBreakDelay.toLong())
                && breakTimer.tick(0L)
            ) {
                breakCrystal(crystalID)
                placeAnvil(targetInfo.surroundDown)
                placeCrystal(targetInfo.crystalPos)
                packetBreakTimer.reset()
            }
        }

        safeListener<CrystalSetDeadEvent> { event ->
            val targetInfo = targetInfo ?: return@safeListener
            if (event.crystals.none { CrystalUtils.crystalIntersects(it, targetInfo.crystalPos) }) return@safeListener
            targetInfo.crystalID = -1

            placeAnvil(targetInfo.surroundDown)
            placeCrystal(targetInfo.crystalPos)
        }

        safeListener<RunGameLoopEvent.Tick> {
            val targetInfo = targetInfo ?: return@safeListener
            val crystalID = targetInfo.crystalID

            if (!targetInfo.anvilPlaced && crystalID != -1 && breakTimer.tick(breakDelay.toLong())) {
                breakCrystal(crystalID)
                placeAnvil(targetInfo.surroundDown)
                placeCrystal(targetInfo.crystalPos)

                placeTimer.reset()
            }

            if (placeTimer.tick(placeDelay.toLong())) {
                if (placeAnvil) {
                    if (!targetInfo.anvilPlaced) {
                        placeAnvil(targetInfo.surroundDown)
                    } else {
                        if (world.getBlock(targetInfo.surroundPos) !is AnvilBlock
                            && world.getBlockState(targetInfo.surroundPos.above()).canBeReplaced()
                        ) {
                            placeAnvil(targetInfo.surroundPos)
                        }
                    }
                }

                if (crystalID == -1) {
                    placeCrystal(targetInfo.crystalPos)
                }
            }
        }
    }

    private fun SafeClientEvent.updateTargetInfo() {
        targetInfo = calcTargetInfo()
    }

    private fun SafeClientEvent.calcTargetInfo(): TargetInfo? {
        val target = CombatManager.target ?: return null
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null

        val targetPos = target.betterPosition
        val prev = targetInfo?.takeIf { it.holePos == targetPos }

        if (prev == null && !HoleManager.getHoleInfo(target).isHole) return null

        val mutableBlockPos = BlockPos.MutableBlockPos()

        if (prev != null) {
            if (!updateTimer.tick(updateDelay.toLong())) return prev
            val prevDamage = prev.calcDamage(contextSelf, mutableBlockPos)
            if (prevDamage > minUpdateDamage) return prev
        }

        var maxDamage = 0.0f
        var maxDamageDiff = minEffectiveDamage
        var resultPair: Pair<BlockPos, BlockPos>? = null

        fun checkDamages(sequence: Sequence<Pair<BlockPos, BlockPos>>) {
            for (pair in sequence) {
                val damage = calcDamage(contextTarget, pair.first, pair.second, mutableBlockPos)
                if (damage < minInitDamage) continue

                val selfDamage = calcDamage(contextSelf, pair.first, pair.second, mutableBlockPos)
                val damageDiff = damage - selfDamage

                if (damageDiff > maxDamageDiff) {
                    maxDamageDiff = damageDiff
                    maxDamage = damage
                    resultPair = pair
                }
            }
        }

        checkDamages(calcSequence(targetPos))

        if (resultPair == null) {
            checkDamages(calcSequence(targetPos.below()))
        }

        val result = resultPair ?: return null

        val surroundPos = result.first
        val crystalPos = result.second

        if (prev != null && (surroundPos != prev.surroundPos || crystalPos != prev.crystalPos)) {
            val surroundBlockState = world.getBlockState(prev.surroundPos)
            if ((surroundBlockState.block is AnvilBlock || !CrystalUtils.isResistant(surroundBlockState))
                || maxDamage - prev.calcDamage(contextTarget, mutableBlockPos) < minDamageIncrease
            ) {
                return prev
            }
        }

        return TargetInfo(
            targetPos,
            surroundPos,
            crystalPos,
        )
    }

    private val HORIZONTALS = arrayOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)

    private fun SafeClientEvent.calcSequence(
        targetPos: BlockPos,
    ): Sequence<Pair<BlockPos, BlockPos>> {
        val playerPos = player.betterPosition
        val eyePos = player.eyePosition
        val rangeSq = range.sq
        val wallRangeSq = wallRange.sq
        val mutableBlockPos = BlockPos.MutableBlockPos()

        return HORIZONTALS.asSequence()
            .flatMap { mainSide ->
                val opposite = mainSide.opposite
                val pos1 = targetPos.relative(mainSide)
                HORIZONTALS.asSequence()
                    .filter {
                        it != opposite
                    }.map {
                        pos1 to pos1.relative(it).below()
                    }
            }.filter { (minePos, _) ->
                playerPos.distSqr(minePos) <= rangeSq
            }
            .filter { (minePos, crystalPos) ->
                val dist = playerPos.distSqr(crystalPos)
                dist <= rangeSq
                    && (dist <= wallRangeSq
                    || !world.fastRayTrace(
                    eyePos,
                    crystalPos.x + 0.5,
                    crystalPos.y + 2.7,
                    crystalPos.z + 0.5,
                    20,
                    mutableBlockPos
                ) { rayTracePos, blockState ->
                    if (rayTracePos != minePos && !blockState.getCollisionShape(this, rayTracePos).isEmpty) {
                        FastRayTraceAction.CALC
                    } else {
                        FastRayTraceAction.SKIP
                    }
                })
            }
            .filter { (minePos, crystalPos) ->
                world.getBlock(minePos) != Blocks.BEDROCK
                    && canPlaceCrystal(crystalPos, mutableBlockPos = mutableBlockPos)
            }
    }

    private fun calcDamage(
        context: CalcContext,
        minePos: BlockPos,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): Float {
        return context.calcDamage(
            pos.x + 0.5,
            pos.y + 1.0,
            pos.z + 0.5,
            false,
            6.0f,
            mutableBlockPos
        ) { rayTracePos, blockState ->
            if (rayTracePos != minePos && !blockState.isAir && CrystalUtils.isResistant(blockState)) {
                FastRayTraceAction.CALC
            } else {
                FastRayTraceAction.SKIP
            }
        }
    }

    private fun SafeClientEvent.placeCrystal(targetPos: BlockPos) {
        if (!placeCrystal) return

        player.hotbarSlots.firstItem(Items.END_CRYSTAL)?.let {
            ghostSwitch(it) {
                sendUseItemOn(targetPos)
            }
            connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
            placeTimer.reset()
        }
    }

    private fun SafeClientEvent.placeAnvil(targetPos: BlockPos) {
        if (!placeAnvil) return

        player.hotbarSlots.firstBlock(Blocks.ANVIL)?.let {
            player.spoofSneak {
                ghostSwitch(it) {
                    sendUseItemOn(targetPos)
                }
            }
            connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
            placeTimer.reset()
        }
    }

    private fun SafeClientEvent.sendUseItemOn(targetPos: BlockPos) {
        val hitResult = BlockHitResult(
            Vec3(targetPos.x + 0.5, targetPos.y + 1.0, targetPos.z + 0.5),
            Direction.UP,
            targetPos,
            false
        )
        world.withPrediction { sequence ->
            connection.send(ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, sequence))
        }
    }

    private fun SafeClientEvent.breakCrystal(entityID: Int) {
        if (!breakCrystal) return

        connection.send(ServerboundAttackPacket(entityID))
        connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
        breakTimer.reset()
    }

    private class TargetInfo(val holePos: BlockPos, val surroundPos: BlockPos, val crystalPos: BlockPos) {
        val surroundDown: BlockPos = surroundPos.below()
        var crystalID = -1
        var anvilPlaced = false

        fun calcDamage(context: CalcContext, mutableBlockPos: BlockPos.MutableBlockPos): Float {
            return calcDamage(context, holePos, crystalPos, mutableBlockPos)
        }
    }
}
