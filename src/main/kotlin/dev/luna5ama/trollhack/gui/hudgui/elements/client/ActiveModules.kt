package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.setting.GuiConfig.setting

internal object ActiveModules : LabelHud(
    name = "Active Modules",
    alias = arrayOf("ArrayList"),
    category = Category.CLIENT,
    description = "Displays active modules",
    enabledByDefault = true,
    defaultDockingH = dev.luna5ama.trollhack.graphics.HAlign.RIGHT,
    defaultDockingV = dev.luna5ama.trollhack.graphics.VAlign.TOP,
    defaultPosX = -2.0f,
    defaultPosY = 2.0f
) {
    private val showHudInfo by setting("Show Hud Info", true)
    private val sortByWidth by setting("Sort By Width", true)

    override fun SafeClientEvent.updateText() {
        val active = ModuleManager.modules.asSequence()
            .filter { it.isEnabled && it.isVisible }
            .map { module ->
                val info = if (showHudInfo) module.getHudInfo() else ""
                val text = if (info.isNotEmpty()) "${module.name} [$info]" else module.name.toString()
                text
            }
            .toMutableList()

        if (sortByWidth) {
            // Approximate width by length; vanilla font measuring is render-thread only
            active.sortByDescending { it.length }
        } else {
            active.sort()
        }

        for (line in active) {
            displayText.addLine(line, GuiColors.primary)
        }
    }
}
