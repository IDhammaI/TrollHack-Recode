package dev.luna5ama.trollhack.gui.clickgui.component

import dev.luna5ama.trollhack.gui.rgui.component.Slider
import dev.luna5ama.trollhack.module.AbstractModule

/**
 * Module row: enabled modules show a full-width primary sweep; left click toggles,
 * right click opens the floating setting window (handled by the screen).
 */
class ModuleButton(
    val module: AbstractModule,
    private val onOpenSettings: (ModuleButton) -> Unit
) : Slider(module.name.toString()) {

    override val progress: Float
        get() = if (module.isEnabled) 1.0f else 0.0f

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (wasDrag || !isInside(mouseX, mouseY)) return

        when (button) {
            0 -> module.toggle()
            1 -> onOpenSettings(this)
        }
    }
}
