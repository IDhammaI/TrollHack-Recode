package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.HotbarUpdateEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.hotbarIndex
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.inventory.slot.isHotbarSlot
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object HotbarSwitchManager : Manager() {
    var serverSideHotbar = 0; private set
    var swapTime = 0L; private set

    private var serverSizeItemOverride: ItemStack? = null

    val LocalPlayer.serverSideItem: ItemStack
        get() = serverSizeItemOverride ?: inventory.getItem(serverSideHotbar)

    init {
        safeListener<PacketEvent.Send>(Int.MIN_VALUE) {
            if (it.cancelled || it.packet !is ServerboundSetCarriedItemPacket) return@safeListener

            val prev: Int

            synchronized(InventoryTaskManager) {
                prev = serverSideHotbar
                serverSideHotbar = it.packet.slot
                swapTime = System.currentTimeMillis()
            }

            if (prev != it.packet.slot) {
                HotbarUpdateEvent(prev, serverSideHotbar).post()
            }
        }
    }

    fun SafeClientEvent.ghostSwitch(slot: Slot, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(slot: Int, block: () -> Unit) {
        ghostSwitch(Override.DEFAULT, slot, block)
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Slot, block: () -> Unit) {
        synchronized(InventoryTaskManager) {
            serverSizeItemOverride = slot.item
            if (slot.hotbarIndex != serverSideHotbar) {
                override.mode.run {
                    switch(slot, block)
                }
                return
            }
            serverSizeItemOverride = null
        }

        block.invoke()
    }

    fun SafeClientEvent.ghostSwitch(override: Override, slot: Int, block: () -> Unit) {
        ghostSwitch(override, player.hotbarSlots[slot], block)
    }

    /**
     * Sends a raw container click without touching client side state.
     * 26.2 uses hashed stacks + state ids; we report no predicted changes and
     * let the server force a resync if it disagrees.
     */
    private fun SafeClientEvent.sendGhostClick(slot: Int, button: Int, type: ContainerInput) {
        val menu = player.inventoryMenu
        val carried = HashedStack.create(menu.carried, connection.decoratedHashOpsGenenerator())

        connection.send(
            ServerboundContainerClickPacket(
                0,
                menu.stateId,
                slot.toShort(),
                button.toByte(),
                type,
                Int2ObjectOpenHashMap(),
                carried
            )
        )
    }

    enum class BypassMode {
        NONE {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                if (!targetSlot.isHotbarSlot) {
                    SWAP.run {
                        switch(targetSlot, block)
                        return
                    }
                }

                val prevSlot = serverSideHotbar
                connection.send(ServerboundSetCarriedItemPacket(targetSlot.hotbarIndex))
                block.invoke()
                connection.send(ServerboundSetCarriedItemPacket(prevSlot))
            }
        },
        MOVE {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                val hotbarSlots = player.hotbarSlots

                sendGhostClick(targetSlot.index, 0, ContainerInput.PICKUP)
                sendGhostClick(hotbarSlots[serverSideHotbar].index, 0, ContainerInput.PICKUP)

                block.invoke()

                sendGhostClick(hotbarSlots[serverSideHotbar].index, 0, ContainerInput.PICKUP)
                sendGhostClick(targetSlot.index, 0, ContainerInput.PICKUP)
            }
        },
        SWAP {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                sendGhostClick(targetSlot.index, serverSideHotbar, ContainerInput.SWAP)
                block.invoke()
                sendGhostClick(targetSlot.index, serverSideHotbar, ContainerInput.SWAP)
            }
        },
        PICK {
            override fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit) {
                // 26.2 removed the pick-from-slot packet; fall back to SWAP
                SWAP.run {
                    switch(targetSlot, block)
                }
            }
        };

        abstract fun SafeClientEvent.switch(targetSlot: Slot, block: () -> Unit)
    }

    enum class Override {
        DEFAULT {
            override val mode get() = Bypass.ghostSwitchBypass
        },
        NONE {
            override val mode = BypassMode.NONE
        },
        MOVE {
            override val mode = BypassMode.MOVE
        },
        SWAP {
            override val mode = BypassMode.SWAP
        },
        PICK {
            override val mode = BypassMode.PICK
        };

        abstract val mode: BypassMode
    }
}
