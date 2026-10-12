package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.ColorUtils
import dev.luna5ama.trollhack.gui.rgui.component.Button
import dev.luna5ama.trollhack.gui.rgui.component.SettingSlider
import dev.luna5ama.trollhack.gui.rgui.component.Slider
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.math.MathUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Faithful port of the 1.12 `gui/rgui/windows/ColorPicker.kt`.
 *
 * Layout, colours, ordering and arithmetic are copied verbatim from the original: a square
 * saturation/brightness field on the left, a vertical 8px hue bar 6px to its right with a
 * triangle pointer on each side, four Red/Green/Blue/Alpha [SettingSlider] rows of width 128
 * stacked down the right side, then Okay/Cancel/Apply [Button]s of width 50, and the
 * previous-vs-current colour swatches (35 wide, 4 apart) to the left of the buttons. The window
 * is centred on the screen and the setting is only written on Apply/Okay.
 *
 * 26.2 deviations (everything else is 1:1):
 * - There is no immediate-mode `RenderUtils2D`, so shapes are built from
 *   [GuiGraphicsExtractor.fill] / [GuiGraphicsExtractor.fillGradient] quads:
 *   * `fillGradient` only interpolates along its local +y (`col1` sits on the `y0` vertices,
 *     `col2` on the `y1` vertices). The black brightness overlay and the six-stop hue bar are
 *     already vertical; the horizontal white -> hue saturation ramp is emitted in a frame
 *     rotated by -90 degrees, so it is still a single hardware gradient like the original.
 *   * The circular field pointer and the two triangular hue pointers are scanline-rasterised
 *     from axis-aligned quads under a 1/[SUB] pose scale so they keep sub-unit precision. Both
 *     are drawn as 1.5-thick outlines like the original; what is lost is the `GL_LINE_SMOOTH`
 *     antialiasing (`GlStateUtils.smooth` has no 26.2 equivalent), so the edges are hard.
 *     The original's line width was a `glLineWidth` call, i.e. device pixels rather than troll
 *     units - it is kept as 1.5 troll units here so the pointers do not vanish at high scales.
 * - `WindowComponent` has no docking, so the original's CENTER/CENTER docking is recomputed
 *   from the live [Resolution] size every time the window is displayed. `screenWidth`,
 *   `screenHeight`, `mouseX` and `mouseY` are accepted only to keep the constructor signature
 *   `WindowScreen.openColorPicker` calls; the original never spawned this window at the cursor
 *   and a snapshot of the screen size would be stale after a resize re-runs `Screen.init`.
 * - The original called `screen.closeWindow(this)`; here [closeRequested] is raised and
 *   `WindowScreen` removes the window on the next tick.
 * - `WindowScreen` only forwards positional hover to `ListWindow`, so the hovered child is
 *   resolved from the mouse handler in [onHover]. [onRelease] recomputes the cursor position
 *   the same way, because `WindowScreen.mouseReleased` currently forwards raw gui-scaled
 *   coordinates instead of troll units.
 *
 * Known original quirk kept on purpose: [updateValues] measures the click 4 units below the top
 * of the field (`mouseY - draggableHeight - 4`) while the field itself starts at
 * `draggableHeight`. That is the original's own arithmetic and its pointer offset is reproduced
 * here rather than silently corrected.
 */
