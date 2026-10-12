package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.rgui.Component.UiSettingGroup
import dev.luna5ama.trollhack.gui.rgui.WindowComponent
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

/**
 * Window with the original background/outline paint order.
 * Per-window backdrop blur is not available on the 26.2 pipeline yet.
 */
abstract class BasicWindow(
    name: String,
    uiSettingGroup: UiSettingGroup = UiSettingGroup.NONE
) : WindowComponent(name, uiSettingGroup) {

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)

        val w = renderWidth.roundToInt()
        val h = renderHeight.roundToInt()
        val barH = draggableHeight.roundToInt()

        if (GuiSetting.titleBar) {
            graphics.fill(0, barH, w, h, GuiSetting.backGround.toArgb())
            graphics.fill(0, 0, w, barH, GuiSetting.primary.toArgb())
        } else {
            graphics.fill(0, 0, w, h, GuiSetting.backGround.toArgb())
        }

        if (GuiSetting.windowOutline) {
            graphics.outline(0, 0, w, h, GuiSetting.primary.alpha(255).toArgb())
        }
    }
}
