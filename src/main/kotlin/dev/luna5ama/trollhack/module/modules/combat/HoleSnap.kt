package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.distanceSq
import dev.fastmc.common.sq
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.InputUpdateEvent
import dev.luna5ama.trollhack.event.events.player.PlayerMoveEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.TimerManager.modifyTimer
import dev.luna5ama.trollhack.manager.managers.TimerManager.resetTimer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFlying
import dev.luna5ama.trollhack.util.MovementUtils.applySpeedPotionEffects
import dev.luna5ama.trollhack.util.MovementUtils.isCentered
import dev.luna5ama.trollhack.util.accessor.setMoveVector
import dev.luna5ama.trollhack.util.combat.HoleInfo
import dev.luna5ama.trollhack.util.math.RotationUtils
import net.minecraft.client.player.KeyboardInput
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.player.Input
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 26.2 port notes / degradations:
 * - HolePathFinder is not ported (its PathFinder / world-dump pathfinding infrastructure does not
 *   exist in this port), so every integration with it is dropped:
 *   - the hole is always sourced from [findHole] (originally `HolePathFinder.hole ?: findHole()`),
 *   - the timeout counter always runs (originally it was suspended while HolePathFinder was active),
 *   - `stuckTicks` increments on every horizontal collision (originally only while HolePathFinder
 *     was disabled).
 * - The "Disable Speed" and "Disable Step" settings are dropped: the Speed and Step modules they
 *   toggled are not ported.
 * - 1.12's mutable `MovementInput.resetMove()` is gone. Movement input is cleared by writing a new
 *   immutable [Input] record (movement keys false, jump/sneak/sprint preserved like the original
 *   `resetMove`) plus a zeroed move vector through the `AccessorClientInput` extension, and the
 *   `MovementInputFromOptions` type check becomes [KeyboardInput].
 * - The tracer line to the hole is drawn through the frame's [Render3DEvent] renderer
 *   (`throughWalls = true` replaces the original's disabled depth test); there is no legacy GL.
 * - The original's `MovementUtils.speed` extension (`hypot(motionX, motionZ)`) is inlined here:
 *   on 26.2 `LivingEntity` has a `getSpeed()` member (the movement speed attribute) which would
 *   shadow the extension and silently change the meaning of `player.speed`.
 */
internal object HoleSnap : Module(
    name = "Hole Snap",
    description = "Move you into the hole nearby",
    category = Category.COMBAT,
    modulePriority = 120
) {
    private val downRange by setting("Down Range", 5, 1..8, 1)
    private val upRange by setting("Up Range", 1, 1..8, 1)
    private val hRange by setting("H Range", 4.0f, 1.0f..8.0f, 0.25f)
    private val timer by setting("Timer", 1.5f, 1.0f..4.0f, 0.01f)
    private val postTimer by setting("Post Timer", 0.8f, 0.01f..1.0f, 0.01f, { timer > 1.0f })
    private val maxPostTicks by setting("Max Post Ticks", 20, 0..50, 1, { timer > 1.0f && postTimer < 1.0f })
    private val timeoutTicks by setting("Timeout Ticks", 10, 0..100, 5)

    var hole: HoleInfo? = null; private set
    private var stuckTicks = 0
    private var ranTicks = 0
    private var enabledTicks = 0

    override fun isActive(): Boolean {
        return isEnabled && hole != null
    }

    init {
        onDisable {
            hole = null
            stuckTicks = 0
            ranTicks = 0
            enabledTicks = 0
            resetTimer()
        }

        safeListener<Render3DEvent>(1) { event ->
            hole?.let {
                val posFrom = EntityUtils.getInterpolatedPos(player, event.partialTicks)
                val color = ColorRGB(32, 255, 32, 255)

                event.renderer.line(posFrom, it.center, color, 2.0f, throughWalls = true)
            }
        }

        listener<PacketEvent.Receive> {
            if (it.packet is ClientboundPlayerPositionPacket) disable()
        }

        listener<InputUpdateEvent>(-69) {
            val input = it.movementInput
            if (input is KeyboardInput && isActive()) {
                val prev = input.keyPresses
                input.keyPresses = Input(
                    false,
                    false,
                    false,
                    false,
                    prev.jump(),
                    prev.shift(),
                    prev.sprint()
                )
                input.setMoveVector(0.0f, 0.0f)
            }
        }

        safeListener<PlayerMoveEvent.Pre>(-10) { event ->
            if (++enabledTicks > timeoutTicks) {
                disable()
                return@safeListener
            }

            if (!player.isAlive || player.isFlying) return@safeListener

            val currentSpeed = hypot(player.deltaMovement.x, player.deltaMovement.z)

            if (shouldDisable(currentSpeed)) {
                val ticks = ranTicks
                disable()

                if (timer > 0.0f && postTimer < 1.0f && ticks > 0) {
                    val x = (postTimer * ticks - timer * postTimer * ticks) / (timer * (postTimer - 1.0f))
                    val postTicks = min(maxPostTicks, x.ceilToInt())
                    modifyTimer(50.0f / postTimer, postTicks)
                }
                return@safeListener
            }

            hole = findHole()?.let {
                enabledTicks = 0

                if (checkYRange(player.y.toInt(), it.origin.y)
                    && distanceSq(player.x, player.z, it.center.x, it.center.z) <= hRange.sq
                ) {
                    modifyTimer(50.0f / timer, 5)
                    ranTicks++

                    val playerPos = player.position()
                    val yawRad = RotationUtils.getRotationTo(playerPos, it.center).x.toRadians()
                    val dist = hypot(it.center.x - playerPos.x, it.center.z - playerPos.z)
                    val baseSpeed = player.applySpeedPotionEffects(0.2873)
                    val speed = if (player.onGround()) baseSpeed else max(currentSpeed + 0.02, baseSpeed)
                    val cappedSpeed = min(speed, dist)

                    player.deltaMovement = Vec3(0.0, player.deltaMovement.y, 0.0)
                    event.x = -sin(yawRad) * cappedSpeed
                    event.z = cos(yawRad) * cappedSpeed

                    if (player.horizontalCollision) stuckTicks++
                    else stuckTicks = 0

                    it
                } else {
                    null
                }
            }
        }
    }

    private fun SafeClientEvent.shouldDisable(currentSpeed: Double) =
        hole?.let { player.y < it.origin.y } ?: false
            || stuckTicks > 5 && currentSpeed < 0.05
            || player.onGround() && HoleManager.getHoleInfo(player).let {
            it.isHole && player.isCentered(it.center)
        }

    private fun SafeClientEvent.findHole(): HoleInfo? {
        val playerPos = player.betterPosition
        val hRangeSq = hRange * hRange

        return HoleManager.holeInfos.asSequence()
            .filterNot { it.isTrapped }
            .filter { checkYRange(playerPos.y, it.origin.y) }
            .filter { distanceSq(player.x, player.z, it.center.x, it.center.z) <= hRangeSq }
            .filter { it.canEnter(world, playerPos) }
            .minByOrNull { distanceSq(player.x, player.z, it.center.x, it.center.z) }
    }

    private fun checkYRange(playerY: Int, holeY: Int): Boolean {
        return if (playerY >= holeY) {
            playerY - holeY <= downRange
        } else {
            holeY - playerY <= -upRange
        }
    }
}
