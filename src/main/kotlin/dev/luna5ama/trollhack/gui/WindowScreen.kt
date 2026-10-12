package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.ParticleBackground
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import org.lwjgl.glfw.GLFW

/**
 * Base window-managing screen: z-ordered draggable windows, click/drag/scroll dispatch,
 * open/close fade + slide-down animation, and a live search string overlay.
 */
abstract class WindowScreen(title: String) : AbstractTrollGui(title) {

    protected val windows = ArrayList<WindowComponent>()

    private var draggingWindow: WindowComponent? = null
    private var clickX = 0.0f
    private var clickY = 0.0f
    private var clickButton = -1
    private var dragging = false

    private var displayTime = System.currentTimeMillis()
    private var closing = false

    private var colorPicker: dev.luna5ama.trollhack.gui.rgui.windows.ColorPicker? = null

    protected var searchString = ""
        set(value) {
            field = value
            searchTime = System.currentTimeMillis()
            onSearchChanged(value)
        }
    private var searchTime = 0L

    protected open fun onSearchChanged(search: String) {}

    // ---- Lifecycle ----

    override fun isPauseScreen(): Boolean = false
    override fun shouldCloseOnEsc(): Boolean = false

    fun addWindow(window: WindowComponent) {
        windows.add(window)
        window.onDisplayed()
    }

    /**
     * Windows that only make sense while their owner is being pointed at: the module/element
     * setting panel and the colour picker.
     *
     * The original dismissed these the moment the user clicked anywhere else, and dropped them
     * when the GUI closed. Without that, the panel has no way to be dismissed at all and one
     * ghost panel is left behind per open/close cycle, since these screens are singletons.
     */
    private val transientWindows = ArrayList<WindowComponent>()

    fun addTransientWindow(window: WindowComponent) {
        transientWindows.add(window)
        addWindow(window)
    }

    fun closeTransientWindows() {
        if (transientWindows.isEmpty()) return
        for (window in transientWindows) {
            window.onClosed()
            windows.remove(window)
        }
        transientWindows.clear()
        colorPicker = null
        onTransientWindowsClosed()
    }

    protected open fun onTransientWindowsClosed() {}

    /** Opens a colour picker beside the cursor, replacing any previous one. */
    protected fun openColorPicker(button: dev.luna5ama.trollhack.gui.rgui.component.ColorButton) {
        colorPicker?.let {
            windows.remove(it)
            transientWindows.remove(it)
        }

        val screenW = Resolution.trollWidthF
        val screenH = Resolution.trollHeightF
        val (mx, my) = trollMousePos()

        val picker = dev.luna5ama.trollhack.gui.rgui.windows.ColorPicker(
            button.setting, screenW, screenH, mx, my
        )
        colorPicker = picker
        addTransientWindow(picker)
    }

    /** Current cursor position in troll units. */
    protected fun trollMousePos(): Pair<Float, Float> {
        val mc = dev.luna5ama.trollhack.util.Wrapper.minecraft
        val sx = mc.window.guiScaledWidth.toFloat() / mc.window.screenWidth.toFloat()
        val sy = mc.window.guiScaledHeight.toFloat() / mc.window.screenHeight.toFloat()
        return Resolution.toTrollX((mc.mouseHandler.xpos() * sx).toFloat()) to
            Resolution.toTrollY((mc.mouseHandler.ypos() * sy).toFloat())
    }

    private fun pruneClosedPicker() {
        colorPicker?.let {
            if (it.closeRequested) {
                windows.remove(it)
                transientWindows.remove(it)
                colorPicker = null
            }
        }
    }

    protected fun openAnimation() {
        displayTime = System.currentTimeMillis()
        closing = false
        for (window in windows) window.onDisplayed()
    }

    override fun removed() {
        closing = false
        closeTransientWindows()
        for (window in windows) window.onClosed()
        super.removed()
    }

