package dev.luna5ama.trollhack.module.modules.movement

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.onGroundOverride
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

/**
 * Movement taken from Seppuku
 * https://github.com/seppukudevelopment/seppuku/blob/005e2da/src/main/java/me/rigamortis/seppuku/impl/module/player/NoHungerModule.java
 */
internal object AntiHunger : Module(
    name = "Anti Hunger",
    category = Category.MOVEMENT,
    description = "Reduces hunger lost when moving around"
) {
    private val cancelMovementState = setting("Cancel Movement State", true)

    init {
        listener<PacketEvent.Send> {
            val player = mc.player ?: return@listener
            if (cancelMovementState.value && it.packet is ServerboundPlayerCommandPacket) {
                if (it.packet.action == ServerboundPlayerCommandPacket.Action.START_SPRINTING
                    || it.packet.action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING
                ) {
                    it.cancel()
                }
            }
            if (it.packet is ServerboundMovePlayerPacket) {
                // Trick the server into thinking the player is airborne unless elytra flying
                it.packet.onGroundOverride =
                    (player.fallDistance <= 0 || mc.gameMode?.isDestroying == true) && player.isFallFlying
            }
        }
    }
}
