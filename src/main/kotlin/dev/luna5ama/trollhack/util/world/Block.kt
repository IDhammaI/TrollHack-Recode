package dev.luna5ama.trollhack.util.world

import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

val BlockState.isBlacklisted: Boolean
    get() = blockBlacklist.contains(this.block)

val BlockState.isLiquid: Boolean
    get() = !this.fluidState.isEmpty

val BlockState.isWater: Boolean
    get() = this.fluidState.`is`(FluidTags.WATER)

val BlockState.isReplaceable: Boolean
    get() = this.canBeReplaced()

fun Level.getBlockState(x: Int, y: Int, z: Int): BlockState {
    return if (y < minY || y > maxY) {
        Blocks.AIR.defaultBlockState()
    } else {
        getBlockState(BlockPos(x, y, z))
    }
}

fun Level.isAir(x: Int, y: Int, z: Int): Boolean {
    return getBlockState(x, y, z).isAir
}

fun Level.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).isAir
}

fun Level.getBlock(pos: BlockPos): Block =
    this.getBlockState(pos).block

fun Level.isFullBox(pos: BlockPos): Boolean =
    this.getBlockState(pos).isCollisionShapeFullBlock(this, pos)

fun Level.getSelectedBox(pos: BlockPos): AABB? {
    val shape = this.getBlockState(pos).getShape(this, pos)
    return if (shape.isEmpty) null else shape.bounds().move(pos)
}

fun Level.getCollisionBox(pos: BlockPos): AABB? {
    val shape = this.getBlockState(pos).getCollisionShape(this, pos)
    return if (shape.isEmpty) null else shape.bounds()
}

fun Level.hasCollisionBox(pos: BlockPos): Boolean = this.getCollisionBox(pos) != null
