package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult

internal object MidClickPearl : Module(
    name = "Mid Click Pearl",
    category = Category.COMBAT,
    description = "Throws a pearl automatically when you middle click in air"
) {
    init {
        safeListener<InputEvent.Mouse> {
            if (it.state || it.button != 2) return@safeListener

            val hitResult = mc.hitResult
            if (hitResult == null || hitResult.type != HitResult.Type.BLOCK) {
                val pearlSlot = player.hotbarSlots.firstItem(Items.ENDER_PEARL)

                if (pearlSlot != null) {
                    ghostSwitch(pearlSlot) {
                        connection.send(
                            ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, player.yRot, player.xRot)
                        )
                    }
                } else {
                    NoSpamMessage.sendMessage("No Ender Pearl was found in hotbar!")
                }
            }
        }
    }
}