    override fun tick() {
        // The close animation runs while the screen is still the active one; once the fade has
        // played out the screen actually goes away. 26.2 stops rendering a Screen the instant it
        // is replaced, so the original's "keep drawing after close" trick is not available.
        if (closing && System.currentTimeMillis() - displayTime >= GuiSetting.fadeOutTime * 1000.0f) {
            closing = false
            onCloseKey()
            return
        }

        pruneClosedPicker()
        for (window in windows) window.onTick()
    }

    /** Starts the close animation, or closes immediately when the user set no fade out time. */
    protected fun requestClose() {
        if (closing) return
        if (GuiSetting.fadeOutTime <= 0.0f) {
            onCloseKey()
        } else {
            beginClose()
        }
    }

    private fun fadeMultiplier(): Float {
        return if (closing) {
            Easing.OUT_CUBIC.incOrDec(Easing.toDelta(displayTime, GuiSetting.fadeOutTime * 1000.0f), 1.0f, 0.0f)
        } else if (GuiSetting.fadeInTime <= 0.0f) {
            1.0f
        } else {
            Easing.OUT_CUBIC.incOrDec(Easing.toDelta(displayTime, GuiSetting.fadeInTime * 1000.0f), 0.0f, 1.0f)
        }
    }

    // ---- Rendering ----

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        val fade = fadeMultiplier()

        // Partial: 26.2 only offers an on/off blur of everything drawn before this stratum, at
        // vanilla's own radius. The original ran a kawase pass whose strength tracked the setting
        // and the fade, which needs a custom pipeline to reproduce exactly.
        if (GuiSetting.backGroundBlur > 0.0f) {
            graphics.blurBeforeThisStratum()
        }

