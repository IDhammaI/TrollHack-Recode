package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.CrystalSpawnEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.withPrediction
import dev.luna5ama.trollhack.util.combat.CrystalUtils.canPlaceCrystal
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.pause.MainHandPause
import dev.luna5ama.trollhack.util.pause.OffhandPause
import dev.luna5ama.trollhack.util.pause.withPause
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

/**
 * Ported 1:1 from TrollHack 1.4.0 (Forge 1.12.2) to Fabric 26.2.
 *
 * Degradations / API translations (no behavioural change intended):
 * - 1.12 built the crystal attack via `CPacketUseEntity` + reflection accessors (`id`/`packetAction`);
 *   26.2 has a dedicated `ServerboundAttackPacket(entityId)` which is used instead
 *   (same idiom as the other ported crystal modules).
 * - 1.12 sent `CPacketPlayerTryUseItemOnBlock(pos, UP, MAIN_HAND, 0.5f, 1.0f, 0.5f)` for placing;
 *   26.2 sends `ServerboundUseItemOnPacket` with the equivalent absolute hit vec
 *   (pos.x + 0.5, pos.y + 1.0, pos.z + 0.5) through the block-state prediction system
 *   (`world.withPrediction`) as required on 26.2.
 * - `player.swingArm(MAIN_HAND)` -> `player.swing(InteractionHand.MAIN_HAND)`.
 */
internal object SelfCrystal : Module(
    name = "Self Crystal",
    description = "Suicide using crystal",
    category = Category.COMBAT,
    modulePriority = Int.MAX_VALUE
) {
    private val breakTimer = TickTimer()
    private var crystalPos: Vec3? = null

    init {
        safeListener<CrystalSpawnEvent> {
            breakCrystal(it.entityID)
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            crystalPos?.let {
                sendPlayerPacket {
                    rotate(getRotationTo(it))
                }
            }
        }

        safeListener<EntityEvent.Death> {
            if (it.entity == player) {
                Notification.send(this@SelfCrystal, "$chatName Disabled on death.")
                disable()
            }
        }

        safeParallelListener<TickEvent.Post> {
            OffhandPause.withPause(this@SelfCrystal, 1000) {
                MainHandPause.withPause(this@SelfCrystal, 1000) {
                    val crystalSlot = player.allSlots.firstItem(Items.END_CRYSTAL)
                    if (crystalSlot == null) {
                        Notification.send(this@SelfCrystal, "$chatName No crystal found in inventory, disabling.")
                        disable()
                        return@safeParallelListener
                    }

                    if (breakTimer.tick(500L)) {
                        CombatManager.crystalList
                            .maxByOrNull { it.second.selfDamage }
                            ?.let {
                                breakCrystal(it.first.id)
                                breakTimer.reset()
                            }
                    }

                    val mutableBlockPos = BlockPos.MutableBlockPos()

                    CombatManager.placeList.asSequence()
                        .filter { player.distanceSqTo(it.crystalPos) < 9.0 }
                        .filter { canPlaceCrystal(it.blockPos, player, mutableBlockPos) }
                        .maxByOrNull { it.selfDamage }
                        ?.let {
                            crystalPos = it.crystalPos

                            MainHandPause.withPause(this@SelfCrystal) {
                                ghostSwitch(crystalSlot) {
                                    sendPlacePacket(it.blockPos)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun SafeClientEvent.breakCrystal(entityID: Int) {
        connection.send(ServerboundAttackPacket(entityID))
        player.swing(InteractionHand.MAIN_HAND)
    }

    private fun SafeClientEvent.sendPlacePacket(pos: BlockPos) {
        val hitResult = BlockHitResult(
            Vec3(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5),
            Direction.UP,
            pos,
            false
        )

        world.withPrediction { sequence ->
            connection.send(ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, sequence))
        }
    }
}
