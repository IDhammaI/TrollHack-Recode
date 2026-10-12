package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.graphics.AnimationFlag
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.graphics.Resolution
import kotlin.math.max

/**
 * Draggable/minimizable window. Minimize animates the render height between the
 * full height and the title bar over 300ms OUT_QUART, matching the original.
 */
abstract class WindowComponent(
    name: String,
    uiSettingGroup: UiSettingGroup = UiSettingGroup.NONE
) : InteractiveComponent(name, uiSettingGroup) {

    open val draggableHeight: Float get() = height
    open val minimizable = false

    /**
     * Geometry is persisted so a window arrangement survives closing the GUI and restarting.
     *
     * Windows registered under [UiSettingGroup.NONE] (transient ones like the setting panel and
     * the colour picker) still get these settings, but GuiConfig drops them, so they behave as
     * plain fields. This mirrors the original, where every Component carried the same settings.
     */
    private val posXSetting = setting("Pos X", 0.0f, -69420.911f..69420.911f, 0.1f, { false })
    private val posYSetting = setting("Pos Y", 0.0f, -69420.911f..69420.911f, 0.1f, { false })
    private val widthSetting = setting("Width", 80.0f, 0.0f..69420.911f, 0.1f, { false })
    private val heightSetting = setting("Height", 400.0f, 0.0f..69420.911f, 0.1f, { false })
    private val minimizedSetting = setting("Minimized", false, { false })

    /**
     * False until the window has been given its default place once. Persisted, so the default
     * layout runs on a fresh install and never again — the user's arrangement is what survives.
     */
    private val initializedSetting = setting("Initialized", false, { false })

    var initialized: Boolean
        get() = initializedSetting.value
        set(value) {
            initializedSetting.value = value
        }

    final override var posX: Float
        get() = posXSetting.value
        set(value) {
            posXSetting.value = value
        }

    final override var posY: Float
        get() = posYSetting.value
        set(value) {
            posYSetting.value = value
        }

    final override var width: Float
        get() = widthSetting.value
        set(value) {
            widthSetting.value = value
        }

    final override var height: Float
        get() = heightSetting.value
        set(value) {
            heightSetting.value = value
        }

    private val renderPosXFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    private val renderPosYFlag = AnimationFlag(Easing.OUT_CUBIC, 200.0f)
    private val minimizeFlag = AnimationFlag(Easing.OUT_QUART, 300.0f)

    val renderPosX: Float get() = renderPosXFlag.getAndUpdate(posX)
    val renderPosY: Float get() = renderPosYFlag.getAndUpdate(posY)

    var minimized: Boolean
        get() = minimizedSetting.value
        set(value) {
            if (value != minimizedSetting.value && System.currentTimeMillis() - minimizeFlag.time > 300L) {
                minimizedSetting.value = value
                minimizeFlag.update(if (value) 0.0f else 1.0f)
            }
        }

    protected val renderMinimizeProgress: Float get() = minimizeFlag.get()

    override val renderHeight: Float
        get() = (renderHeightFlag.getAndUpdate(height) - draggableHeight) * renderMinimizeProgress + draggableHeight

    private var preDragX = 0.0f
    private var preDragY = 0.0f

    /**
     * Positional hover, delivered every frame by WindowScreen in window-external (troll) space.
     * Windows with children override this to track which child is hovered; (-1e6, -1e6) means
     * the cursor is over another window.
     */
    open fun updateHovered(mouseX: Float, mouseY: Float) {}

    /** Pulls the window back on screen after a resolution change, keeping its size. */
    fun clampToScreen(screenWidth: Float, screenHeight: Float) {
        width = width.coerceIn(minWidth, max(minWidth, screenWidth))
        height = height.coerceIn(minHeight, max(minHeight, screenHeight))
        posX = posX.coerceIn(0.0f, max(0.0f, screenWidth - width))
        posY = posY.coerceIn(0.0f, max(0.0f, screenHeight - draggableHeight))
    }

    override fun onDisplayed() {
        super.onDisplayed()
        renderPosXFlag.forceUpdate(posX)
        renderPosYFlag.forceUpdate(posY)

        // Replay the open animation: expand from the title bar over 300ms
        if (minimizable && !minimized) {
            minimizeFlag.forceUpdate(0.0f, 0.0f)
            minimizeFlag.update(1.0f)
        } else {
            minimizeFlag.forceUpdate(if (minimized) 0.0f else 1.0f)
        }
    }

    override fun onClick(mouseX: Float, mouseY: Float, button: Int) {
        super.onClick(mouseX, mouseY, button)
        preDragX = posX
        preDragY = posY
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        // Right-click on the title bar toggles minimize (not after a drag)
        if (button == 1 && minimizable && prevState != MouseState.DRAG && mouseY - posY < draggableHeight) {
            minimized = !minimized
        }
        super.onRelease(mouseX, mouseY, button)
    }

    override fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        super.onDrag(mouseX, mouseY, clickX, clickY, button)

        if (button == 0 && clickY - preDragY < draggableHeight) {
            val screenWidth = Resolution.trollWidthF
            val screenHeight = Resolution.trollHeightF

            posX = (preDragX + (mouseX - clickX)).coerceIn(0.0f, screenWidth - width)
            posY = (preDragY + (mouseY - clickY)).coerceIn(0.0f, screenHeight - draggableHeight)
        }
    }
}
