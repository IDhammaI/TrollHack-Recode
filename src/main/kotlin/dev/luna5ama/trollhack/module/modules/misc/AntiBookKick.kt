package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.originalName
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.WrittenBookItem

/**
 * @author IronException
 * Used with permission from ForgeHax
 * https://github.com/fr1kin/ForgeHax/blob/bb522f8/src/main/java/com/matt/forgehax/mods/AntiBookKick.java
 * Permission (and ForgeHax is MIT licensed):
 * https://discordapp.com/channels/573954110454366214/634010802403409931/693919755647844352
 *
 * Degraded for 26.2: `ServerboundContainerClickPacket` no longer carries the clicked `ItemStack`
 * (1.12's `CPacketClickWindow.clickedItem`) - it only carries `HashedStack` digests, so the oversized
 * book NBT that caused the original kick is never sent in the first place. The detection therefore
 * reads the cursor stack that the packet was built from
 * (`MultiPlayerGameMode.handleContainerInput` hashes `containerMenu.getCarried()` right before sending),
 * which is exactly the stack this packet describes. As a side effect shift clicks no longer trip the
 * warning at all, which matches the message the module prints.
 *
 * The click is restricted to `ContainerInput.PICKUP` with a real slot index because that is the only
 * case 1.12's `Container.slotClick` returned a non-empty stack for (besides `QUICK_MOVE`, which is not
 * detectable from the cursor), and the only case where replaying the click actually undoes it.
 */
internal object AntiBookKick : Module(
    name = "Anti Book Kick",
    category = Category.MISC,
    description = "Prevents being kicked by clicking on books",
    visible = false
) {
    init {
        safeListener<PacketEvent.PostSend> {
            val packet = it.packet
            if (packet !is ServerboundContainerClickPacket) return@safeListener
            if (packet.containerInput() != ContainerInput.PICKUP) return@safeListener

            val slotNum = packet.slotNum().toInt()
            if (slotNum < 0) return@safeListener

            val container = player.containerMenu
            if (packet.containerId() != container.containerId) return@safeListener

            val stack = container.carried
            if (stack.isEmpty || stack.item !is WrittenBookItem) return@safeListener

            NoSpamMessage.sendWarning(
                AntiBookKick,
                chatName
                    + " Don't click the book \""
                    + stack.originalName
                    + "\", shift click it instead!"
            )
            container.clicked(slotNum, packet.buttonNum().toInt(), ContainerInput.PICKUP, player)
        }
    }
}
