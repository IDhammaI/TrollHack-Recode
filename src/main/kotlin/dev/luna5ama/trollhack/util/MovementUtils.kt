package dev.luna5ama.trollhack.util

import dev.fastmc.common.toDegree
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import net.minecraft.client.Minecraft
import net.minecraft.client.player.ClientInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

object MovementUtils {
    private val mc get() = Minecraft.getInstance()

    fun isInputting(
        movementInput: ClientInput? = Wrapper.player?.input,
        jump: Boolean = false,
        sneak: Boolean = false
    ): Boolean {
        if (movementInput == null) return false
        return movementInput.moveVector.y != 0.0f
            || movementInput.moveVector.x != 0.0f
            || jump && movementInput.keyPresses.jump()
            || sneak && movementInput.keyPresses.shift()
    }

    val Entity.isMoving get() = speed > 0.0001
    val Entity.speed get() = hypot(deltaMovement.x, deltaMovement.z)
    val Entity.realSpeed get() = hypot(x - xOld, z - zOld)

    val Entity.realMotionX get() = x - xOld
    val Entity.realMotionY get() = y - yOld
    val Entity.realMotionZ get() = z - zOld

    val Entity.motionX: Double get() = deltaMovement.x
    val Entity.motionY: Double get() = deltaMovement.y
    val Entity.motionZ: Double get() = deltaMovement.z

    /* totally not taken from elytrafly */
    fun LocalPlayer.calcMoveYaw(): Double {
        return calcMoveYaw(
            yaw = yRot,
            moveForward = input.moveVector.y,
            moveStrafe = input.moveVector.x
        )
    }

    fun calcMoveYaw(
        yaw: Float,
        moveForward: Float,
        moveStrafe: Float
    ): Double {
        val moveYaw = if (moveForward == 0.0f && moveStrafe == 0.0f) 0.0
        else atan2(moveForward, moveStrafe).toDegree() - 90.0
        return RotationUtils.normalizeAngle(yaw + moveYaw).toRadians()
    }

    fun calcMovementInput(
        forward: Boolean,
        backward: Boolean,
        left: Boolean,
        right: Boolean,
        up: Boolean,
        down: Boolean,
    ): Vec3f {
        var moveForward = 0.0f
        var moveStrafing = 0.0f
        var moveVertical = 0.0f

        if (forward) moveForward++
        if (backward) moveForward--

        if (left) moveStrafing++
        if (right) moveStrafing--

        if (up) moveVertical++
        if (down) moveVertical--

        return Vec3f(moveStrafing, moveVertical, moveForward)
    }

    fun LivingEntity.applySpeedPotionEffects(speed: Double): Double {
        var result = speed

        this.getEffect(MobEffects.SPEED)?.let {
            result += speed * (it.amplifier + 1.0) * 0.2
        }

        this.getEffect(MobEffects.SLOWNESS)?.let {
            result -= speed * (it.amplifier + 1.0) * 0.15
        }

        return result
    }

    val LivingEntity.speedEffectMultiplier: Double
        get() {
            var result = 1.0

            this.getEffect(MobEffects.SPEED)?.let {
                result += (it.amplifier + 1.0) * 0.2
            }

            this.getEffect(MobEffects.SLOWNESS)?.let {
                result -= (it.amplifier + 1.0) * 0.15
            }

            return result
        }

    fun LivingEntity.applyJumpBoostPotionEffects(motion: Double): Double {
        return this.getEffect(MobEffects.JUMP_BOOST)?.let {
            motion + (it.amplifier + 1.0) * 0.2
        } ?: motion
    }

    fun LocalPlayer.isCentered(center: BlockPos): Boolean {
        return this.isCentered(center.x + 0.5, center.z + 0.5)
    }

    fun LocalPlayer.isCentered(center: Vec3): Boolean {
        return this.isCentered(center.x, center.z)
    }

    fun LocalPlayer.isCentered(x: Double, z: Double): Boolean {
        return abs(this.x - x) < 0.2
            && abs(this.z - z) < 0.2
    }

    fun setEntityMotion(entity: Entity, x: Double, y: Double, z: Double) {
        entity.deltaMovement = Vec3(x, y, z)
    }

    fun Entity.setMotionX(value: Double) {
        deltaMovement = Vec3(value, deltaMovement.y, deltaMovement.z)
    }

    fun Entity.setMotionY(value: Double) {
        deltaMovement = Vec3(deltaMovement.x, value, deltaMovement.z)
    }

    fun Entity.setMotionZ(value: Double) {
        deltaMovement = Vec3(deltaMovement.x, deltaMovement.y, value)
    }
}
