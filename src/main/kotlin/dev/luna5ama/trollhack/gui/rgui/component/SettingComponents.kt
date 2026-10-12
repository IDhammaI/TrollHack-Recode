package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.setting.settings.impl.number.NumberSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting
import dev.luna5ama.trollhack.util.KeyboardUtils
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.lwjgl.glfw.GLFW
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Number slider: bar shows (value - min) / range; left drag sets, right click types.
 */
class SettingSlider(private val setting: NumberSetting<*>) : Slider(setting.name.toString()) {

    private val min get() = (setting.range.start as Number).toDouble()
    private val max get() = (setting.range.endInclusive as Number).toDouble()

    private var dragging = false

    override val progress: Float
        get() = if (dragging || listening) Float.NaN
        else (((setting.value.toDouble() - min) / (max - min)).coerceIn(0.0, 1.0)).toFloat()

    override val valueText: CharSequence get() = setting.toString()

    private fun setFromMouse(mouseX: Float) {
        val range = max - min
        val step = setting.step.toDouble().takeIf { it > 0.0 } ?: (range / 20.0)
        val fraction = ((mouseX - posX) / width).coerceIn(0.0f, 1.0f)

        var value = min + fraction * range
        value = min + ((value - min) / step).roundToInt() * step
        setting.setValue(value.coerceIn(min, max))
    }

    override fun onClick(mouseX: Float, mouseY: Float, button: Int) {
        super.onClick(mouseX, mouseY, button)
        if (button == 1) {
            listening = !listening
            inputField = if (listening) setting.value.toString() else ""
        } else if (button == 0 && !listening) {
            dragging = true
            setFromMouse(mouseX)
        } else if (button == 0 && listening) {
            commitInput()
        }
    }

    override fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        super.onDrag(mouseX, mouseY, clickX, clickY, button)
        if (button == 0 && dragging) {
            setFromMouse(mouseX)
        }
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        dragging = false
        super.onRelease(mouseX, mouseY, button)
    }

    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        if (!listening || !state) return listening

        when (key) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitInput()
            GLFW.GLFW_KEY_ESCAPE -> {
                listening = false
                inputField = ""
            }
            GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> {
                inputField = inputField.dropLast(1).ifEmpty { "0" }
            }
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (!listening) return false
        if (char.isDigit() || char == '-' || char == '.' || char == 'e') {
            inputField += char
        }
        return true
    }

    private fun commitInput() {
        inputField.toDoubleOrNull()?.let { setting.setValue(it.coerceIn(min, max)) }
        listening = false
        inputField = ""
    }
}

/**
 * Enum slider: fill maps ordinal proportionally; any non-drag release cycles forward,
 * dragging scrubs through the values.
 */
class EnumSlider(private val setting: EnumSetting<*>) : Slider(setting.name.toString()) {

    private var dragging = false

    override val progress: Float
        get() {
            if (dragging) return Float.NaN
            val n = setting.enumValues.size
            if (n <= 1) return 1.0f
            val ordinal = setting.value.ordinal
            return (ordinal + ordinal.toFloat() / (n - 1)) / n
        }

    override val valueText: CharSequence
        get() = (setting.value as? DisplayEnum)?.displayName ?: setting.value.name

    override fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        super.onDrag(mouseX, mouseY, clickX, clickY, button)
        if (button == 0) {
            dragging = true
            val n = setting.enumValues.size
            val fraction = ((mouseX - posX) / width).coerceIn(0.0f, 0.999f)
            val index = floor(fraction * n).toInt().coerceIn(0, n - 1)
            setEnumOrdinal(index)
        }
    }

    private fun setEnumOrdinal(index: Int) {
        var guard = 0
        while (setting.value.ordinal != index && guard++ < setting.enumValues.size) {
            setting.nextValue()
        }
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        dragging = false
        if (!wasDrag && isInside(mouseX, mouseY)) {
            setting.nextValue()
        }
    }
}

/**
 * String field: bar is full normally, drains while editing; click to edit, Enter commits.
 */
class StringButton(private val setting: StringSetting) : Slider(setting.name.toString()) {

    override val progress: Float get() = if (listening) 0.0f else 1.0f

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (wasDrag) return

        if (!listening) {
            listening = true
            inputField = setting.value
        } else if (button == 0) {
            setting.value = inputField
            listening = false
        } else {
            listening = false
            inputField = ""
        }
    }

    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        if (!listening || !state) return listening

        when (key) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                setting.value = inputField
                listening = false
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                listening = false
                inputField = ""
            }
            GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> {
                inputField = inputField.dropLast(1)
            }
        }
        return true
    }

    override fun onCharTyped(char: Char): Boolean {
        if (!listening) return false
        if (char >= ' ') inputField += char
        return true
    }
}

/**
 * Bind button: right side shows the bind or "Listening"; any release toggles listening,
 * next key press binds (Escape/Backspace/Delete clears).
 */
class BindButton(private val setting: BindSetting) : Slider(setting.name.toString()) {

    override val progress: Float get() = 0.0f

    override val valueText: CharSequence
        get() = if (listening) "Listening" else setting.value.toString()

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (wasDrag) return

        if (listening && button > 1) {
            setting.value.setBind(IntArrayList(), -button - 1)
            listening = false
        } else {
            listening = !listening
        }
    }

    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        if (!listening) return false
        if (state) return true // capture on release like the original

        when (key) {
            GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE -> setting.value.clear()
            else -> {
                if (key != KeyboardUtils.KEY_NONE) {
                    setting.value.setBind(IntArrayList(), key)
                }
            }
        }
        listening = false
        return true
    }
}
