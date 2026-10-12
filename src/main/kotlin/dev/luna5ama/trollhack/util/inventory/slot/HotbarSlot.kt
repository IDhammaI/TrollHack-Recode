package dev.luna5ama.trollhack.util.inventory.slot

import net.minecraft.world.inventory.Slot

class HotbarSlot(slot: Slot) : Slot(slot.container, slot.containerSlot, slot.x, slot.y) {
    init {
        index = slot.index
    }

    val hotbarSlot = slot.index - 36

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HotbarSlot) return false

        return hotbarSlot == other.hotbarSlot
    }

    override fun hashCode(): Int {
        return hotbarSlot
    }
}
