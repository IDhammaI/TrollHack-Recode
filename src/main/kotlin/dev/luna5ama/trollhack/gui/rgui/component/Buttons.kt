package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting

/**
 * Momentary action button: fills with primary while pressed, fires on non-drag release.
 */
open class Button(name: String, val action: (button: Int) -> Unit) : Slider(name) {

    override val progress: Float
        get() = if (mouseState == MouseState.CLICK || mouseState == MouseState.DRAG) 1.0f else 0.0f

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (!wasDrag && isInside(mouseX, mouseY)) {
            action(button)
        }
    }
}

/**
 * Latching toggle: full primary bar when on, empty when off (300ms OUT_QUART sweep).
 */
open class CheckButton(
    name: String,
    private val getState: () -> Boolean,
    private val setState: (Boolean) -> Unit
) : Slider(name) {

    override val progress: Float
        get() = if (getState()) 1.0f else 0.0f

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (!wasDrag && button == 0 && isInside(mouseX, mouseY)) {
            setState(!getState())
        }
    }
}

class SettingButton(private val setting: BooleanSetting) : CheckButton(
    setting.name.toString(),
    { setting.value },
    { setting.value = it }
)
