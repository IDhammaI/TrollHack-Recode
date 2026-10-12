package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Interactive building block of the window system, rebuilt on the 26.2 gui stack.
 * Mouse coordinates passed in are window-local unless stated otherwise.
 */
abstract class InteractiveComponent(
    val componentName: String,
    uiSettingGroup: UiSettingGroup = UiSettingGroup.NONE
) : Component(componentName, uiSettingGroup) {

    enum class MouseState {
        NONE, HOVER, CLICK, DRAG
    }

    var mouseState = MouseState.NONE
        private set
    var prevState = MouseState.NONE
        private set
    var lastStateUpdateTime: Long = System.currentTimeMillis()
        private set

    // Layout (parent-relative, gui-scaled pixels).
    // Open so WindowComponent can back them with persisted settings.
    open var posX = 0.0f
    open var posY = 0.0f
    open var width = 20.0f
    open var height = 10.0f

    /**
     * When set, [visible] is answered by this predicate instead of the stored flag.
     *
     * Setting rows use it to mirror their setting's visibility condition, so conditional rows
     * appear and disappear live as the user toggles the setting they depend on.
     */
    var visibility: (() -> Boolean)? = null

    open var visible = true
        get() = visibility?.invoke() ?: field

    protected val renderWidthFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    protected val renderHeightFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)

    open val renderWidth: Float get() = renderWidthFlag.getAndUpdate(width)
    open val renderHeight: Float get() = renderHeightFlag.getAndUpdate(height)

    open val minWidth = 20.0f
    open val minHeight = 10.0f

    protected fun setMouseState(state: MouseState) {
        if (state != mouseState) {
            prevState = mouseState
            mouseState = state
            lastStateUpdateTime = System.currentTimeMillis()
        }
    }

    /** Hover overlay color, mixing between the previous and current state over 300ms OUT_EXPO. */
    protected fun stateOverlayColor(): ColorRGB {
        val mix = Easing.OUT_EXPO.inc(Easing.toDelta(lastStateUpdateTime, 300.0f))
        return stateColor(prevState).mix(stateColor(mouseState), mix)
    }

    private fun stateColor(state: MouseState) = when (state) {
        MouseState.NONE -> GuiSetting.idle
        MouseState.HOVER -> GuiSetting.hover
        MouseState.CLICK, MouseState.DRAG -> GuiSetting.click
    }

    fun isInside(mouseX: Float, mouseY: Float): Boolean {
        return mouseX >= posX && mouseX <= posX + width
            && mouseY >= posY && mouseY <= posY + height
    }

    open fun onDisplayed() {
        renderWidthFlag.forceUpdate(width)
        renderHeightFlag.forceUpdate(height)
        setMouseState(MouseState.NONE)
    }

    open fun onClosed() {
        setMouseState(MouseState.NONE)
    }

    open fun onTick() {}

    /** Draw in window-local space; the parent already translated to (renderPosX, renderPosY). */
    open fun onRender(graphics: GuiGraphicsExtractor) {}

    /** Second pass for overlays (tooltips), drawn after all onRender calls. */
    open fun onPostRender(graphics: GuiGraphicsExtractor) {}

    open fun onHover() {
        setMouseState(MouseState.HOVER)
    }

    open fun onLeave() {
        setMouseState(MouseState.NONE)
    }

    open fun onClick(mouseX: Float, mouseY: Float, button: Int) {
        setMouseState(MouseState.CLICK)
    }

    open fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        setMouseState(if (isInside(mouseX, mouseY)) MouseState.HOVER else MouseState.NONE)
    }

    open fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        setMouseState(MouseState.DRAG)
    }

    open fun onKeyInput(key: Int, state: Boolean): Boolean = false

    open fun onCharTyped(char: Char): Boolean = false

    open fun onScroll(amount: Double): Boolean = false
}
