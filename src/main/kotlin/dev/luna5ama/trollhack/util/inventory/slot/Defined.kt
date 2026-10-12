package dev.luna5ama.trollhack.util.inventory.slot

import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.inventory.Slot

val Player.allSlots: List<Slot>
    get() = inventoryMenu.getSlots(1..45)

val Player.allSlotsPrioritized: List<Slot>
    get() = mutableListOf<Slot>(offhandSlot).apply {
        val hotbarSlots = hotbarSlots
        val current = HotbarSwitchManager.serverSideHotbar
        add(hotbarSlots[current])
        for (i in hotbarSlots.indices) {
            if (i != current) add(hotbarSlots[i])
        }
        addAll(inventoryMenu.getSlots(1..35))
    }

val Player.armorSlots: List<Slot>
    get() = inventoryMenu.getSlots(5..8)

val Player.headSlot: Slot
    get() = inventoryMenu.slots[5]

val Player.chestSlot: Slot
    get() = inventoryMenu.slots[6]

val Player.legsSlot: Slot
    get() = inventoryMenu.slots[7]

val Player.feetSlot: Slot
    get() = inventoryMenu.slots[8]

val Player.offhandSlot: Slot
    get() = inventoryMenu.slots[45]

val Player.craftingSlots: List<Slot>
    get() = inventoryMenu.getSlots(1..4)

val Player.inventorySlots: List<Slot>
    get() = inventoryMenu.getSlots(9..44)

val Player.storageSlots: List<Slot>
    get() = inventoryMenu.getSlots(9..35)

val Player.hotbarSlots: List<HotbarSlot>
    get() = ArrayList<HotbarSlot>().apply {
        for (slot in 36..44) {
            add(HotbarSlot(inventoryMenu.slots[slot]))
        }
    }

val Player.currentHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryMenu.getSlot(inventory.selectedSlot + 36))

val Player.firstHotbarSlot: HotbarSlot
    get() = HotbarSlot(inventoryMenu.getSlot(36))

fun Player.getHotbarSlot(slot: Int): HotbarSlot {
    if (slot !in 0..8) throw IllegalArgumentException("Invalid hotbar slot: $slot")
    return HotbarSlot(inventoryMenu.slots[slot + 36])
}

fun AbstractContainerMenu.getContainerSlots(): List<Slot> =
    getSlots(0 until getContainerSlotSize())

fun AbstractContainerMenu.getPlayerSlots(): List<Slot> {
    val size = getContainerSlotSize()
    return getSlots(size until size + 36)
}

fun AbstractContainerMenu.getSlots(range: IntRange): List<Slot> =
    slots.subList(range.first, range.last + 1)

fun AbstractContainerMenu.getContainerSlotSize(): Int {
    return if (this is InventoryMenu) 0 else this.slots.size - 36
}
