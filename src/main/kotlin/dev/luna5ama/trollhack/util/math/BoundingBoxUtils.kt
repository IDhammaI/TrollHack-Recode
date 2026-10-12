package dev.luna5ama.trollhack.util.math

import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.util.math.VectorUtils.plus
import dev.luna5ama.trollhack.util.math.VectorUtils.times
import dev.luna5ama.trollhack.util.math.VectorUtils.toViewVec
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import net.minecraft.core.Direction as McDirection

val AABB.xCenter get() = minX + xLength * 0.5

val AABB.yCenter get() = minY + yLength * 0.5

val AABB.zCenter get() = minZ + zLength * 0.5

val AABB.xLength get() = maxX - minX

val AABB.yLength get() = maxY - minY

val AABB.zLength get() = maxZ - minZ

val AABB.lengths get() = Vec3(xLength, yLength, zLength)

fun AABB.corners(scale: Double): Array<Vec3> {
    val growSizes = lengths * (scale - 1.0)
    return inflate(growSizes.x, growSizes.y, growSizes.z).corners()
}

fun AABB.corners() = arrayOf(
    Vec3(minX, minY, minZ),
    Vec3(minX, minY, maxZ),
    Vec3(minX, maxY, minZ),
    Vec3(minX, maxY, maxZ),
    Vec3(maxX, minY, minZ),
    Vec3(maxX, minY, maxZ),
    Vec3(maxX, maxY, minZ),
    Vec3(maxX, maxY, maxZ),
)

fun AABB.side(side: McDirection, scale: Double = 0.5): Vec3 {
    val lengths = lengths
    val sideDirectionVec = side.unitVec3i.toVec3d()
    return lengths * sideDirectionVec * scale + center
}

fun AABB.scale(multiplier: Double): AABB {
    return this.scale(multiplier, multiplier, multiplier)
}

fun AABB.scale(x: Double, y: Double, z: Double): AABB {
    val halfXLength = this.xLength * 0.5
    val halfYLength = this.yLength * 0.5
    val halfZLength = this.zLength * 0.5

    return this.inflate(halfXLength * (x - 1.0), halfYLength * (y - 1.0), halfZLength * (z - 1.0))
}

fun AABB.limitSize(x: Double, y: Double, z: Double): AABB {
    val halfX = min(xLength, x) / 2.0
    val halfY = min(yLength, y) / 2.0
    val halfZ = min(zLength, z) / 2.0
    val center = center

    return AABB(
        center.x - halfX, center.y - halfY, center.z - halfZ,
        center.x + halfX, center.y + halfY, center.z + halfZ,
    )
}

fun AABB.intersectsBlock(x: Int, y: Int, z: Int): Boolean {
    return intersects(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 1.0, z + 1.0)
}

fun AABB.intersectsBlock(pos: BlockPos): Boolean {
    return intersectsBlock(pos.x, pos.y, pos.z)
}

/**
 * Check if a box is in sight
 */
fun AABB.isInSight(
    posFrom: Vec3 = PlayerPacketManager.position,
    rotation: Vec2f = PlayerPacketManager.rotation,
    range: Double = 8.0
): Boolean {
    return isInSight(posFrom, rotation.toViewVec(), range)
}

/**
 * Check if a box is in sight
 */
fun AABB.isInSight(
    posFrom: Vec3,
    viewVec: Vec3,
    range: Double = 4.25
): Boolean {
    val sightEnd = posFrom.add(viewVec.scale(range))
    return inflate(dev.luna5ama.trollhack.module.modules.exploit.Bypass.placeRotationBoundingBoxGrow)
        .intersects(posFrom, sightEnd)
}
