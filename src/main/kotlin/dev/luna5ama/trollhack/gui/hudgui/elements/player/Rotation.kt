package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.math.RotationUtils

internal object Rotation : LabelHud(
    name = "Rotation",
    category = Category.PLAYER,
    description = "Displays yaw and pitch"
) {
    override fun SafeClientEvent.updateText() {
        displayText.add("Yaw:", GuiColors.primary)
        displayText.add("%.1f".format(RotationUtils.normalizeAngle(player.yRot)), GuiColors.text)
        displayText.add("Pitch:", GuiColors.primary)
        displayText.add("%.1f".format(player.xRot), GuiColors.text)
    }
}
