package dev.luna5ama.trollhack.util.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3

class PlaceInfo(
    val pos: BlockPos,
    val direction: Direction,
    val dist: Double,
    val hitVecOffset: Vec3f,
    val hitVec: Vec3,
    val placedPos: BlockPos
) {
    companion object {
        fun SafeClientEvent.newPlaceInfo(pos: BlockPos, side: Direction): PlaceInfo {
            val hitVecOffset = getHitVecOffset(side)
            val hitVec = getHitVec(pos, side)

            return PlaceInfo(pos, side, player.eyePosition.distanceTo(hitVec), hitVecOffset, hitVec, pos.relative(side))
        }
    }
}
