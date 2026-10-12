package dev.luna5ama.trollhack.gui.clickgui

import dev.luna5ama.trollhack.graphics.Resolution
import dev.luna5ama.trollhack.gui.WindowScreen
import dev.luna5ama.trollhack.gui.clickgui.component.CategoryWindow
import dev.luna5ama.trollhack.gui.clickgui.component.ModuleButton
import dev.luna5ama.trollhack.gui.clickgui.component.ModuleSettingWindow
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.Wrapper
import kotlin.math.max

/**
 * The Click GUI: one ListWindow per category, right-click a module to open a floating
 * setting window. Rebuilt to match the original TrollHack look and interactions.
 */
internal object TrollClickGui : WindowScreen("Click GUI") {

    private val categoryWindows = ArrayList<CategoryWindow>()
    private var settingWindow: ModuleSettingWindow? = null
    private var built = false

    /**
     * Builds the category windows. Called from GuiManager before the config is read so the
     * windows' persisted geometry settings exist by the time the config load runs.
     */
    internal fun build() {
        if (built) return
        built = true

        windows.clear()
        categoryWindows.clear()

        for (category in Category.entries) {
            val window = CategoryWindow(category, ::openSettings)
            categoryWindows.add(window)
            windows.add(window)
        }
    }

    override fun init() {
        build()
        layoutWindows()
        openAnimation()
    }

    /**
     * Places windows that have never been placed, using the original's grid: a fixed 80x400
     * window per category, butted together with no gap from the top left corner, wrapping to a
     * new row 100px down when the row runs past the screen edge.
     *
     * Windows the user has already arranged keep their saved geometry — the layout is not
     * re-applied on later opens or on a resize, which used to wipe the arrangement.
     */
    private fun layoutWindows() {
        val screenW = Resolution.trollWidthF
        val screenH = Resolution.trollHeightF
        var x = 0.0f
        var y = 0.0f

        for (window in categoryWindows) {
            if (!window.initialized) {
                window.posX = x
                window.posY = y
                window.width = 80.0f
                window.height = 400.0f.coerceAtMost(max(100.0f, screenH))
                window.initialized = true
            }

            x += 80.0f
            if (x > screenW) {
                x = 0.0f
                y += 100.0f
            }
        }

        // Keep everything on screen after a resolution change without disturbing the arrangement
        for (window in categoryWindows) {
            window.clampToScreen(screenW, screenH)
        }
    }

    private fun openSettings(button: ModuleButton) {
        closeTransientWindows()

        val screenW = Resolution.trollWidthF
        val screenH = Resolution.trollHeightF
        val (mx, my) = trollMousePos()

        val window = ModuleSettingWindow(button.module, screenW, screenH, mx, my, ::openColorPicker)
        settingWindow = window
        addTransientWindow(window)
    }

    override fun onSearchChanged(search: String) {
        for (window in categoryWindows) {
            window.applySearch(search)
        }
    }

    override fun onCloseKey() {
        ClickGUI.disable()
    }

    override fun closeBindKey() = ClickGUI.bind.value.key

    override fun onTransientWindowsClosed() {
        settingWindow = null
    }

    override fun removed() {
        settingWindow = null
        searchString = ""
        for (window in categoryWindows) window.applySearch("")
        if (ClickGUI.isEnabled) ClickGUI.disable()
        super.removed()
    }
}