class ColorPicker @Suppress("UNUSED_PARAMETER") constructor(
    private val setting: ColorSetting,
    screenWidth: Float,
    screenHeight: Float,
    mouseX: Float,
    mouseY: Float
) : TitledWindow("Color Picker") {

    override val minimizable = false

    /** Set when Okay/Cancel is pressed; the screen removes the window. */
    var closeRequested = false; private set

    // Positions
    private var fieldHeight = 0.0f
    private val fieldRect = Rect()
    private val hueRect = Rect()
    private val prevColorRect = Rect()
    private val currentColorRect = Rect()

    // Main values
    private var hue = 0.0f
    private var saturation = 1.0f
    private var brightness = 1.0f
    private var prevHue = 0.0f
    private var prevSaturation = 1.0f
    private var prevBrightness = 1.0f

    // Sliders
    private val r = IntegerSetting("Red", setting.value.r, 0..255, 1)
    private val g = IntegerSetting("Green", setting.value.g, 0..255, 1)
    private val b = IntegerSetting("Blue", setting.value.b, 0..255, 1)
    private val a = IntegerSetting("Alpha", setting.value.a, 0..255, 1, { setting.hasAlpha })
    private val sliderR = SettingSlider(r)
    private val sliderG = SettingSlider(g)
    private val sliderB = SettingSlider(b)
    private val sliderA = SettingSlider(a).apply { visibility = { a.isVisible } }

    // Buttons
    private val buttonOkay = Button("Okay") { actionOk() }
    private val buttonCancel = Button("Cancel") { actionCancel() }
    private val buttonApply = Button("Apply") { actionApply() }

    private val components = arrayOf<Slider>(
        sliderR, sliderG, sliderB, sliderA, buttonOkay, buttonCancel, buttonApply
    )

    private var hoveredChild: Slider? = null
        set(value) {
            if (value == field) return
            field?.onLeave()
            value?.onHover()
            field = value
        }

    private var clickedChild: Slider? = null

    // ---- Lifecycle ----

    override fun onDisplayed() {
        r.value = setting.value.r
        g.value = setting.value.g
        b.value = setting.value.b
        a.value = setting.value.a

        // Children size themselves first so the window can be laid out around them
        for (component in components) component.onDisplayed()

        updateHSBFromRGB()
        prevHue = hue
        prevSaturation = saturation
        prevBrightness = brightness

        updatePos()
        centerOnScreen()

        // Runs last: it snapshots the final pos/size into the render animation flags
        super.onDisplayed()
    }

    override fun onClosed() {
        super.onClosed()
        hoveredChild = null
        clickedChild = null
        for (component in components) component.onClosed()
    }

    override fun onTick() {
        super.onTick()
        prevHue = hue
        prevSaturation = saturation
        prevBrightness = brightness
        for (component in components) component.onTick()
        if (hoveredChild != null) updateHSBFromRGB()
        updatePos()
    }

    // ---- Input ----

    override fun onHover() {
        super.onHover()
        val mouseX = trollMouseX() - renderPosX
        val mouseY = trollMouseY() - renderPosY
        hoveredChild = components.lastOrNull { it.visible && it.isInside(mouseX, mouseY) }
    }

    override fun onLeave() {
        super.onLeave()
        hoveredChild = null
    }

    override fun onClick(mouseX: Float, mouseY: Float, button: Int) {
        super.onClick(mouseX, mouseY, button)

        val localX = mouseX - posX
        val localY = mouseY - posY
        val child = components.lastOrNull { it.visible && it.isInside(localX, localY) }
        clickedChild = child

        if (child != null) {
            child.onClick(localX, localY, button)
        } else {
            updateValues(localX, localY, localX, localY)
        }
    }

    override fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        super.onDrag(mouseX, mouseY, clickX, clickY, button)

        val localX = mouseX - posX
        val localY = mouseY - posY
        val localClickX = clickX - posX
        val localClickY = clickY - posY

        val child = clickedChild
        if (child != null) {
            child.onDrag(localX, localY, localClickX, localClickY, button)
        } else {
            updateValues(localX, localY, localClickX, localClickY)
        }
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        // The passed coordinates are gui-scaled rather than troll units, so they are re-derived
        // here; once WindowScreen converts them this resolves to the exact same value.
        val correctedX = trollMouseX() - renderPosX + posX
        val correctedY = trollMouseY() - renderPosY + posY
        super.onRelease(correctedX, correctedY, button)

        val localX = correctedX - posX
        val localY = correctedY - posY

        val child = clickedChild
        clickedChild = null

        if (child != null) {
            child.onRelease(localX, localY, button)
        } else {
            updateValues(localX, localY, localX, localY)
        }
    }

    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        for (component in components) {
            if (component.onKeyInput(key, state)) return true
        }
        return false
    }

    override fun onCharTyped(char: Char): Boolean {
        for (component in components) {
            if (component.onCharTyped(char)) return true
        }
        return false
    }

    /**
     * The original dispatched mouse input to the topmost hovered window only, so nothing under
     * the picker ever saw the wheel. `WindowScreen.mouseScrolled` instead walks down the z-order
     * until a window returns true, so the picker has to swallow it itself - otherwise scrolling
     * over the picker scrolls the module list behind it.
     */
    override fun onScroll(amount: Double): Boolean = true

    private fun updateValues(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float) {
        val relativeX = mouseX - 4.0f
        val relativeY = mouseY - draggableHeight - 4.0f

        if (fieldRect.contains(clickX, clickY)) {
            saturation = (relativeX / fieldHeight).coerceIn(0.0f, 1.0f)
            brightness = (1.0f - relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        } else if (hueRect.contains(clickX, clickY)) {
            hue = (relativeY / fieldHeight).coerceIn(0.0f, 1.0f)
            updateRGBFromHSB()
        }
    }

    // ---- Actions ----

    private fun actionOk() {
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
        closeRequested = true
    }

    private fun actionCancel() {
        closeRequested = true
    }

    private fun actionApply() {
        setting.value = ColorRGB(r.value, g.value, b.value, a.value)
    }

    private fun updateRGBFromHSB() {
        val color = ColorUtils.hsbToRGB(hue, saturation, brightness)
        r.value = color.r
        g.value = color.g
        b.value = color.b
    }

    private fun updateHSBFromRGB() {
        val color = ColorUtils.rgbToHSB(r.value, g.value, b.value, 0)
        hue = color.h
        saturation = color.s
        brightness = color.b
    }

    // ---- Rendering ----

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)

        drawColorField(graphics)
        drawHueSlider(graphics)
        drawColorPreview(graphics)

        for (component in components) {
            if (!component.visible) continue
            graphics.pose().pushMatrix()
            graphics.pose().translate(component.posX, component.posY)
            component.onRender(graphics)
            graphics.pose().popMatrix()
        }
    }

    override fun onPostRender(graphics: GuiGraphicsExtractor) {
        super.onPostRender(graphics)

        for (component in components) {
            if (!component.visible) continue
            graphics.pose().pushMatrix()
            graphics.pose().translate(component.posX, component.posY)
            component.onPostRender(graphics)
            graphics.pose().popMatrix()
        }
    }

    private fun drawColorField(graphics: GuiGraphicsExtractor) {
        val partial = partialTicks

        // Saturation: white on the left, the current hue on the right
        val interpolatedHue = MathUtils.lerp(prevHue, hue, partial)
        val rightColor = ColorUtils.hsbToRGB(interpolatedHue, 1.0f, 1.0f, 1.0f)
        val leftColor = ColorRGB(255, 255, 255)

        val x0 = fieldRect.x0.roundToInt()
        val y0 = fieldRect.y0.roundToInt()
        val x1 = fieldRect.x1.roundToInt()
        val y1 = fieldRect.y1.roundToInt()
        val fieldW = x1 - x0
        val fieldH = y1 - y0

        // fillGradient interpolates along its local +y only, so the quad is emitted in a frame
        // rotated -90 degrees: local (lx, ly) maps to world (ly, -lx), which turns the gradient
        // axis into the field's +x. That keeps it a single hardware gradient like the original.
        graphics.pose().pushMatrix()
        graphics.pose().translate(x0.toFloat(), y0.toFloat())
        graphics.pose().rotate(-HALF_PI)
        graphics.fillGradient(-fieldH, 0, 0, fieldW, leftColor.toArgb(), rightColor.toArgb())
        graphics.pose().popMatrix()

        // Brightness: transparent on top, black at the bottom
        graphics.fillGradient(x0, y0, x1, y1, BRIGHTNESS_TOP, BRIGHTNESS_BOTTOM)

        // Circle pointer, greyscale tracking the contrast against the field
        val interpolatedSaturation = MathUtils.lerp(prevSaturation, saturation, partial)
        val interpolatedBrightness = MathUtils.lerp(prevBrightness, brightness, partial)
        val relativeBrightness = ((1.0f - (1.0f - interpolatedSaturation) * interpolatedBrightness) * 255.0f).toInt()
        val circleColor = ColorRGB(relativeBrightness, relativeBrightness, relativeBrightness)

        drawCircleOutline(
            graphics,
            fieldRect.x0 + fieldHeight * interpolatedSaturation,
            fieldRect.y0 + fieldHeight * (1.0f - interpolatedBrightness),
            4.0f,
            1.5f,
            circleColor.toArgb()
        )
    }

    private fun drawHueSlider(graphics: GuiGraphicsExtractor) {
        val x0 = hueRect.x0.roundToInt()
        val x1 = hueRect.x1.roundToInt()
        val top = hueRect.y0
        val partHeight = (hueRect.y1 - hueRect.y0) / 6.0f

        for (i in 0 until 6) {
            graphics.fillGradient(
                x0,
                (top + partHeight * i).roundToInt(),
                x1,
                (top + partHeight * (i + 1)).roundToInt(),
                HUE_STOPS[i],
                HUE_STOPS[i + 1]
            )
        }

        // Arrow pointers on both sides of the bar
        val interpolatedHue = MathUtils.lerp(prevHue, hue, partialTicks)
        val pointerPosY = hueRect.y0 + fieldHeight * interpolatedHue
        val pointerColor = GuiSetting.primary.toArgb()

        drawPointer(graphics, hueRect.x0 - 5.0f, hueRect.x0 - 1.0f, pointerPosY, 2.0f, 1.5f, pointerColor)
        drawPointer(graphics, hueRect.x1 + 5.0f, hueRect.x1 + 1.0f, pointerPosY, 2.0f, 1.5f, pointerColor)
    }

    private fun drawColorPreview(graphics: GuiGraphicsExtractor) {
        // Previous color
        val previous = setting.value.alpha(255)
        graphics.fill(
            prevColorRect.x0.roundToInt(),
            prevColorRect.y0.roundToInt(),
            prevColorRect.x1.roundToInt(),
            prevColorRect.y1.roundToInt(),
            previous.toArgb()
        )

        // Current color
        val current = ColorRGB(r.value, g.value, b.value)
        graphics.fill(
            currentColorRect.x0.roundToInt(),
            currentColorRect.y0.roundToInt(),
            currentColorRect.x1.roundToInt(),
            currentColorRect.y1.roundToInt(),
            current.toArgb()
        )
    }

    /** Ring of [thickness] centred on [radius], scanline-rasterised at 1/[SUB] unit precision. */
    private fun drawCircleOutline(
        graphics: GuiGraphicsExtractor,
        centerX: Float,
        centerY: Float,
        radius: Float,
        thickness: Float,
        argb: Int
    ) {
        val outer = (radius + thickness * 0.5f) * SUB
        val inner = (radius - thickness * 0.5f) * SUB
        val rows = ceil(outer).toInt()

        graphics.pose().pushMatrix()
        graphics.pose().translate(centerX, centerY)
        graphics.pose().scale(1.0f / SUB, 1.0f / SUB)

        for (y in -rows until rows) {
            val dy = y + 0.5f
            val outerSq = outer * outer - dy * dy
            if (outerSq <= 0.0f) continue
            val outerX = sqrt(outerSq)

            val innerSq = inner * inner - dy * dy
            if (innerSq > 0.0f) {
                val innerX = sqrt(innerSq)
                graphics.fill((-outerX).roundToInt(), y, (-innerX).roundToInt(), y + 1, argb)
                graphics.fill(innerX.roundToInt(), y, outerX.roundToInt(), y + 1, argb)
            } else {
                graphics.fill((-outerX).roundToInt(), y, outerX.roundToInt(), y + 1, argb)
            }
        }

        graphics.pose().popMatrix()
    }

    /**
     * Outline of [thickness] around the isosceles triangle whose vertical base is at [baseX]
     * and whose apex is at [tipX], scanline-rasterised at 1/[SUB] unit precision.
     *
     * The hole is the same triangle inset by half the thickness. Insetting a triangle by a
     * constant distance is exactly a scale about its incenter by `(r - t/2) / r`, so both the
     * outer and the inner span of every row are closed-form.
     */
    private fun drawPointer(
        graphics: GuiGraphicsExtractor,
        baseX: Float,
        tipX: Float,
        centerY: Float,
        halfHeight: Float,
        thickness: Float,
        argb: Int
    ) {
        val signedLength = (tipX - baseX) * SUB
        val length = abs(signedLength)
        val half = halfHeight * SUB
        if (length <= 0.0f || half <= 0.0f) return

        // Rasterise pointing towards +x and mirror on the way out, so one code path covers both
        // the left and the right pointer.
        val dir = if (signedLength < 0.0f) -1 else 1

        val leg = sqrt(length * length + half * half)
        val inRadius = half * length / (half + leg)
        val scale = ((inRadius - thickness * 0.5f * SUB) / inRadius).coerceAtLeast(0.0f)
        val innerBase = inRadius * (1.0f - scale)
        val innerTip = inRadius + scale * (length - inRadius)
        val innerHalf = scale * half

        val rows = ceil(half).toInt()

        graphics.pose().pushMatrix()
        graphics.pose().translate(baseX, centerY)
        graphics.pose().scale(1.0f / SUB, 1.0f / SUB)

        for (y in -rows until rows) {
            val dy = abs(y + 0.5f)
            if (dy >= half) continue
            val outerEnd = length * (1.0f - dy / half)

            if (innerHalf > 0.0f && dy < innerHalf) {
                val innerEnd = innerBase + (innerTip - innerBase) * (1.0f - dy / innerHalf)
                graphics.fill(0, y, dir * innerBase.roundToInt(), y + 1, argb)
                graphics.fill(dir * innerEnd.roundToInt(), y, dir * outerEnd.roundToInt(), y + 1, argb)
            } else {
                graphics.fill(0, y, dir * outerEnd.roundToInt(), y + 1, argb)
            }
        }

        graphics.pose().popMatrix()
    }

    // ---- Layout ----

    private fun updatePos() {
        // Red slider
        sliderR.posY = draggableHeight
        sliderR.width = 128.0f

        // Green slider
        sliderG.posY = sliderR.posY + sliderR.height + 2.0f
        sliderG.width = 128.0f

        // Blue slider
        sliderB.posY = sliderG.posY + sliderG.height + 2.0f
        sliderB.width = 128.0f

        // Alpha slider
        sliderA.posY = sliderB.posY + sliderB.height + 2.0f
        sliderA.width = 128.0f

        // Okay button
        buttonOkay.posY = sliderA.posY + sliderA.height + 2.0f
        buttonOkay.width = 50.0f

        // Cancel button
        buttonCancel.posY = buttonOkay.posY + buttonOkay.height + 2.0f
        buttonCancel.width = 50.0f

        // Apply button
        buttonApply.posY = buttonCancel.posY + buttonCancel.height + 2.0f
        buttonApply.width = 50.0f

        // Main window
        height = buttonApply.posY + buttonApply.height + 4.0f
        width = height - draggableHeight + 4.0f + 8.0f + 4.0f + 128.0f + 8.0f

        // PositionX of components
        val sliderX = width - 4.0f - 128.0f
        sliderR.posX = sliderX
        sliderG.posX = sliderX
        sliderB.posX = sliderX
        sliderA.posX = sliderX

        val buttonX = width - 4.0f - 50.0f
        buttonOkay.posX = buttonX
        buttonCancel.posX = buttonX
        buttonApply.posX = buttonX

        // Variables
        fieldHeight = height - draggableHeight - 4.0f
        fieldRect.set(
            4.0f,
            draggableHeight,
            4.0f + fieldHeight,
            draggableHeight + fieldHeight
        )
        hueRect.set(
            4.0f + fieldHeight + 6.0f,
            draggableHeight,
            4.0f + fieldHeight + 6.0f + 8.0f,
            draggableHeight + fieldHeight
        )
        prevColorRect.set(
            sliderX,
            buttonOkay.posY,
            sliderX + 35.0f,
            height - 4.0f
        )
        currentColorRect.set(
            sliderX + 35.0f + 4.0f,
            buttonOkay.posY,
            sliderX + 35.0f + 4.0f + 35.0f,
            height - 4.0f
        )
    }

    /**
     * The original docked this window CENTER/CENTER with a zero relative offset, which it
     * re-resolved against the current screen size. The size is read live rather than from the
     * constructor arguments because `Screen.init` re-runs [onDisplayed] on every resize, and a
     * snapshot taken when the picker was opened would centre it on the old resolution.
     */
    private fun centerOnScreen() {
        posX = max(0.0f, (Resolution.trollWidthF - width) * 0.5f)
        posY = max(0.0f, (Resolution.trollHeightF - height) * 0.5f)
    }

    // ---- Helpers ----

    private val partialTicks: Float
        get() = Wrapper.minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)

    private fun trollMouseX(): Float {
        val mc = Wrapper.minecraft
        val ratio = mc.window.guiScaledWidth.toFloat() / mc.window.screenWidth.toFloat()
        return Resolution.toTrollX((mc.mouseHandler.xpos() * ratio).toFloat())
    }

    private fun trollMouseY(): Float {
        val mc = Wrapper.minecraft
        val ratio = mc.window.guiScaledHeight.toFloat() / mc.window.screenHeight.toFloat()
        return Resolution.toTrollY((mc.mouseHandler.ypos() * ratio).toFloat())
    }

    private class Rect {
        var x0 = 0.0f; private set
        var y0 = 0.0f; private set
        var x1 = 0.0f; private set
        var y1 = 0.0f; private set

        fun set(x0: Float, y0: Float, x1: Float, y1: Float) {
            this.x0 = x0
            this.y0 = y0
            this.x1 = x1
            this.y1 = y1
        }

        fun contains(x: Float, y: Float) = x in x0..x1 && y in y0..y1
    }

    private companion object {
        /** Sub-unit resolution used by the scanline shape helpers. */
        const val SUB = 4.0f

        /** Quarter turn used to feed [GuiGraphicsExtractor.fillGradient] a horizontal ramp. */
        val HALF_PI = (PI / 2.0).toFloat()

        val BRIGHTNESS_TOP = ColorRGB(0, 0, 0, 0).toArgb()
        val BRIGHTNESS_BOTTOM = ColorRGB(0, 0, 0, 255).toArgb()

        /** 0.0, 0.1666, 0.3333, 0.5, 0.6666, 0.8333, 1.0 */
        val HUE_STOPS = intArrayOf(
            ColorRGB(255, 0, 0).toArgb(),
            ColorRGB(255, 255, 0).toArgb(),
            ColorRGB(0, 255, 0).toArgb(),
            ColorRGB(0, 255, 255).toArgb(),
            ColorRGB(0, 0, 255).toArgb(),
            ColorRGB(255, 0, 255).toArgb(),
            ColorRGB(255, 0, 0).toArgb()
        )
    }
}
