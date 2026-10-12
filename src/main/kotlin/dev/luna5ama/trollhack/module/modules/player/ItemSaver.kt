package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.id
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.currentHotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstEmpty
import dev.luna5ama.trollhack.util.inventory.slot.firstID
import dev.luna5ama.trollhack.util.inventory.slot.inventorySlots
import dev.luna5ama.trollhack.util.inventory.slot.storageSlots
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

internal object ItemSaver : Module(
    name = "Item Saver",
    category = Category.PLAYER,
    description = "Saves your items from breaking",
    modulePriority = 20
) {
    private val duraThreshold by setting("Durability Threshold", 5, 1..50, 1)
    private val delayMs by setting("Delay ms", 50, 0..1000, 5)

    private var lastTask: InventoryTask? = null

    init {
        safeParallelListener<TickEvent.Post> {
            if (!lastTask.confirmedOrTrue) return@safeParallelListener
            if (!checkDamage(player.mainHandItem)) return@safeParallelListener

            val currentSlot = player.currentHotbarSlot
            val itemStack = player.mainHandItem
            val undamagedItem = getUndamagedItem(itemStack.item.id)
            val emptySlot = player.inventorySlots.firstEmpty()

            when {
                undamagedItem != null -> {
                    lastTask = inventoryTask {
                        swapWith(undamagedItem, currentSlot)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                }
                emptySlot != null -> {
                    lastTask = inventoryTask {
                        swapWith(emptySlot, currentSlot)
                        runInGui()
                        delay(0)
                        postDelay(delayMs)
                    }
                }
                else -> {
                    player.drop(false)
                }
            }
        }
    }

    private fun checkDamage(itemStack: ItemStack): Boolean {
        return (itemStack.isDamageableItem
            && itemStack.damageValue > itemStack.maxDamage * (1.0f - duraThreshold / 100.0f))
    }

    private fun SafeClientEvent.getUndamagedItem(itemID: Int): Slot? {
        return player.storageSlots.firstID(itemID) {
            !checkDamage(it)
        }
    }
}
