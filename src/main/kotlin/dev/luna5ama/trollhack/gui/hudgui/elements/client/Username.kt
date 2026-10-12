package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting

internal object Username : LabelHud(
    name = "Username",
    category = Category.CLIENT,
    description = "Displays your username"
) {
    private val prefix by setting("Prefix", "Welcome")
    private val suffix by setting("Suffix", "")

    override fun SafeClientEvent.updateText() {
        if (prefix.isNotEmpty()) displayText.add(prefix, GuiColors.primary)
        displayText.add(mc.user.name, GuiColors.text)
        if (suffix.isNotEmpty()) displayText.add(suffix, GuiColors.primary)
    }
}
