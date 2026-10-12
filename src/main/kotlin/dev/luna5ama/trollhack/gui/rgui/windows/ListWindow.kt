package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.rgui.Component.UiSettingGroup
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Scrollable vertical stack of child components with the original
 * exponential-decay scroll physics (quarter speed per 100ms + rubber band).
 */
open class ListWindow(
    name: String,
    uiSettingGroup: UiSettingGroup = UiSettingGroup.NONE,
    vararg childrenIn: InteractiveComponent
) : TitledWindow(name, uiSettingGroup) {

    val children = ArrayList<InteractiveComponent>()

    private var hoveredChild: InteractiveComponent? = null
        set(value) {
            if (value != field) {
                field?.onLeave()
                value?.onHover()
                field = value
            }
        }

    private var clickedChild: InteractiveComponent? = null

    private var scrollSpeed = 0.0f
    private var scrollProgress = 0.0f
    private var lastScrollUpdate = System.currentTimeMillis()
    private var lastRubberBand = System.currentTimeMillis()

    /** Bind-listening children swallow all keyboard input; the screen checks this. */
    open val keyboardListening: Boolean get() = false

    override val minWidth = 80.0f
    override val minHeight = 100.0f

    init {
        children.addAll(childrenIn)
    }

    open val optimalWidth: Float
        get() = max(
            children.maxOfOrNull { it.minWidth + GuiSetting.xMargin * 2.0f } ?: 80.0f,
            max(TrollFont.getWidth(componentName) + 20.0f, 80.0f)
        )

    open val optimalHeight: Float
        get() = children.filter { it.visible }.sumOf { (it.height + GuiSetting.yMargin).toDouble() }.toFloat() +
            draggableHeight + max(GuiSetting.xMargin, GuiSetting.yMargin)

    protected fun updateChildPosSize() {
        var y = draggableHeight + GuiSetting.yMargin
        for (child in children) {
            if (!child.visible) continue
            child.posX = GuiSetting.xMargin
            child.posY = y
            child.width = width - GuiSetting.xMargin * 2.0f
            y += child.height + GuiSetting.yMargin
        }
    }

    override fun onDisplayed() {
        super.onDisplayed()
        scrollSpeed = 0.0f
        scrollProgress = 0.0f
        updateChildPosSize()
        for (child in children) child.onDisplayed()
    }

    override fun onClosed() {
        super.onClosed()
        hoveredChild = null
        clickedChild = null
        for (child in children) child.onClosed()
    }

    override fun onTick() {
        super.onTick()
        updateChildPosSize()
        for (child in children) child.onTick()
    }

    private fun updateScrollProgress() {
        val now = System.currentTimeMillis()
        val x = (now - lastScrollUpdate) / 100.0
        lastScrollUpdate = now

        // Analytic exponential decay: quarter speed per 100ms
        val newSpeed = (scrollSpeed * 0.25.pow(x)).toFloat()
        val lnBase = ln(0.25).toFloat()
        scrollProgress += (newSpeed / lnBase) - (scrollSpeed / lnBase)
        scrollSpeed = newSpeed

        if (now - lastRubberBand >= 100L) {
            lastRubberBand = now
            val contentBottom = children.filter { it.visible }.maxOfOrNull { it.posY + it.height } ?: 0.0f
            val maxScroll = max(contentBottom + GuiSetting.yMargin - height, 0.01f)

            if (scrollProgress < 0.0f) {
                scrollSpeed = scrollProgress * -0.4f
            } else if (scrollProgress > maxScroll) {
                scrollSpeed = (scrollProgress - maxScroll) * -0.4f
            }
        }
    }

    override fun onScroll(amount: Double): Boolean {
        scrollSpeed -= (amount * 10.0f).toFloat()
        return true
    }

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)
        if (renderMinimizeProgress == 0.0f) return

        updateScrollProgress()

        // Window-local coords; the parent pose already translated to (renderPosX, renderPosY)
        // and enableScissor bakes the current pose in, so the slide/drag follow automatically.
        graphics.enableScissor(
            (GuiSetting.xMargin - 0.5f).toInt(),
            draggableHeight.toInt(),
            (renderWidth - GuiSetting.xMargin + 1.0f).roundToInt(),
            renderHeight.toInt()
        )

        graphics.pose().pushMatrix()
        graphics.pose().translate(0.0f, -scrollProgress)

        for (child in children) {
            if (!child.visible) continue
            if (child.posY + child.height - scrollProgress < draggableHeight) continue
            if (child.posY - scrollProgress > renderHeight) continue

            graphics.pose().pushMatrix()
            graphics.pose().translate(child.posX, child.posY)
            child.onRender(graphics)
            graphics.pose().popMatrix()
        }

        graphics.pose().popMatrix()
        graphics.disableScissor()
    }

    override fun onPostRender(graphics: GuiGraphicsExtractor) {
        super.onPostRender(graphics)
        if (renderMinimizeProgress == 0.0f) return

        graphics.pose().pushMatrix()
        graphics.pose().translate(0.0f, -scrollProgress)

        for (child in children) {
            if (!child.visible) continue

            graphics.pose().pushMatrix()
            graphics.pose().translate(child.posX, child.posY)
            child.onPostRender(graphics)
            graphics.pose().popMatrix()
        }

        graphics.pose().popMatrix()
    }

    /** Converts parent-space mouse to child-space (accounting for window pos and scroll). */
    private fun childAt(mouseX: Float, mouseY: Float): InteractiveComponent? {
        val localX = mouseX - posX
        val localY = mouseY - posY + scrollProgress

        if (mouseY - posY < draggableHeight) return null
        if (localX < GuiSetting.xMargin || localX > width - GuiSetting.xMargin) return null

        return children.lastOrNull { it.visible && it.isInside(localX, localY) }
    }

    override fun updateHovered(mouseX: Float, mouseY: Float) {
        hoveredChild = if (mouseState == MouseState.NONE || mouseState == MouseState.HOVER) {
            childAt(mouseX, mouseY)
        } else {
            hoveredChild
        }
    }

    override fun onClick(mouseX: Float, mouseY: Float, button: Int) {
        super.onClick(mouseX, mouseY, button)

        val child = childAt(mouseX, mouseY)
        clickedChild = child
        child?.onClick(mouseX - posX, mouseY - posY + scrollProgress, button)
    }

    override fun onRelease(mouseX: Float, mouseY: Float, button: Int) {
        clickedChild?.onRelease(mouseX - posX, mouseY - posY + scrollProgress, button)
        clickedChild = null
        super.onRelease(mouseX, mouseY, button)
    }

    override fun onDrag(mouseX: Float, mouseY: Float, clickX: Float, clickY: Float, button: Int) {
        val child = clickedChild
        if (child != null) {
            child.onDrag(
                mouseX - posX,
                mouseY - posY + scrollProgress,
                clickX - posX,
                clickY - posY + scrollProgress,
                button
            )
        } else {
            super.onDrag(mouseX, mouseY, clickX, clickY, button)
        }
    }

    override fun onKeyInput(key: Int, state: Boolean): Boolean {
        for (child in children) {
            if (child.onKeyInput(key, state)) return true
        }
        return false
    }

    override fun onCharTyped(char: Char): Boolean {
        for (child in children) {
            if (child.onCharTyped(char)) return true
        }
        return false
    }
}
