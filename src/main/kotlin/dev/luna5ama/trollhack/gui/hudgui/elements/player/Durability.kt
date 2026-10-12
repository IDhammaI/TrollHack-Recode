package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.inventory.duraPercentage
import dev.luna5ama.trollhack.util.inventory.durability
import dev.luna5ama.trollhack.util.inventory.originalName

internal object Durability : LabelHud(
    name = "Durability",
    category = Category.PLAYER,
    description = "Displays durability of held items"
) {
    private val showItemName by setting("Show Item Name", false)
    private val showOffhand by setting("Show Offhand", true)
    private val showPercentage by setting("Show Percentage", false)

    override fun SafeClientEvent.updateText() {
        val mainHand = player.mainHandItem
        if (!mainHand.isEmpty && mainHand.isDamageableItem) {
            if (showItemName) displayText.add(mainHand.originalName, GuiColors.primary)
            displayText.add(
                if (showPercentage) "${mainHand.duraPercentage}%" else mainHand.durability.toString(),
                GuiColors.text
            )
        }

        if (showOffhand) {
            val offhand = player.offhandItem
            if (!offhand.isEmpty && offhand.isDamageableItem) {
                displayText.newLine()
                if (showItemName) displayText.add(offhand.originalName, GuiColors.primary)
                displayText.add(
                    if (showPercentage) "${offhand.duraPercentage}%" else offhand.durability.toString(),
                    GuiColors.text
                )
            }
        }
    }
}
