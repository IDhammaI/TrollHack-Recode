package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.BaritoneUtils
import dev.luna5ama.trollhack.util.threads.runSafeOrFalse
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player

/**
 * Hooked from MixinPlayerEdge.isStayingOnGroundSurface
 */
internal object SafeWalk : Module(
    name = "Safe Walk",
    category = Category.MOVEMENT,
    description = "Keeps you from walking off edges"
) {
    private val checkFallDist by setting("Check Fall Distance", true, description = "Check fall distance from edge")

    @JvmStatic
    fun shouldSafewalk(entity: Player): Boolean {
        return runSafeOrFalse {
            if (entity != player) return false
            if (player.isShiftKeyDown) return false

            // TODO: hook Scaffold.shouldSafeWalk once Scaffold is ported
            if (isEnabled && (!checkFallDist && !BaritoneUtils.isPathing || !isEdgeSafe())) return true

            false
        }
    }

    private fun isEdgeSafe(): Boolean {
        return runSafeOrFalse {
            checkFallDist(player.x, player.z)
                && checkFallDist(player.x + player.deltaMovement.x, player.z + player.deltaMovement.z)
        }
    }

    private fun SafeClientEvent.checkFallDist(posX: Double, posZ: Double): Boolean {
        val startY = (player.y - 0.5).floorToInt()
        val pos = BlockPos.MutableBlockPos(posX.floorToInt(), startY, posZ.floorToInt())

        for (y in startY downTo startY - 2) {
            pos.setY(y)
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty) {
                return true
            }
        }

        pos.setY(startY - 3)
        val shape = world.getBlockState(pos).getCollisionShape(world, pos)

        return !shape.isEmpty && shape.bounds().maxY >= 1.0
    }
}
