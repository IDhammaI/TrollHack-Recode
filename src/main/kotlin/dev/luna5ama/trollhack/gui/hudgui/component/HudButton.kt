package dev.luna5ama.trollhack.gui.hudgui.component

import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.rgui.component.Slider

/**
 * HUD element row: primary sweep when visible; left click toggles,
 * right click opens the floating setting window.
 */
class HudButton(
    val element: AbstractHudElement,
    private val onOpenSettings: (HudButton) -> Unit
) : Slider(element.name.toString()) {

    override val progress: Float
        get() = if (element.visible) 1.0f else 0.0f

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (wasDrag || !isInside(mouseX, mouseY)) return

        when (button) {
            0 -> element.visible = !element.visible
            1 -> onOpenSettings(this)
        }
    }
}
