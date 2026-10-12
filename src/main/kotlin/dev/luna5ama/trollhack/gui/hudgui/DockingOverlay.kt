package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.HAlign
import dev.luna5ama.trollhack.graphics.VAlign
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * The nine drop zones shown while right-dragging a HUD element, letting the user glue it to a
 * corner or edge so it stays put when the resolution changes.
 *
 * The original attached this to WindowComponent; in this port docking lives on
 * [AbstractHudElement], so the overlay is driven by the HUD editor instead. Layout, the 3x3 grid
 * on fifths, the labels and the 300ms OUT_QUART fade all match the original.
 */
class DockingOverlay {

    private val alphaFlag = AnimationFlag(Easing.OUT_QUART, 300.0f)

    /** True from the moment it is shown until the fade out has finished. */
    var active = false
        private set

    private var closing = false

    fun show() {
        active = true
        closing = false
        alphaFlag.forceUpdate(0.0f, 1.0f)
    }

    fun beginHide() {
        if (!active) return
        closing = true
        alphaFlag.update(0.0f)
    }

    fun tick() {
        if (closing && alphaFlag.get() <= 0.001f) {
            active = false
            closing = false
        }
    }

    /**
     * Which zone the cursor is over, or null when it is in one of the gaps between them.
     * The grid sits on fifths: zones occupy columns 0, 2 and 4, leaving a gap either side.
     */
    fun resolve(mouseX: Float, mouseY: Float, width: Float, height: Float): Pair<HAlign, VAlign>? {
        val h = pick(HAlign.VALUES, mouseX, width / 5.0f) ?: return null
        val v = pick(VAlign.VALUES, mouseY, height / 5.0f) ?: return null
        return h to v
    }

    private fun <T> pick(values: List<T>, v: Float, step: Float): T? = when (v) {
        in step * 0.0f..step * 1.0f -> values[0]
        in step * 2.0f..step * 3.0f -> values[1]
        in step * 4.0f..step * 5.0f -> values[2]
        else -> null
    }

    fun render(graphics: GuiGraphicsExtractor, width: Float, height: Float) {
        if (!active) return

        val alphaMul = alphaFlag.get()
        if (alphaMul <= 0.0f) return

        val fifthW = width / 5.0f
        val fifthH = height / 5.0f

        val background = GuiSetting.backGround
        val rectColor = background.alpha((background.a * alphaMul).toInt()).toArgb()
        val text = GuiSetting.text
        val textColor = text.alpha((text.a * alphaMul).toInt())

        fun zone(x: Int, y: Int, label: String) {
            graphics.fill(
                (fifthW * x).toInt(),
                (fifthH * y).toInt(),
                (fifthW * (x + 1)).toInt(),
                (fifthH * (y + 1)).toInt(),
                rectColor
            )

            val scale = 1.5f
            TrollFont.drawString(
                graphics,
                label,
                fifthW * (x + 0.5f) - TrollFont.getWidth(label, scale) / 2.0f,
                fifthH * (y + 0.5f) - TrollFont.getHeight(scale) / 2.0f,
                textColor,
                scale
            )
        }

        zone(0, 0, "Top Left")
        zone(2, 0, "Top Center")
        zone(4, 0, "Top Right")

        zone(0, 2, "Middle Left")
        zone(2, 2, "Middle Center")
        zone(4, 2, "Middle Right")

        zone(0, 4, "Bottom Left")
        zone(2, 4, "Bottom Center")
        zone(4, 4, "Bottom Right")
    }
}
