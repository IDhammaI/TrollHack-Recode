package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSetDeadEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.accessor.withPrediction
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.hasValidSpaceForCrystal
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.world.PlacementSearchOption
import dev.luna5ama.trollhack.util.world.canBreakBlock
import dev.luna5ama.trollhack.util.world.getHitVecOffset
import dev.luna5ama.trollhack.util.world.getMiningSide
import dev.luna5ama.trollhack.util.world.getPlacement
import dev.luna5ama.trollhack.util.world.placeBlock
import dev.luna5ama.trollhack.util.world.rayTraceVisible
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

/**
 * Ported 1:1 from TrollHack 1.4.0 (Forge 1.12.2) to Fabric 26.2.
 *
 * Degradations from the original:
 * - `AutoOffhand` is not ported yet, so the `AutoOffhand.lastType != AutoOffhand.Type.TOTEM`
 *   guard in [safeCheck] was dropped. The module no longer pauses while an offhand totem
 *   swap would be in progress; restore the guard once AutoOffhand is ported.
 * - `Burrow` is not ported yet, so the `Burrow.isBurrowed(target)` early-exit in [calcTarget]
 *   was dropped. The module may now also target burrowed players; restore the check once
 *   Burrow is ported.
 * - `CPacketPlayerTryUseItemOnBlock` was replaced by a prediction-wrapped
 *   [ServerboundUseItemOnPacket] (26.2 block interactions must go through the prediction
 *   system); crystal attacks use [ServerboundAttackPacket] like the rest of the port.
 */
