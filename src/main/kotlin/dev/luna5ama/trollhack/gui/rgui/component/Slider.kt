package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

/**
 * Base of all row components: primary progress fill, hover overlay, animated name text,
 * right-aligned value text at 0.75 scale, and inline text input while listening.
 */
abstract class Slider(name: String) : InteractiveComponent(name) {

    /** 0..1 fill of the primary bar; NaN keeps the last rendered value (used while dragging). */
    protected abstract val progress: Float

    /** Text shown right-aligned at 0.75 scale; empty for none. */
    protected open val valueText: CharSequence get() = ""

    protected var listening = false
    val listeningState get() = listening
    protected var inputField = ""

    protected var protectedWidth = 0.0f

    private val renderProgress = AnimationFlag(Easing.OUT_QUART, 300.0f)
    private val hoverScaleFlag = AnimationFlag(Easing.OUT_BACK, 300.0f)
    private val clickedScaleFlag = AnimationFlag(Easing.OUT_CUBIC, 300.0f)

    override val minWidth: Float get() = TrollFont.getWidth(componentName) + 20.0f + protectedWidth
    override val minHeight: Float get() = TrollFont.getHeight() + 3.0f

    override fun onTick() {
        super.onTick()
        height = TrollFont.getHeight() + 3.0f
    }

    override fun onDisplayed() {
        super.onDisplayed()
        height = TrollFont.getHeight() + 3.0f
        renderProgress.forceUpdate(progress.takeIf { !it.isNaN() } ?: 0.0f)
    }

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)

        val w = width.roundToInt()
        val h = height.roundToInt()

        // Progress fill
        val renderedProgress = renderProgress.getAndUpdate(progress)
        if (renderedProgress > 0.0f) {
            graphics.fill(0, 0, (width * renderedProgress).roundToInt(), h, GuiSetting.primary.toArgb())
        }

        // Hover/click overlay
        val overlay = stateOverlayColor()
        if (overlay.a > 0) {
            graphics.fill(0, 0, w, h, overlay.toArgb())
        }

        // Animated name text (or input field while listening)
        val hovering = mouseState == MouseState.HOVER || mouseState == MouseState.CLICK || mouseState == MouseState.DRAG
        val clicking = mouseState == MouseState.CLICK || mouseState == MouseState.DRAG
        val hoverScale = hoverScaleFlag.getAndUpdate(if (hovering) 1.0f else 0.0f)
        val clickedScale = clickedScaleFlag.getAndUpdate(if (clicking) 1.0f else 0.0f)

        val fontHeight = TrollFont.getHeight()
        val textX = 2.0f + 2.0f * hoverScale
        val textY = 1.0f - 0.025f * hoverScale * fontHeight + 0.05f * clickedScale * fontHeight
        val textScale = 1.0f + 0.05f * hoverScale - 0.1f * clickedScale

        val nameText = if (listening) inputField + "_" else componentName
        TrollFont.drawString(graphics, nameText, textX, textY, GuiSetting.text, textScale)

        // Right-aligned value text
        val value = valueText
        if (value.isNotEmpty() && !listening) {
            val valueWidth = TrollFont.getWidth(value, 0.75f)
            protectedWidth = valueWidth
            TrollFont.drawString(
                graphics,
                value,
                width - valueWidth - 2.0f,
                height - 2.0f - TrollFont.getHeight(0.75f),
                GuiSetting.text,
                0.75f
            )
        }
    }
}
