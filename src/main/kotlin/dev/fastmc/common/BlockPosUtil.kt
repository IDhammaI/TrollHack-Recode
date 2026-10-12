package dev.fastmc.common

import net.minecraft.core.BlockPos

object BlockPosUtil {
    fun toLong(x: Int, y: Int, z: Int): Long {
        return BlockPos.asLong(x, y, z)
    }

    fun xFromLong(long: Long): Int {
        return BlockPos.getX(long)
    }

    fun yFromLong(long: Long): Int {
        return BlockPos.getY(long)
    }

    fun zFromLong(long: Long): Int {
        return BlockPos.getZ(long)
    }
}
