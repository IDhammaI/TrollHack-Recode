package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.threads.runSafeOrFalse
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.world.entity.LivingEntity

internal object Sprint : Module(
    name = "Sprint",
    description = "Automatically makes the player sprint",
    category = Category.MOVEMENT
) {
    private val multiDirection by setting("Multi Direction", true, description = "Sprint in any direction")
    private val checkFlying by setting("Check Flying", true, description = "Cancels while flying")
    private val checkCollide by setting("Check Collide", true, description = "Cancels on colliding with blocks")
    private val checkCriticals by setting("Check Criticals", false, description = "Cancels on attack for criticals")

    init {
        safeListener<PacketEvent.Send>(-69420) {
            if (!it.cancelled && checkCriticals(it)) {
                connection.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING))
            }
        }

        safeListener<PacketEvent.PostSend> {
            if (checkCriticals(it)) {
                connection.send(ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING))
            }
        }
    }

    private fun SafeClientEvent.checkCriticals(event: PacketEvent) =
        event.packet is ServerboundAttackPacket
            && checkCriticals
            && player.isSprinting
            && world.getEntity(event.packet.entityId()) is LivingEntity

    init {
        safeListener<TickEvent.Pre> {
            player.isSprinting = shouldSprint()
        }
    }

    @JvmStatic
    fun shouldSprint(): Boolean {
        return runSafeOrFalse {
            isEnabled
                && !mc.options.keyShift.isDown
                && !player.isFallFlying
                && player.foodData.foodLevel > 6
                && !BaritoneUtils.isPathing
                && checkMovementInput()
                && (!checkFlying || !player.abilities.flying)
                && (!checkCollide || !player.horizontalCollision)
        }
    }

    private fun SafeClientEvent.checkMovementInput(): Boolean {
        return if (multiDirection) {
            MovementUtils.isInputting()
        } else {
            player.input.moveVector.y > 0.0f
        }
    }
}
