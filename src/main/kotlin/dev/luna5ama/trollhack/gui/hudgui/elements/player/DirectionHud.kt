package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.math.Direction

internal object DirectionHud : LabelHud(
    name = "Direction",
    category = Category.PLAYER,
    description = "Displays facing direction"
) {
    override fun SafeClientEvent.updateText() {
        val direction = Direction.fromEntity(mc.cameraEntity ?: player)
        displayText.add(direction.displayName, GuiColors.text)
        displayText.add(direction.displayNameXY, GuiColors.primary)
    }
}
