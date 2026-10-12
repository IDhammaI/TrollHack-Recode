package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import net.minecraft.world.level.Level

internal object Coordinate : LabelHud(
    name = "Coordinate",
    alias = arrayOf("Coords", "Coordinates"),
    category = Category.WORLD,
    description = "Displays your coordinates",
    enabledByDefault = true,
    defaultDockingH = dev.luna5ama.trollhack.graphics.HAlign.LEFT,
    defaultDockingV = dev.luna5ama.trollhack.graphics.VAlign.BOTTOM,
    defaultPosX = 2.0f,
    defaultPosY = -2.0f
) {
    private val showY by setting("Show Y", true)
    private val showConverted by setting("Show Nether/Overworld", true)
    private val decimalPlaces by setting("Decimal Places", 1, 0..4, 1)

    override fun SafeClientEvent.updateText() {
        val format = "%.${decimalPlaces}f"

        displayText.add("XYZ", GuiColors.primary)
        displayText.add(format.format(player.x), GuiColors.text)
        if (showY) displayText.add(format.format(player.y), GuiColors.text)
        displayText.add(format.format(player.z), GuiColors.text)

        if (showConverted) {
            when (world.dimension()) {
                Level.OVERWORLD -> {
                    displayText.add("(", GuiColors.primary)
                    displayText.add(format.format(player.x / 8.0), GuiColors.text)
                    displayText.add(format.format(player.z / 8.0), GuiColors.text)
                    displayText.add(")", GuiColors.primary)
                }
                Level.NETHER -> {
                    displayText.add("(", GuiColors.primary)
                    displayText.add(format.format(player.x * 8.0), GuiColors.text)
                    displayText.add(format.format(player.z * 8.0), GuiColors.text)
                    displayText.add(")", GuiColors.primary)
                }
                else -> {}
            }
        }
    }
}
