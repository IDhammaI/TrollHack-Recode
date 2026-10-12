package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting

internal object MemoryUsage : LabelHud(
    name = "Memory Usage",
    category = Category.MISC,
    description = "Displays memory usage"
) {
    private val showAllocated by setting("Show Allocated", false)
    private val showMax by setting("Show Max", false)

    override fun SafeClientEvent.updateText() {
        val runtime = Runtime.getRuntime()
        val allocated = runtime.totalMemory() / 1048576L
        val used = allocated - runtime.freeMemory() / 1048576L
        val max = runtime.maxMemory() / 1048576L

        displayText.add(used.toString(), GuiColors.text)
        displayText.add("MB", GuiColors.primary)

        if (showAllocated) {
            displayText.add("/ $allocated", GuiColors.text)
            displayText.add("MB", GuiColors.primary)
        }

        if (showMax) {
            displayText.add("/ $max", GuiColors.text)
            displayText.add("MB", GuiColors.primary)
        }
    }
}
