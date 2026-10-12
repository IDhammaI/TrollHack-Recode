package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.onGroundOverride
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

internal object NoFall : Module(
    name = "No Fall",
    category = Category.PLAYER,
    description = "Prevents fall damage"
) {
    private val distance = setting("Distance", 3, 1..10, 1)
    private val mode = setting("Mode", Mode.CATCH)
    private val voidOnly = setting("Void Only", false, mode.atValue(Mode.CATCH))

    private enum class Mode {
        FALL, CATCH
    }

    init {
        safeListener<PacketEvent.Send> {
            if (it.packet !is ServerboundMovePlayerPacket || !noFallCheck()) return@safeListener
            it.packet.onGroundOverride = true
        }

        safeParallelListener<TickEvent.Post> {
            if (mode.value == Mode.CATCH && noFallCheck() && fallDistCheck()) {
                player.fallDistance = 0.0
                connection.send(
                    ServerboundMovePlayerPacket.Pos(
                        player.x,
                        player.y + 10.0,
                        player.z,
                        false,
                        player.horizontalCollision
                    )
                )
            }
        }
    }

    private fun SafeClientEvent.noFallCheck(): Boolean {
        return !player.isCreative
            && !player.isSpectator
            && !player.isFallFlying
    }

    private fun SafeClientEvent.fallDistCheck(): Boolean {
        return !voidOnly.value && player.fallDistance >= distance.value || player.y < world.minY && world.getGroundLevel(
            player
        ) == Double.MIN_VALUE
    }
}
