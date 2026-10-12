package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils
import dev.luna5ama.trollhack.util.MovementUtils.calcMoveYaw
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

internal object Flight : Module(
    name = "Flight",
    category = Category.MOVEMENT,
    description = "Makes the player fly",
    modulePriority = 500
) {
    private val speed by setting("Speed", 1.0f, 0.0f..10.0f, 0.1f)
    private val upSpeed by setting("Up Speed", 1.0f, 0.1f..10.0f, 0.05f)
    private val downSpeed by setting("Down Speed", 1.0f, 0.1f..10.0f, 0.05f)
    private val glideSpeed by setting("Glide Speed", 0.05, 0.0..0.3, 0.001)
    private val antiKick by setting("Anti Kick", true)

    private val antiKickTimer = TickTimer(TimeUnit.TICKS)

    init {
        safeListener<PlayerMoveEvent.Pre>(-500) {
            if (MovementUtils.isInputting()) {
                val yaw = player.calcMoveYaw()
                it.x = -sin(yaw) * speed
                it.z = cos(yaw) * speed
            } else {
                it.x = 0.0
                it.z = 0.0
            }

            val jump = player.input.keyPresses.jump()
            val sneak = player.input.keyPresses.shift()

            if (jump xor sneak) {
                if (jump) {
                    it.y = upSpeed.toDouble()
                } else {
                    it.y = -downSpeed.toDouble()
                }
            } else {
                it.y = -glideSpeed
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (antiKick && antiKickTimer.tickAndReset(50L)) {
                if (world.noCollision(player.boundingBox.inflate(0.0625).expandTowards(0.0, -0.55, 0.0))) {
                    sendPlayerPacket {
                        move(Vec3(player.x, PlayerPacketManager.position.y - 0.04, player.z))
                    }
                }
            }
        }
    }
}
