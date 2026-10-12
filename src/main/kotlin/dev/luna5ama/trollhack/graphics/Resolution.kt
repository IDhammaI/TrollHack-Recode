package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.Wrapper

/**
 * The TrollHack GUI's own coordinate space.
 *
 * The client lays its GUI out in "troll" units, which are the raw framebuffer divided by
 * [GuiSetting.scaleFactor] rather than by Minecraft's Gui Scale. That is what keeps the GUI the
 * same physical size no matter what the player sets vanilla's Gui Scale to, and it is why the
 * Scale setting can go 50%-400% independently.
 *
 * [guiScaleRatio] converts troll units into the gui-scaled units the 26.2 draw calls expect, so
 * a screen renders by scaling the pose by it and then working entirely in troll units.
 */
object Resolution {
    private val window get() = Wrapper.minecraft.window

    val widthF: Float get() = window.width.toFloat()
    val heightF: Float get() = window.height.toFloat()

    val trollWidthF: Float get() = widthF / GuiSetting.scaleFactor
    val trollHeightF: Float get() = heightF / GuiSetting.scaleFactor

    /** troll units -> gui-scaled units. 1.0 when vanilla Gui Scale happens to match. */
    val guiScaleRatio: Float
        get() = GuiSetting.scaleFactor / window.guiScale.coerceAtLeast(1).toFloat()

    /** Converts a gui-scaled mouse coordinate into troll space. */
    fun toTrollX(guiScaledX: Float): Float = guiScaledX / guiScaleRatio

    fun toTrollY(guiScaledY: Float): Float = guiScaledY / guiScaleRatio
}