        if (GuiSetting.darkness > 0.0f) {
            val alpha = (GuiSetting.darkness * 255.0f * fade).toInt()
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha shl 24))
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        updateHover(Resolution.toTrollX(mouseX.toFloat()), Resolution.toTrollY(mouseY.toFloat()))

        val fade = fadeMultiplier()
        val slide = Resolution.trollHeightF * (1.0f - fade)

        // Everything below is drawn in troll units; this maps them onto the gui-scaled surface
        // so the GUI keeps its own size regardless of Minecraft's Gui Scale option.
        val k = Resolution.guiScaleRatio
        graphics.pose().pushMatrix()
        graphics.pose().scale(k, k)

        // Behind the windows and not affected by the slide, like the original
        if (GuiSetting.particle) {
            ParticleBackground.render(
                graphics,
                Resolution.trollWidthF,
                Resolution.trollHeightF,
                Resolution.toTrollX(mouseX.toFloat()),
                Resolution.toTrollY(mouseY.toFloat()),
                fade
            )
        }

        graphics.pose().translate(0.0f, -slide)

        // First pass: all window bodies
        for (window in windows) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(window.renderPosX, window.renderPosY)
            window.onRender(graphics)
            graphics.pose().popMatrix()
        }

        // Second pass: overlays (tooltips)
        for (window in windows) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(window.renderPosX, window.renderPosY)
            window.onPostRender(graphics)
            graphics.pose().popMatrix()
        }

        drawSearchString(graphics)
        graphics.pose().popMatrix()
    }

    private fun drawSearchString(graphics: GuiGraphicsExtractor) {
        if (searchString.isBlank()) return
        if (System.currentTimeMillis() - searchTime > 5000L) return

        val alpha = Easing.IN_CUBIC.incOrDec(Easing.toDelta(searchTime, 5000.0f), 255.0f, 0.0f).toInt()
        val textW = TrollFont.getWidth(searchString, 2.0f)
        val x = Resolution.trollWidthF / 2.0f - textW / 2.0f
        val y = Resolution.trollHeightF / 2.0f - TrollFont.getHeight(2.0f) / 2.0f
        TrollFont.drawString(graphics, searchString, x, y, GuiSetting.text.alpha(alpha), 2.0f)
    }

    // ---- Hover ----

    private fun updateHover(mouseX: Float, mouseY: Float) {
        if (dragging) return
        // Topmost window under cursor
        var top: WindowComponent? = null
        for (window in windows) {
            if (isInsideWindow(window, mouseX, mouseY)) top = window
        }
        for (window in windows) {
            if (window === top) window.onHover() else window.onLeave()
            if (window === top) window.updateHovered(mouseX, mouseY) else window.updateHovered(-1e6f, -1e6f)
        }
    }

    private fun isInsideWindow(window: WindowComponent, mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= window.renderPosX && mouseX <= window.renderPosX + window.renderWidth
            && mouseY >= window.renderPosY && mouseY <= window.renderPosY + window.renderHeight
    }

    // ---- Input ----

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val mx = Resolution.toTrollX(event.x().toFloat())
        val my = Resolution.toTrollY(event.y().toFloat())
        clickX = mx
        clickY = my
        clickButton = event.button()
        dragging = false

        val hit = windows.lastOrNull { isInsideWindow(it, mx, my) }

        // Clicking anything that is not one of them dismisses the transient windows
        if (hit == null || hit !in transientWindows) {
            closeTransientWindows()
        }

        if (hit != null) {
            // Move to top of z-order
            windows.remove(hit)
            windows.add(hit)
            draggingWindow = hit
            hit.onClick(mx - hit.renderPosX + hit.posX, my - hit.renderPosY + hit.posY, event.button())
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val window = draggingWindow ?: return super.mouseDragged(event, dx, dy)
        val mx = Resolution.toTrollX(event.x().toFloat())
        val my = Resolution.toTrollY(event.y().toFloat())

        if (!dragging && (kotlin.math.abs(mx - clickX) >= 2.0f || kotlin.math.abs(my - clickY) >= 2.0f)) {
            dragging = true
        }
        if (dragging) {
            window.onDrag(
                mx - window.renderPosX + window.posX,
                my - window.renderPosY + window.posY,
                clickX - window.renderPosX + window.posX,
                clickY - window.renderPosY + window.posY,
                clickButton
            )
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        val window = draggingWindow
        if (window != null) {
            window.onRelease(
                Resolution.toTrollX(event.x().toFloat()) - window.renderPosX + window.posX,
                Resolution.toTrollY(event.y().toFloat()) - window.renderPosY + window.posY,
                event.button()
            )
            draggingWindow = null
            dragging = false
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        val tx = Resolution.toTrollX(x.toFloat())
        val ty = Resolution.toTrollY(y.toFloat())
        for (window in windows.asReversed()) {
            if (isInsideWindow(window, tx, ty)) {
                if (window.onScroll(scrollY)) return true
            }
        }
        return super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        // Route to a keyboard-listening window (bind/text capture) first
        for (window in windows.asReversed()) {
            if (window.onKeyInput(event.key(), true)) return true
        }

        when (event.key()) {
            GLFW.GLFW_KEY_ESCAPE -> {
                requestClose()
                return true
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (searchString.isNotEmpty()) {
                    searchString = searchString.dropLast(1)
                    return true
                }
            }
        }

        if (closeBindKey() == event.key() && searchString.isEmpty()) {
            requestClose()
            return true
        }

        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        for (window in windows.asReversed()) {
            if (window.onKeyInput(event.key(), false)) return true
        }
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val char = event.codepointAsString().firstOrNull() ?: return false

        for (window in windows.asReversed()) {
            if (window.onCharTyped(char)) return true
        }

        if (char.isLetterOrDigit() || char == ' ') {
            searchString += char
            return true
        }
        return super.charTyped(event)
    }

    protected abstract fun onCloseKey()
    protected open fun closeBindKey(): Int = GLFW.GLFW_KEY_UNKNOWN
    protected fun beginClose() {
        closing = true
        displayTime = System.currentTimeMillis()
    }
}
