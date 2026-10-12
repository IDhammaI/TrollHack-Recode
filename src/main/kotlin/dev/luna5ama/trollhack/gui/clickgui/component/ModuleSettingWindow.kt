package dev.luna5ama.trollhack.gui.clickgui.component

import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.gui.rgui.component.BindButton
import dev.luna5ama.trollhack.gui.rgui.component.ColorButton
import dev.luna5ama.trollhack.gui.rgui.component.SettingComponentFactory
import dev.luna5ama.trollhack.gui.rgui.windows.FloatingSettingWindow
import dev.luna5ama.trollhack.module.AbstractModule

class ModuleSettingWindow(
    val module: AbstractModule,
    screenWidth: Float,
    screenHeight: Float,
    mouseX: Float,
    mouseY: Float,
    onOpenPicker: (ColorButton) -> Unit
) : FloatingSettingWindow(
    module.name.toString(),
    buildComponents(module, onOpenPicker),
    screenWidth,
    screenHeight,
    mouseX,
    mouseY
) {
    override val copyableSettingGroup get() = module.settingGroup

    private companion object {
        fun buildComponents(
            module: AbstractModule,
            onOpenPicker: (ColorButton) -> Unit
        ): List<InteractiveComponent> {
            val list = ArrayList<InteractiveComponent>()
            for (setting in module.settingList) {
                SettingComponentFactory.forSetting(setting, onOpenPicker)?.let { list.add(it) }
            }
            list.add(BindButton(module.bind))
            return list
        }
    }
}
