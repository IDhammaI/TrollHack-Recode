package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.accessor.serverBrand

internal object ServerBrand : LabelHud(
    name = "Server Brand",
    category = Category.MISC,
    description = "Displays server brand"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add(connection.serverBrand ?: "Unknown", GuiColors.text)
    }
}
