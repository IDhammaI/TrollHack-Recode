package dev.luna5ama.trollhack.util.inventory

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager
import dev.luna5ama.trollhack.util.inventory.operation.swapToSlot
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.craftingSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstEmpty
import dev.luna5ama.trollhack.util.inventory.slot.getSlots
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.state.BlockState

fun SafeClientEvent.equipBestTool(blockState: BlockState) {
    findBestTool(blockState)?.let {
        swapToSlot(it)
    }
}

fun SafeClientEvent.findBestTool(blockState: BlockState): Slot? {
    var maxSpeed = 0.0f
    var bestSlot: Slot? = null

    for (slot in player.allSlotsPrioritized) {
        val stack = slot.item

        if (stack.isEmpty || !stack.item.isTool) {
            continue
        } else {
            var speed = stack.getDestroySpeed(blockState)

            if (speed > 1.0f) {
                val efficiency = stack.getEnchantmentLevel(Enchantments.EFFICIENCY)
                if (efficiency > 0) {
                    speed += efficiency * efficiency + 1.0f
                }
            }

            if (speed > maxSpeed) {
                maxSpeed = speed
                bestSlot = slot
            }
        }
    }

    return bestSlot
}

/**
 * Put the item currently holding by mouse to somewhere or throw it
 */
fun SafeClientEvent.removeHoldingItem() {
    if (player.inventoryMenu.carried.isEmpty) return

    val slot = player.inventoryMenu.getSlots(9..45)
        .firstEmpty()?.index // Get empty slots in inventory and offhand
        ?: player.craftingSlots.firstEmpty()?.index // Get empty slots in crafting slot
        ?: -999 // Throw on the ground

    clickSlot(0, slot, 0, ContainerInput.PICKUP)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id (always 0 on 26.2, kept for API compatibility)
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Slot, mouseButton: Int, type: ContainerInput): Short {
    return clickSlot(windowID, slot.index, mouseButton, type)
}

/**
 * Performs inventory clicking in specific window, slot, mouseButton, and click type
 *
 * @return Transaction id (always 0 on 26.2, kept for API compatibility)
 */
fun SafeClientEvent.clickSlot(windowID: Int, slot: Int, mouseButton: Int, type: ContainerInput): Short {
    synchronized(InventoryTaskManager) {
        getContainerForID(windowID) ?: return -32768

        playerController.handleContainerInput(windowID, slot, mouseButton, type, player)

        return 0
    }
}

private fun SafeClientEvent.getContainerForID(windowID: Int): AbstractContainerMenu? {
    return if (windowID == 0) player.inventoryMenu
    else player.containerMenu.takeIf { it.containerId == windowID }
}

fun ItemStack.isStackable(other: ItemStack): Boolean {
    return this.count < this.maxStackSize && isCompatible(other)
}

fun ItemStack.isCompatible(other: ItemStack): Boolean {
    return this.isEmpty || other.isEmpty || ItemStack.isSameItemSameComponents(this, other)
}