@CombatManager.CombatModule
internal object CevBreaker : Module(
    name = "Cev Breaker",
    description = "Troll module",
    category = Category.COMBAT,
    modulePriority = 400
) {
    private val minHealth by setting("Min Health", 8.0f, 0.0f..20.0f, 0.5f)
    private val placeDelay by setting("Place Delay", 500, 0..1000, 1)
    private val breakDelay by setting("Break Delay", 100, 0..1000, 1)
    private val range by setting("Range", 5.0f, 0.0f..6.0f, 0.25f)

    private val renderer = ESPRenderer().apply { aFilled = 31; aOutline = 233 }
    private val placeTimer = TickTimer()
    private val breakTimer = TickTimer()
    private val packetTimer = TickTimer()
    private var posInfo: Info? = null
    private var crystalID = -69420

    init {
        onEnable {
            PacketMine.enable()
        }

        onDisable {
            reset()
        }

        listener<Render3DEvent> { event ->
            if (posInfo != null) {
                renderer.render(event.renderer, false)
            }
        }

        safeListener<WorldEvent.ServerBlockUpdate> { event ->
            val info = posInfo ?: return@safeListener

            if (event.pos == info.pos) {
                val current = event.oldState.block
                val new = event.newState.block

                if (new != current && new == Blocks.AIR) {
                    val id = crystalID
                    if (id != -69420 && safeCheck()) {
                        breakCrystal(id)
                    }
                }
            }
        }

        safeListener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet !is ClientboundSoundPacket) return@safeListener
            val info = posInfo ?: return@safeListener

            if (packet.source == SoundSource.BLOCKS
                && packet.sound.value() == SoundEvents.GENERIC_EXPLODE.value()
                && info.pos.distanceSqToCenter(packet.x, packet.y - 0.5, packet.z) < 0.25
            ) {
                crystalID = -69420
            }
        }

        safeListener<CrystalSpawnEvent> {
            val info = posInfo ?: return@safeListener
            if (it.crystalDamage.blockPos != info.pos
                && !CrystalUtils.crystalPlaceBoxIntersectsCrystalBox(info.pos, it.crystalDamage.crystalPos)
            ) return@safeListener

            crystalID = it.entityID
        }

        safeListener<CrystalSetDeadEvent> { event ->
            val info = posInfo ?: return@safeListener
            if (event.crystals.none { it.boundingBox.intersects(info.placeBB) }) return@safeListener

            crystalID = -69420
            place(info)
        }

        safeParallelListener<TickEvent.Post> {
            if (!safeCheck()) {
                posInfo = null
                return@safeParallelListener
            }

            updateTarget()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (!safeCheck()) {
                return@safeListener
            }

            val info = posInfo ?: return@safeListener
            val blockState = world.getBlockState(info.pos)

            if (blockState.block == Blocks.AIR) {
                var id = crystalID
                if (id == -69420) {
                    CombatManager.crystalList.asSequence()
                        .filter { it.first.isAlive }
                        .find { it.first.boundingBox.intersects(info.placeBB) }
                        ?.let { id = it.first.id }
                }

                PacketMine.mineBlock(CevBreaker, info.pos, CevBreaker.modulePriority)

                if (id != -69420) {
                    if (breakTimer.tick(breakDelay)) breakCrystal(id)
                }

                if (placeTimer.tick(placeDelay)) place(info)
            }
        }
    }

    private fun SafeClientEvent.safeCheck(): Boolean {
        return player.health >= minHealth
    }

    private fun SafeClientEvent.updateTarget() {
        val targetInfo = calcTarget()

        if (targetInfo == null) {
            reset()
        } else if (targetInfo != posInfo) {
            reset()

            renderer.clear()
            renderer.add(AABB(targetInfo.pos), ColorRGB(255, 255, 255))
            packetTimer.reset(-69420L)

            PacketMine.mineBlock(CevBreaker, targetInfo.pos, CevBreaker.modulePriority)
        }

        posInfo = targetInfo
    }

    private fun SafeClientEvent.calcTarget(): Info? {
        val target = CombatManager.target ?: return null

        val pos = BlockPos.containing(target.x, target.y + 2.5, target.z)

        if (player.distanceSqToCenter(pos) > range * range) return null
        if (!world.canBreakBlock(pos)) return null
        if (!hasValidSpaceForCrystal(pos)) return null
        if (!wallCheck(pos)) return null

        return Info(pos, getMiningSide(pos) ?: Direction.UP)
    }

    private fun SafeClientEvent.wallCheck(pos: BlockPos): Boolean {
        val eyePos = player.eyePosition
        return eyePos.distanceSqTo(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) <= 9.0
            || world.rayTraceVisible(eyePos, pos.toVec3d(0.5, 2.7, 0.5))
    }

    private fun SafeClientEvent.place(info: Info) {
        val obsidianSlot = player.allSlotsPrioritized.firstBlock(Blocks.OBSIDIAN) ?: return
        val crystalSlot = player.allSlotsPrioritized.firstItem(Items.END_CRYSTAL) ?: return
        val placeInfo = getPlacement(
            info.pos,
            arrayOf(Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST, Direction.DOWN),
            PlacementSearchOption.ENTITY_COLLISION,
            PlacementSearchOption.range(6.0f)
        ) ?: return

        synchronized(InventoryTaskManager) {
            ghostSwitch(obsidianSlot) {
                placeBlock(placeInfo)
            }

            ghostSwitch(crystalSlot) {
                val hitResult = BlockHitResult(
                    Vec3(
                        info.pos.x + info.hitVecOffset.x.toDouble(),
                        info.pos.y + info.hitVecOffset.y.toDouble(),
                        info.pos.z + info.hitVecOffset.z.toDouble()
                    ),
                    info.side,
                    info.pos,
                    false
                )

                world.withPrediction { sequence ->
                    connection.send(ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, sequence))
                }

                connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
            }
        }

        placeTimer.reset()
    }

    private fun SafeClientEvent.breakCrystal(id: Int) {
        connection.send(ServerboundAttackPacket(id))
        connection.send(ServerboundSwingPacket(InteractionHand.MAIN_HAND))
        breakTimer.reset()
    }

    private fun reset() {
        placeTimer.reset(-69420L)
        breakTimer.reset(-69420L)
        packetTimer.reset(-69420L)
        posInfo = null
        crystalID = -69420
        PacketMine.reset(this)
    }

    private class Info(
        val pos: BlockPos,
        val side: Direction,
    ) {
        val placeBB = AABB(
            pos.x - 1.0, pos.y + 0.0, pos.z - 1.0,
            pos.x + 2.0, pos.y + 3.0, pos.z + 2.0
        )
        val hitVecOffset = getHitVecOffset(side)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Info) return false

            return pos == other.pos
        }

        override fun hashCode(): Int {
            return pos.hashCode()
        }
    }
}
