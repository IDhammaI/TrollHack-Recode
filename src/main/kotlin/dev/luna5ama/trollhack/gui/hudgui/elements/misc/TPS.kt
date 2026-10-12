package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.TpsCalculator

internal object TPS : LabelHud(
    name = "TPS",
    category = Category.MISC,
    description = "Displays server TPS"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add("%.2f".format(TpsCalculator.tickRate), GuiColors.text)
        displayText.add("tps", GuiColors.primary)
    }
}
