package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud

internal object Fps : LabelHud(
    name = "Fps",
    category = Category.MISC,
    description = "Displays frames per second"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add(mc.fps.toString(), GuiColors.text)
        displayText.add("fps", GuiColors.primary)
    }
}
