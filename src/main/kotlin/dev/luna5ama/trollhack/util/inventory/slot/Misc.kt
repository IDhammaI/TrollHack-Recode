package dev.luna5ama.trollhack.util.inventory.slot

import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

/**
 * Find an empty slot or slot that matches [predicate]
 * or slot 0 if none of those were found
 */
fun Player.anyHotbarSlot(predicate: Predicate<ItemStack>? = null): HotbarSlot {
    val hotbarSlots = this.hotbarSlots
    return hotbarSlots.firstEmpty()
        ?: hotbarSlots.firstByStack(predicate)
        ?: this.firstHotbarSlot
}

/**
 * Find an empty slot or slot 0
 */
fun Player.anyHotbarSlot() =
    this.hotbarSlots.firstEmpty()
        ?: this.firstHotbarSlot


val Slot.isHotbarSlot: Boolean
    get() = this.index in 36..44 && this.container == Wrapper.player?.inventory

val Slot.hotbarIndex get() = if (isHotbarSlot) this.index - 36 else -1

fun Slot.toHotbarSlotOrNull() =
    if (isHotbarSlot) HotbarSlot(this)
    else null
