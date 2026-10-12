package dev.luna5ama.trollhack.gui.hudgui.component

import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.component.ColorButton
import dev.luna5ama.trollhack.gui.rgui.component.SettingComponentFactory
import dev.luna5ama.trollhack.gui.rgui.windows.FloatingSettingWindow

class HudSettingWindow(
    val element: AbstractHudElement,
    screenWidth: Float,
    screenHeight: Float,
    mouseX: Float,
    mouseY: Float,
    onOpenPicker: (ColorButton) -> Unit
) : FloatingSettingWindow(
    element.name.toString(),
    buildComponents(element, onOpenPicker),
    screenWidth,
    screenHeight,
    mouseX,
    mouseY
) {
    override val copyableSettingGroup get() = element.settingGroup

    private companion object {
        fun buildComponents(
            element: AbstractHudElement,
            onOpenPicker: (ColorButton) -> Unit
        ): List<InteractiveComponent> {
            val list = ArrayList<InteractiveComponent>()
            for (setting in element.settingList) {
                if (setting.isTransient) continue
                SettingComponentFactory.forSetting(setting, onOpenPicker)?.let { list.add(it) }
            }
            return list
        }
    }
}
