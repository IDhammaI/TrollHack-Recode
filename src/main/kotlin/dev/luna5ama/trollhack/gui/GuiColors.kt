package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.module.modules.client.GuiSetting

/**
 * Shared gui colors, forwarded live from the GuiSetting module.
 *
 * These are getters rather than values on purpose: the HUD and the ClickGUI must react the
 * moment the user drags a colour setting, and they must not drift into two different palettes.
 * The original had every HUD element read GuiSetting directly; forwarding here keeps that
 * behaviour while leaving the existing GuiColors.* call sites unchanged.
 */
object GuiColors {
    val text: ColorRGB get() = GuiSetting.text
    val primary: ColorRGB get() = GuiSetting.primary
    val backGround: ColorRGB get() = GuiSetting.backGround

    /** The original drew every window/element outline with the primary colour at full alpha. */
    val outline: ColorRGB get() = GuiSetting.primary.alpha(255)
}
