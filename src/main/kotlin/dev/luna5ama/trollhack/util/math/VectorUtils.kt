package dev.luna5ama.trollhack.util.math

import dev.fastmc.common.*
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import net.minecraft.core.Direction as McDirection

object VectorUtils {
    fun getBlockPosInSphere(entity: Entity, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(entity.x, entity.y, entity.z, radius)
    }

    fun getBlockPosInSphere(pos: Vec3, radius: Float): Sequence<BlockPos> {
        return getBlockPosInSphere(pos.x, pos.y, pos.z, radius)
    }

    fun getBlockPosInSphere(cx: Double, cy: Double, cz: Double, radius: Float): Sequence<BlockPos> {
        val squaredRadius = radius.sq.toDouble()
        val blockPos = BlockPos.MutableBlockPos()

        return sequence {
            for (x in getAxisRange(cx, radius)) {
                for (y in getAxisRange(cy, radius)) {
                    for (z in getAxisRange(cz, radius)) {
                        blockPos.set(x, y, z)
                        if (blockPos.distanceSqToCenter(cx, cy, cz) > squaredRadius) continue
                        yield(blockPos.immutable())
                    }
                }
            }
        }
    }

    private fun getAxisRange(d1: Double, d2: Float): IntRange {
        return IntRange((d1 - d2).floorToInt(), (d1 + d2).ceilToInt())
    }

    fun Vec2f.toViewVec(): Vec3 {
        val yawRad = this.x.toDouble().toRadians()
        val pitchRag = this.y.toDouble().toRadians()
        val yaw = -yawRad - PI_FLOAT
        val pitch = -pitchRag

        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosPitch = -cos(pitch)
        val sinPitch = sin(pitch)

        return Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch)
    }

    fun Vec3i.multiply(multiplier: Int): Vec3i {
        return Vec3i(this.x * multiplier, this.y * multiplier, this.z * multiplier)
    }

    infix operator fun Vec3.times(vec3d: Vec3): Vec3 = Vec3(x * vec3d.x, y * vec3d.y, z * vec3d.z)

    infix operator fun Vec3.times(multiplier: Double): Vec3 =
        Vec3(x * multiplier, y * multiplier, z * multiplier)

    infix operator fun Vec3.plus(vec3d: Vec3): Vec3 = add(vec3d)

    infix operator fun Vec3.minus(vec3d: Vec3): Vec3 = subtract(vec3d)

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, add: Vec3i): BlockPos.MutableBlockPos {
        return this.set(set.x + add.x, set.y + add.y, set.z + add.z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: Vec3i, x: Int, y: Int, z: Int): BlockPos.MutableBlockPos {
        return this.set(set.x + x, set.y + y, set.z + z)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: McDirection): BlockPos.MutableBlockPos {
        return this.setAndAdd(set, side.unitVec3i)
    }

    fun BlockPos.MutableBlockPos.setAndAdd(set: BlockPos, side: McDirection, n: Int): BlockPos.MutableBlockPos {
        val dirVec = side.unitVec3i
        return this.set(set.x + dirVec.x * n, set.y + dirVec.y * n, set.z + dirVec.z * n)
    }

    fun toLong(blockPos: BlockPos): Long {
        return BlockPosUtil.toLong(blockPos.x, blockPos.y, blockPos.z)
    }

    fun toLong(x: Int, y: Int, z: Int): Long {
        return BlockPosUtil.toLong(x, y, z)
    }

    fun toLong(x: Double, y: Double, z: Double): Long {
        return BlockPosUtil.toLong(x.floorToInt(), y.floorToInt(), z.floorToInt())
    }

    fun fromLong(long: Long): BlockPos {
        return BlockPos(BlockPosUtil.xFromLong(long), BlockPosUtil.yFromLong(long), BlockPosUtil.zFromLong(long))
    }

    fun xFromLong(long: Long): Int {
        return BlockPosUtil.xFromLong(long)
    }

    fun yFromLong(long: Long): Int {
        return BlockPosUtil.yFromLong(long)
    }

    fun zFromLong(long: Long): Int {
        return BlockPosUtil.zFromLong(long)
    }
}
