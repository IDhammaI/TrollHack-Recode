package dev.luna5ama.trollhack.module.modules.client

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import kotlin.math.round

internal object GuiSetting : Module(
    name = "Gui Setting",
    description = "GUI theming",
    category = Category.CLIENT,
    visible = false,
    alwaysEnabled = true
) {
    private val scaleSetting = setting("Scale", 100, 50..400, 5)
    val particle by setting("Particle", false)
    val backGroundBlur by setting("Background Blur", 0.0f, 0.0f..1.0f, 0.05f)
    val windowOutline by setting("Window Outline", true)
    val titleBar by setting("Title Bar", false)
    val xMargin by setting("X Margin", 4.0f, 0.0f..10.0f, 0.5f)
    val yMargin by setting("Y Margin", 1.0f, 0.0f..10.0f, 0.5f)
    val darkness by setting("Darkness", 0.25f, 0.0f..1.0f, 0.05f)
    val fadeInTime by setting("Fade In Time", 0.4f, 0.0f..1.0f, 0.05f)
    val fadeOutTime by setting("Fade Out Time", 0.4f, 0.0f..1.0f, 0.05f)

    private val primarySetting = setting("Primary Color", ColorRGB(255, 140, 180, 220))
    private val backgroundSetting = setting("Background Color", ColorRGB(40, 32, 36, 160))
    private val textSetting = setting("Text Color", ColorRGB(255, 250, 253, 255))
    private val aHover = setting("Hover Alpha", 32, 0..255, 1)

    val primary: ColorRGB get() = primarySetting.value
    val backGround: ColorRGB get() = backgroundSetting.value
    val text: ColorRGB get() = textSetting.value

    val idle: ColorRGB
        get() = if (primary.lightness < 0.9f) ColorRGB(255, 255, 255, 0) else ColorRGB(0, 0, 0, 0)

    val hover: ColorRGB get() = idle.alpha(aHover.value)
    val click: ColorRGB get() = idle.alpha(aHover.value * 2)

    // ---- Gui scale ----

    private var prevScale = scaleSetting.value / 100.0f
    private var scale = prevScale
    private val settingTimer = TickTimer()

    /**
     * The GUI's own scale, independent of Minecraft's Gui Scale option.
     *
     * The base of 2.0 is the original's: at Scale = 100 the TrollHack GUI is laid out over half
     * the raw framebuffer, whatever the vanilla Gui Scale happens to be. Changing the setting
     * eases across rather than snapping, matching the original's 500ms ramp.
     */
    val scaleFactor: Float get() = scale * 2.0f

    fun resetScale() {
        scaleSetting.value = 100
        prevScale = 1.0f
        scale = 1.0f
    }

    private fun getRoundedScale(): Float {
        return round((scaleSetting.value / 100.0f) / 0.1f) * 0.1f
    }

    init {
        safeParallelListener<TickEvent.Post> {
            prevScale = scale
            if (settingTimer.tick(500L)) {
                val diff = scale - getRoundedScale()
                when {
                    diff < -0.025 -> scale += 0.025f
                    diff > 0.025 -> scale -= 0.025f
                    else -> scale = getRoundedScale()
                }
            }
        }

        scaleSetting.listeners.add {
            settingTimer.reset()
        }
    }
}
