package dev.luna5ama.trollhack.util.math

import dev.fastmc.common.sq
import dev.fastmc.common.toDegree
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.*

/**
 * Utils for calculating angles and rotations
 */
object RotationUtils {
    fun calcAbsAngleDiff(a: Float, b: Float): Float {
        return abs(a - b) % 180.0f
    }

    fun calcAngleDiff(a: Float, b: Float): Float {
        val diff = a - b
        return normalizeAngle(diff)
    }

    fun SafeClientEvent.faceEntityClosest(entity: Entity) {
        val rotation = getRotationToEntityClosest(entity)
        player.setRotation(rotation)
    }

    fun SafeClientEvent.getRelativeRotation(entity: Entity): Float {
        return getRelativeRotation(entity.eyePosition)
    }

    fun SafeClientEvent.getRelativeRotation(posTo: Vec3): Float {
        return getRotationDiff(getRotationTo(posTo), Vec2f(player))
    }

    fun getRotationDiff(r1: Vec2f, r2: Vec2f): Float {
        val r1Radians = r1.toRadians()
        val r2Radians = r2.toRadians()
        return acos(
            cos(r1Radians.y) * cos(r2Radians.y) * cos(r1Radians.x - r2Radians.x) + sin(r1Radians.y) * sin(
                r2Radians.y
            )
        ).toDegree()
    }

    fun SafeClientEvent.getRotationToEntityClosest(entity: Entity): Vec2f {
        val box = entity.boundingBox

        val eyePos = player.eyePosition

        if (player.boundingBox.intersects(box)) {
            return getRotationTo(eyePos, box.center)
        }

        val x = eyePos.x.coerceIn(box.minX, box.maxX)
        val y = eyePos.y.coerceIn(box.minY, box.maxY)
        val z = eyePos.z.coerceIn(box.minZ, box.maxZ)

        val hitVec = Vec3(x, y, z)
        return getRotationTo(eyePos, hitVec)
    }

    fun SafeClientEvent.getRotationToEntity(entity: Entity): Vec2f {
        return getRotationTo(entity.position())
    }

    /**
     * Get rotation from a player position to another position vector
     *
     * @param posTo Calculate rotation to this position vector
     */
    fun SafeClientEvent.getRotationTo(posTo: Vec3): Vec2f {
        return getRotationTo(player.getEyePosition(1f), posTo)
    }

    fun SafeClientEvent.getYawTo(posTo: Vec3): Float {
        val vec = posTo.subtract(player.eyePosition)
        return normalizeAngle((atan2(vec.z, vec.x).toDegree() - 90.0).toFloat())
    }

    /**
     * Get rotation from a position vector to another position vector
     *
     * @param posFrom Calculate rotation from this position vector
     * @param posTo Calculate rotation to this position vector
     */
    fun getRotationTo(posFrom: Vec3, posTo: Vec3): Vec2f {
        val x = posTo.x - posFrom.x
        val y = posTo.y - posFrom.y
        val z = posTo.z - posFrom.z
        val xz = sqrt(x.sq + z.sq)
        val yaw = normalizeAngle(atan2(z, x).toDegree() - 90.0)
        val pitch = normalizeAngle(-atan2(y, xz).toDegree())
        return Vec2f(yaw, pitch)
    }

    fun normalizeAngle(angleIn: Double): Double {
        var angle = angleIn
        angle %= 360.0
        if (angle >= 180.0) {
            angle -= 360.0
        }
        if (angle < -180.0) {
            angle += 360.0
        }
        return angle
    }

    fun normalizeAngle(angleIn: Float): Float {
        var angle = angleIn
        angle %= 360.0f

        if (angle >= 180.0f) {
            angle -= 360.0f
        } else if (angle < -180.0f) {
            angle += 360.0f
        }

        return angle
    }

    fun LocalPlayer.setRotation(rotation: Vec2f) {
        this.setYaw(rotation.x)
        this.setPitch(rotation.y)
    }

    fun LocalPlayer.setYaw(yaw: Float) {
        this.yRot += normalizeAngle(yaw - this.yRot)
    }

    fun LocalPlayer.setPitch(pitch: Float) {
        this.xRot = (this.xRot + normalizeAngle(pitch - this.xRot)).coerceIn(-90.0f, 90.0f)
    }

    fun LocalPlayer.legitRotation(rotation: Vec2f): Vec2f {
        return Vec2f(legitYaw(rotation.x), legitPitch(rotation.y))
    }

    fun LocalPlayer.legitYaw(yaw: Float): Float {
        return this.yRot + normalizeAngle(yaw - this.yRot)
    }

    fun LocalPlayer.legitPitch(pitch: Float): Float {
        return (this.xRot + normalizeAngle(pitch - this.xRot)).coerceIn(-90.0f, 90.0f)
    }

    val Direction.yaw: Float
        get() = when (this) {
            Direction.NORTH -> -180.0f
            Direction.SOUTH -> 0.0f
            Direction.EAST -> -90.0f
            Direction.WEST -> 90.0f
            else -> 0.0f
        }
}
