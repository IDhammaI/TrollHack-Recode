package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.gui.rgui.Component.UiSettingGroup
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class TitledWindow(
    name: String,
    uiSettingGroup: UiSettingGroup = UiSettingGroup.NONE
) : BasicWindow(name, uiSettingGroup) {

    override val draggableHeight: Float get() = TrollFont.getHeight() + 6.0f
    override val minimizable = true

    override fun onRender(graphics: GuiGraphicsExtractor) {
        super.onRender(graphics)
        TrollFont.drawString(graphics, componentName, 3.0f, 3.5f, GuiSetting.text, bold = true)
    }
}
