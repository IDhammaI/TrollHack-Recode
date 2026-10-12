package dev.luna5ama.trollhack.util.math

import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.Entity
import kotlin.math.roundToInt
import net.minecraft.core.Direction as McDirection

/**
 * [McDirection] but with 45° directions
 */
@Suppress("UNUSED")
enum class Direction(
    override val displayName: CharSequence,
    val displayNameXY: String,
    val directionVec: Vec3i,
    val isDiagonal: Boolean
) : DisplayEnum {
    NORTH("North", "-Z", Vec3i(0, 0, -1), false),
    NORTH_EAST("North East", "+X -Z", Vec3i(1, 0, -1), true),
    EAST("East", "+X", Vec3i(1, 0, 0), false),
    SOUTH_EAST("South East", "+X +Z", Vec3i(1, 0, 1), true),
    SOUTH("South", "+Z", Vec3i(0, 0, 1), false),
    SOUTH_WEST("South West", "-X +Z", Vec3i(-1, 0, 1), true),
    WEST("West", "-X", Vec3i(-1, 0, 0), false),
    NORTH_WEST("North West", "-X -Z", Vec3i(-1, 0, -1), true);

    fun clockwise(n: Int = 1) = entries[Math.floorMod((ordinal + n), 8)]

    fun counterClockwise(n: Int = 1) = entries[Math.floorMod((ordinal - n), 8)]

    companion object {

        fun fromEntity(entity: Entity?) = entity?.let {
            fromYaw(it.yRot)
        } ?: NORTH

        fun fromYaw(yaw: Float): Direction {
            val normalizedYaw = (RotationUtils.normalizeAngle(yaw) + 180.0f).coerceIn(0.0f, 360.0f)
            val index = (normalizedYaw / 45.0f).roundToInt() % 8
            return entries[index]
        }

        fun McDirection.toDirection() = when (this) {
            McDirection.NORTH -> NORTH
            McDirection.EAST -> EAST
            McDirection.SOUTH -> SOUTH
            McDirection.WEST -> WEST
            else -> null
        }
    }
}
