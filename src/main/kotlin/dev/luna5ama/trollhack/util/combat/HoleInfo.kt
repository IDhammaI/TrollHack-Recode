package dev.luna5ama.trollhack.util.combat

import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class HoleInfo(
    val origin: BlockPos,
    val center: Vec3,
    val boundingBox: AABB,
    val holePos: ObjectSet<BlockPos>,
    val surroundPos: ObjectSet<BlockPos>,
    val type: HoleType,
    val isTrapped: Boolean,
    val isFullyTrapped: Boolean
) {
    val isHole = type != HoleType.NONE
    val isSafe = type == HoleType.BEDROCK
    val isTwo = type == HoleType.TWO
    val isFour = type == HoleType.FOUR

    fun canEnter(world: Level, pos: BlockPos): Boolean {
        val headPosY = pos.y + 2
        if (origin.y >= headPosY) return false
        val box = boundingBox.expandTowards(0.0, headPosY - origin.y - 1.0, 0.0)
        return world.noCollision(box)
    }

    override fun equals(other: Any?): Boolean {
        return (this === other
            || other is HoleInfo
            && origin == other.origin)
    }

    override fun hashCode(): Int {
        return origin.hashCode()
    }

    companion object {
        fun empty(pos: BlockPos): HoleInfo {
            return HoleInfo(
                pos,
                Vec3.ZERO,
                emptyAABB,
                ObjectSets.emptySet(),
                ObjectSets.emptySet(),
                HoleType.NONE,
                isTrapped = false,
                isFullyTrapped = false,
            )
        }

        private val emptyAABB = AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
    }
}
