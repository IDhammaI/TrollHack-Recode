package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.Cancellable
import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand

sealed class InteractEvent : Cancellable(), Event {
    sealed class Block(
        val pos: BlockPos,
        val side: Direction,
        val hand: InteractionHand
    ) : InteractEvent() {
        class LeftClick(pos: BlockPos, side: Direction) : Block(pos, side, InteractionHand.MAIN_HAND),
            EventPosting by Companion {
            companion object : EventBus()
        }

        class RightClick(pos: BlockPos, side: Direction, hand: InteractionHand = InteractionHand.MAIN_HAND) :
            Block(pos, side, hand),
            EventPosting by Companion {
            companion object : EventBus()
        }

        class Damage(pos: BlockPos, side: Direction) : Block(pos, side, InteractionHand.MAIN_HAND),
            EventPosting by Companion {
            companion object : EventBus()
        }
    }

    sealed class Item(
        val hand: InteractionHand
    ) : InteractEvent() {
        class RightClick(hand: InteractionHand) : Item(hand), EventPosting by Companion {
            companion object : EventBus()
        }
    }
}
