package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud

internal object Watermark : LabelHud(
    name = "Watermark",
    category = Category.CLIENT,
    description = "Renders client name and version",
    enabledByDefault = true
) {
    override fun SafeClientEvent.updateText() {
        displayText.add(TrollHackMod.NAME, GuiColors.primary)
        displayText.add(TrollHackMod.VERSION, GuiColors.text)
    }
}
