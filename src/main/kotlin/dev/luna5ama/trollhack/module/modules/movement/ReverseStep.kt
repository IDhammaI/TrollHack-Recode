package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.PlayerTravelEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isInOrAboveLiquid
import dev.luna5ama.trollhack.util.world.getGroundLevel
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3

internal object ReverseStep : Module(
    name = "Reverse Step",
    description = "Walks down edge of block faster",
    category = Category.MOVEMENT
) {
    private val height by setting("Height", 2.0f, 0.25f..8.0f, 0.1f)
    private val speed by setting("Speed", 1.0f, 0.1f..8.0f, 0.1f)

    private val timer = TickTimer()

    init {
        listener<PacketEvent.Receive> {
            if (it.packet is ClientboundPlayerPositionPacket) {
                timer.reset()
            }
        }

        safeListener<PlayerTravelEvent>(100) {
            if (shouldRun()) {
                player.deltaMovement = player.deltaMovement.add(0.0, -speed.toDouble(), 0.0)
            }
        }
    }

    private fun SafeClientEvent.shouldRun(): Boolean {
        return !mc.options.keyShift.isDown
            && !mc.options.keyJump.isDown
            && !player.isFallFlying
            && !player.abilities.flying
            && !player.onClimbable()
            && !player.isInOrAboveLiquid
            && player.onGround()
            && player.deltaMovement.y in -0.08..0.0
            && timer.tick(3000L)
            && checkGroundLevel()
    }

    private fun SafeClientEvent.checkGroundLevel(): Boolean {
        val motion = player.deltaMovement
        return player.y - world.getGroundLevel(player) in 0.25..height.toDouble()
            || player.y - world.getGroundLevel(
            player.boundingBox.move(
                motion.x,
                0.0,
                motion.z
            )
        ) in 0.25..height.toDouble()
            || player.y - world.getGroundLevel(
            player.boundingBox.move(
                motion.x * 2.0,
                0.0,
                motion.z * 2.0
            )
        ) in 0.25..height.toDouble()
    }
}
