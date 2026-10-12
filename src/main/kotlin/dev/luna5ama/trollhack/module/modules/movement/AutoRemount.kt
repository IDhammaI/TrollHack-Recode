package dev.luna5ama.trollhack.module.modules.movement

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.equine.Donkey
import net.minecraft.world.entity.animal.equine.Horse
import net.minecraft.world.entity.animal.equine.Llama
import net.minecraft.world.entity.animal.equine.Mule
import net.minecraft.world.entity.animal.equine.SkeletonHorse
import net.minecraft.world.entity.animal.pig.Pig
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import net.minecraft.world.phys.EntityHitResult

internal object AutoRemount : Module(
    name = "Auto Remount",
    description = "Automatically remounts your horse",
    category = Category.MOVEMENT
) {
    private val boat by setting("Boats", true)
    private val horse by setting("Horse", true)
    private val skeletonHorse by setting("Skeleton Horse", true)
    private val donkey by setting("Donkey", true)
    private val mule by setting("Mule", true)
    private val pig by setting("Pig", true)
    private val llama by setting("Llama", true)
    private val range by setting("Range", 2.0f, 1.0f..5.0f, 0.5f)
    private val remountDelay by setting("Remount Delay", 5, 0..10, 1)

    private val remountTimer = TickTimer(TimeUnit.TICKS)

    init {
        safeListener<TickEvent.Pre> {
            // we don't need to do anything if we're already riding.
            if (player.isPassenger) {
                remountTimer.reset()
                return@safeListener
            }

            if (remountTimer.tickAndReset(remountDelay.toLong())) {
                EntityManager.entity.asSequence()
                    .filter(::isValidEntity)
                    .minByOrNull { player.distanceSqTo(it) }
                    ?.let {
                        if (player.distanceTo(it) < range) {
                            playerController.interact(player, it, EntityHitResult(it), InteractionHand.MAIN_HAND)
                        }
                    }
            }
        }
    }

    private fun isValidEntity(entity: Entity): Boolean {
        return boat && entity is AbstractBoat
            || entity is Animal && !entity.isBaby // FBI moment
            && (horse && entity is Horse
            || skeletonHorse && entity is SkeletonHorse
            || donkey && entity is Donkey
            || mule && entity is Mule
            || pig && entity is Pig && entity.isSaddled
            || llama && entity is Llama)
    }
}
