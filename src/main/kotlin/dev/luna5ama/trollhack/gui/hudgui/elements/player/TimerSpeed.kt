package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.TimerManager

internal object TimerSpeed : LabelHud(
    name = "Timer Speed",
    category = Category.PLAYER,
    description = "Displays current timer speed"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add("%.2f".format(50.0f / TimerManager.tickLength), GuiColors.text)
        displayText.add("x", GuiColors.primary)
    }
}
