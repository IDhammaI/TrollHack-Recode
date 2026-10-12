package dev.luna5ama.trollhack.util.combat

import dev.luna5ama.trollhack.util.EntityUtils
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

class MotionTracker(val entity: Entity) {
    private val motionLog = ArrayDeque<Vec3>()

    var prevMotion = Vec3(0.0, 0.0, 0.0); private set
    var motion = Vec3(0.0, 0.0, 0.0); private set

    fun tick() {
        synchronized(this) {
            motionLog.add(calcActualMotion(entity))
            while (motionLog.size > 5) motionLog.removeFirstOrNull()
            prevMotion = motion
            motion = calcAverageMotion()
        }
    }

    private fun calcActualMotion(entity: Entity): Vec3 {
        return Vec3(
            entity.x - entity.xOld,
            entity.y - entity.yOld,
            entity.z - entity.zOld
        )
    }

    private fun calcAverageMotion(): Vec3 {
        var sumX = 0.0
        var sumY = 0.0
        var sumZ = 0.0

        for (motion in motionLog) {
            sumX += motion.x
            sumY += motion.y
            sumZ += motion.z
        }

        return Vec3(sumX, sumY, sumZ).scale(1.0 / motionLog.size)
    }

    /**
     * Calculate the predicted position of the target entity based on [calcAverageMotion]
     */
    fun calcPosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3 {
        val relativePos = calcRelativePosAhead(ticksAhead, interpolation)
        val partialTicks = if (interpolation) currentPartialTicks() else 1.0f
        return EntityUtils.getInterpolatedPos(entity, partialTicks).add(relativePos)
    }

    /**
     * Calculate the predicted moved vector of the target entity based on [calcAverageMotion]
     */
    fun calcRelativePosAhead(ticksAhead: Int, interpolation: Boolean = false): Vec3 {
        val world = Wrapper.world ?: return Vec3.ZERO
        val partialTicks = if (interpolation) currentPartialTicks() else 1.0f

        val averageMotion = prevMotion.add(motion.subtract(prevMotion).scale(partialTicks.toDouble()))
        var movedVec = Vec3(0.0, 0.0, 0.0)

        for (ticks in 0..ticksAhead) {
            movedVec = if (canMove(
                    world,
                    entity.boundingBox,
                    movedVec.add(averageMotion)
                )
            ) { // Attempt to move with full motion
                movedVec.add(averageMotion)
            } else if (canMove(
                    world,
                    entity.boundingBox,
                    movedVec.add(averageMotion.x, 0.0, averageMotion.z)
                )
            ) { // Attempt to move horizontally
                movedVec.add(averageMotion.x, 0.0, averageMotion.z)
            } else if (canMove(world, entity.boundingBox, movedVec.add(0.0, averageMotion.y, 0.0))) {
                movedVec.add(0.0, averageMotion.y, 0.0)
            } else {
                break
            }
        }

        return movedVec
    }

    private fun canMove(world: Level, bbox: AABB, offset: Vec3): Boolean {
        return world.noCollision(bbox.move(offset))
    }

    private fun currentPartialTicks(): Float =
        Wrapper.minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)
}
