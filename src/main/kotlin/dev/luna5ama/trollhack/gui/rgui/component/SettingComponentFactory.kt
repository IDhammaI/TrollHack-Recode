package dev.luna5ama.trollhack.gui.rgui.component

import dev.luna5ama.trollhack.gui.rgui.InteractiveComponent
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.NumberSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.ColorSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.StringSetting

/**
 * Maps settings to editor row components; shared by the module and HUD setting windows.
 */
object SettingComponentFactory {
    fun forSetting(
        setting: AbstractSetting<*>,
        onOpenPicker: (ColorButton) -> Unit = {}
    ): InteractiveComponent? {
        val component = when (setting) {
            is BooleanSetting -> SettingButton(setting)
            is NumberSetting<*> -> SettingSlider(setting)
            is EnumSetting<*> -> EnumSlider(setting)
            is StringSetting -> StringButton(setting)
            is BindSetting -> BindButton(setting)
            is ColorSetting -> ColorButton(setting, onOpenPicker)
            else -> null
        }

        // Conditional settings (atValue/atTrue/...) hide their row until the condition holds
        component?.visibility = { setting.isVisible }
        return component
    }
}
