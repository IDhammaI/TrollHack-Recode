package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

/**
 * Colour setting row: shows a swatch of the current colour; clicking asks the
 * screen to open a [dev.luna5ama.trollhack.gui.rgui.windows.ColorPicker].
 */
class ColorButton(
    val setting: ColorSetting,
    private val onOpenPicker: (ColorButton) -> Unit
) : Slider(setting.name.toString()) {

    override val progress: Float
        get() = if (mouseState == MouseState.CLICK || mouseState == MouseState.DRAG) 1.0f else 0.0f

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)

        val swatchW = 24
        val x = (width - swatchW - 2.0f).roundToInt()
        val h = height.roundToInt()
        graphics.fill(x, 2, x + swatchW, h - 2, setting.value.toArgb())
        graphics.outline(x, 2, swatchW, h - 4, GuiSetting.primary.alpha(255).toArgb())
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        val wasDrag = mouseState == MouseState.DRAG
        super.onRelease(mouseX, mouseY, button)
        if (!wasDrag && isInside(mouseX, mouseY)) {
            onOpenPicker(this)
        }
    }
}
