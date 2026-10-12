package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.InfoCalculator

internal object Ping : LabelHud(
    name = "Ping",
    category = Category.MISC,
    description = "Displays your ping"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add(InfoCalculator.ping().toString(), GuiColors.text)
        displayText.add("ms", GuiColors.primary)
    }
}
