package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.gui.GuiManager
import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.gui.WindowScreen
import dev.luna5ama.trollhack.gui.hudgui.component.HudButton
import dev.luna5ama.trollhack.gui.hudgui.component.HudSettingWindow
import dev.luna5ama.trollhack.gui.rgui.windows.ListWindow
import dev.luna5ama.trollhack.module.modules.client.HudEditor
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent

/**
 * HUD editor: rgui category windows toggle elements; visible HUD elements render
 * with frames and can be dragged directly to reposition (dock-relative persistence).
 */
internal object TrollHudGui : WindowScreen("Hud Editor") {

    private class HudCategoryWindow(
        category: AbstractHudElement.Category,
        onOpenSettings: (HudButton) -> Unit
    ) : ListWindow(category.displayName.toString()) {
        init {
            for (element in GuiManager.hudElements) {
                if (element.category != category) continue
                children.add(HudButton(element, onOpenSettings))
            }
        }

        fun updateSize(maxHeight: Float) {
            for (child in children) {
                child.height = child.minHeight
            }
            updateChildPosSize()
            width = optimalWidth
            height = optimalHeight.coerceAtMost(maxHeight)
        }
    }

    private var settingWindow: HudSettingWindow? = null
    private var built = false

    private val dockingOverlay = DockingOverlay()
    private var dockingElement: AbstractHudElement? = null

    private var draggingElement: AbstractHudElement? = null
    private var dragOffsetX = 0.0f
    private var dragOffsetY = 0.0f

    private val categoryWindows = ArrayList<HudCategoryWindow>()

    override fun init() {
        if (!built) {
            windows.clear()
            categoryWindows.clear()

            for (category in AbstractHudElement.Category.entries) {
                val window = HudCategoryWindow(category, ::openSettings)
                categoryWindows.add(window)
                windows.add(window)
            }
            built = true
        }

        layoutWindows()
        openAnimation()
    }

    private fun layoutWindows() {
        val maxHeight = height - 12.0f
        var x = 6.0f
        var rowY = 6.0f
        var rowMaxHeight = 0.0f

        for (window in categoryWindows) {
            window.updateSize(maxHeight)

            if (x + window.width > width - 6.0f && x > 6.0f) {
                x = 6.0f
                rowY += rowMaxHeight + 6.0f
                rowMaxHeight = 0.0f
            }

            window.posX = x
            window.posY = rowY
            x += window.width + 6.0f
            rowMaxHeight = kotlin.math.max(rowMaxHeight, window.height)
        }
    }

    private fun openSettings(button: HudButton) {
        closeTransientWindows()

        val screenW = Resolution.trollWidthF
        val screenH = Resolution.trollHeightF
        val (mx, my) = trollMousePos()

        val window = HudSettingWindow(button.element, screenW, screenH, mx, my, ::openColorPicker)
        settingWindow = window
        addTransientWindow(window)
    }

    override fun onTransientWindowsClosed() {
        settingWindow = null
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        // HUD elements underneath, framed for placement
        for (element in GuiManager.hudElements) {
            if (!element.visible) continue

            element.renderFrame(graphics)

            graphics.pose().pushMatrix()
            graphics.pose().translate(element.renderPosX, element.renderPosY)
            graphics.pose().scale(element.scale, element.scale)
            element.renderHud(graphics)
            graphics.pose().popMatrix()
        }

        val mc = Wrapper.minecraft
        dockingOverlay.render(
            graphics,
            mc.window.guiScaledWidth.toFloat(),
            mc.window.guiScaledHeight.toFloat()
        )

        graphics.nextStratum()
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true

        // Click-through to HUD elements: left drag moves, right drag picks a docking zone
        for (element in GuiManager.hudElements) {
            if (!element.visible) continue
            if (element.isInside(event.x(), event.y())) {
                if (event.button() == 1) {
                    dockingElement = element
                    dockingOverlay.show()
                } else {
                    draggingElement = element
                    dragOffsetX = (event.x() - element.renderPosX).toFloat()
                    dragOffsetY = (event.y() - element.renderPosY).toFloat()
                }
                return true
            }
        }
        return false
    }

    override fun mouseDragged(event: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        draggingElement?.let {
            it.renderPosX = (event.x() - dragOffsetX).toFloat()
            it.renderPosY = (event.y() - dragOffsetY).toFloat()
            return true
        }
        return super.mouseDragged(event, dx, dy)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        dockingElement?.let { element ->
            val mc = Wrapper.minecraft
            dockingOverlay.resolve(
                event.x().toFloat(),
                event.y().toFloat(),
                mc.window.guiScaledWidth.toFloat(),
                mc.window.guiScaledHeight.toFloat()
            )?.let { (h, v) ->
                // Keep the element where it visually is while its anchor changes
                val screenX = element.renderPosX
                val screenY = element.renderPosY
                element.dockingH = h
                element.dockingV = v
                element.renderPosX = screenX
                element.renderPosY = screenY
            }

            dockingElement = null
            dockingOverlay.beginHide()
            return true
        }

        if (draggingElement != null) {
            draggingElement = null
            return true
        }
        return super.mouseReleased(event)
    }

    override fun tick() {
        super.tick()
        dockingOverlay.tick()
    }

    override fun onCloseKey() {
        HudEditor.disable()
    }

    override fun closeBindKey() = HudEditor.bind.value.key

    override fun removed() {
        settingWindow = null
        draggingElement = null
        if (HudEditor.isEnabled) HudEditor.disable()
        super.removed()
    }
}
