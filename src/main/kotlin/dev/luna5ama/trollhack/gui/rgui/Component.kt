package dev.luna5ama.trollhack.gui.rgui

import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.interfaces.Nameable

/**
 * Skeleton of the rgui component system.
 * The full windowed GUI is ported in the gui phase; config plumbing lives here
 * so GuiConfig can compile early.
 */
open class Component(
    final override val name: CharSequence,
    val uiSettingGroup: UiSettingGroup,
    val config: AbstractConfig<out Nameable> = GuiConfig
) : Nameable {
    override val internalName = rootName.replace(" ", "")

    val settingGroup get() = config.getGroupOrPut(uiSettingGroup.groupName).getGroupOrPut(internalName)

    enum class UiSettingGroup(val groupName: String) {
        NONE(""),
        CLICK_GUI("click_gui"),
        HUD_GUI("hud_gui")
    }
}
