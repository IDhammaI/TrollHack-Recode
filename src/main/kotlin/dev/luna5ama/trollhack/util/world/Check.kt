package dev.luna5ama.trollhack.util.world

import dev.fastmc.common.ceilToInt
import dev.fastmc.common.floorToInt
import dev.fastmc.common.sq
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.math.vector.toVec3dCenter
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import kotlin.math.max

fun Level.canBreakBlock(pos: BlockPos): Boolean {
    val blockState = this.getBlockState(pos)
    return blockState.isAir || blockState.getDestroySpeed(this, pos) != -1.0f
}

fun Level.getGroundPos(entity: Entity): BlockPos {
    return getGroundPos(entity.boundingBox)
}

fun Level.getGroundPos(boundingBox: AABB): BlockPos {
    val center = boundingBox.center

    val cx = center.x.floorToInt()
    val cz = center.z.floorToInt()

    var rx = cx
    var ry = Int.MIN_VALUE
    var rz = cz

    val pos = BlockPos.MutableBlockPos()

    for (x in (boundingBox.minX + 0.01).floorToInt()..(boundingBox.maxX - 0.01).floorToInt()) {
        for (z in (boundingBox.minZ + 0.01).floorToInt()..(boundingBox.maxZ - 0.01).floorToInt()) {
            for (y in (boundingBox.minY - 0.5).floorToInt() downTo minY - 1) {
                if (y < ry) break

                pos.set(x, y, z)
                val shape = this.getBlockState(pos).getCollisionShape(this, pos)
                if (!shape.isEmpty && (ry == Int.MIN_VALUE || y > ry || (x - cx).sq <= (rx - cx).sq && (z - cz).sq <= (rz - cz).sq)) {
                    rx = x
                    ry = y
                    rz = z
                }
            }
        }
    }

    return BlockPos(rx, if (ry == Int.MIN_VALUE) -999 else ry, rz)
}

fun Level.getGroundLevel(entity: Entity): Double {
    return getGroundLevel(entity.boundingBox)
}

fun Level.getGroundLevel(boundingBox: AABB): Double {
    var maxY = Double.MIN_VALUE
    val pos = BlockPos.MutableBlockPos()

    for (x in (boundingBox.minX + 0.01).floorToInt()..(boundingBox.maxX - 0.01).floorToInt()) {
        for (z in (boundingBox.minZ + 0.01).floorToInt()..(boundingBox.maxZ - 0.01).floorToInt()) {
            for (y in (boundingBox.minY - 0.5).floorToInt() downTo minY - 1) {
                if (y < maxY.ceilToInt() - 1) break

                pos.set(x, y, z)
                val box = this.getCollisionBox(pos)
                if (box != null) {
                    maxY = max(maxY, y + box.maxY)
                }
            }
        }
    }

    return maxY
}

fun Level.isVisible(
    pos: BlockPos,
    tolerance: Double = 1.0
) = Wrapper.player?.let {
    val center = pos.toVec3dCenter()
    val result = clip(
        ClipContext(
            it.getEyePosition(1.0f),
            center,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            it
        )
    )

    result.type != HitResult.Type.MISS
        && (result.blockPos == pos
        || result.location.distanceTo(center) <= tolerance)
} ?: false

fun Level.isLiquid(pos: BlockPos): Boolean {
    return this.getBlockState(pos).isLiquid
}

fun Level.isWater(pos: BlockPos): Boolean {
    return this.getBlockState(pos).isWater
}

fun SafeClientEvent.hasNeighbor(pos: BlockPos): Boolean {
    return Direction.entries.any {
        !world.getBlockState(pos.relative(it)).isReplaceable
    }
}

/**
 * Checks if given [pos] is able to place block in it
 *
 * @return true player is not colliding with [pos] and there is block below it
 */
fun Level.isPlaceable(pos: BlockPos, ignoreSelfCollide: Boolean = false) =
    this.getBlockState(pos).isReplaceable
        && EntityManager.checkNoEntityCollision(AABB(pos), if (ignoreSelfCollide) Wrapper.player else null)

fun Level.checkBlockCollision(pos: BlockPos, box: AABB, tolerance: Double = 0.005): Boolean {
    val blockBox = getCollisionBox(pos) ?: return false
    return box.intersects(
        pos.x + blockBox.minX + tolerance, pos.y + blockBox.minY + tolerance, pos.z + blockBox.minZ + tolerance,
        pos.x + blockBox.maxX - tolerance, pos.y + blockBox.maxY - tolerance, pos.z + blockBox.maxZ - tolerance
    )
}
