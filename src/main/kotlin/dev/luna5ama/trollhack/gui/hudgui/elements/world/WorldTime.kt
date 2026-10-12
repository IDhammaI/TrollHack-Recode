package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import org.apache.commons.lang3.time.DurationFormatUtils

/**
 * 1:1 port of the 1.12 `WorldTime` HUD element.
 *
 * Colours read through [GuiColors] instead of `GuiSetting` directly; [GuiColors] forwards live
 * from the same `GuiSetting` settings, so the palette is byte-for-byte the original's.
 *
 * Only real deviation: 26.2 removed `World.getWorldTime()` / the `DayTime` level field entirely --
 * day time is now owned by the world clock registry (see
 * `net.minecraft.util.datafix.fixes.DayTimeToClockFix`, which migrates `DayTime` into
 * `world_clocks.minecraft:overworld.total_ticks`). The direct replacement is
 * `Level.getOverworldClockTime()` (`.mcsrc/net/minecraft/world/level/Level.java:887`), which
 * vanilla itself labels "day time" in its crash report detail and which, like the old field,
 * is an unbounded running tick counter. `getDefaultClockTime()` was deliberately NOT used:
 * it resolves the *current dimension's* clock and returns 0 in dimensions without one, whereas
 * 1.12's `world.worldTime` was always the (overworld) day-time counter.
 */
internal object WorldTime : LabelHud(
    name = "World Time",
    category = Category.WORLD,
    description = "Time in the Minecraft world"
) {

    private val displayMode by setting("Display Mode", DisplayMode.H24)
    private val fromMidNight by setting(
        "From Midnight",
        true,
        { displayMode == DisplayMode.REAL_TIME || displayMode == DisplayMode.TICKS })

    private enum class DisplayMode(override val displayName: CharSequence) : DisplayEnum {
        H12("12-Hours"),
        H24("24-Hours"),
        REAL_TIME("Real Time"),
        TICKS("Ticks"), // Dummy format
    }

    override fun SafeClientEvent.updateText() {
        displayText.add("World Time ", GuiColors.primary)

        val ticks = getWorldTimeTicks()

        when (displayMode) {
            DisplayMode.H12 -> {
                var ticksHalf = ticks % 12000L
                if (ticksHalf < 1000L) ticksHalf += 12000L // Hacky way to display 12:00 instead of 00:00

                val millisHalf = ticksHalf * 3600L
                val timeString = DurationFormatUtils.formatDuration(millisHalf, "HH:mm")

                val period = if (ticks < 12000L) "AM" else "PM"

                displayText.add(timeString, GuiColors.text)
                displayText.add(period, GuiColors.primary)
            }
            DisplayMode.H24 -> {
                val millis = ticks * 3600L
                val timeString = DurationFormatUtils.formatDuration(millis, "HH:mm")

                displayText.add(timeString, GuiColors.text)
            }
            DisplayMode.REAL_TIME -> {
                val realTimeMillis = ticks * 50L
                val timeString = DurationFormatUtils.formatDuration(realTimeMillis, "mm:ss")

                displayText.add(timeString, GuiColors.text)
            }
            DisplayMode.TICKS -> {
                displayText.add("$ticks", GuiColors.text)
                displayText.add("ticks", GuiColors.primary)
            }
        }
    }

    private fun SafeClientEvent.getWorldTimeTicks() =
        if (fromMidNight && (displayMode == DisplayMode.H12 || displayMode == DisplayMode.H24)) {
            Math.floorMod(world.overworldClockTime - 18000L, 24000L)
        } else {
            world.overworldClockTime
        }

}
