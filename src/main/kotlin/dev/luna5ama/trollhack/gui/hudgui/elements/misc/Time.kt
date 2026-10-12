package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.TimeUtils

internal object Time : LabelHud(
    name = "Time",
    category = Category.MISC,
    description = "Displays current system time"
) {
    private val showDate by setting("Show Date", true)
    private val showTime by setting("Show Time", true)
    private val dateFormat by setting("Date Format", TimeUtils.DateFormat.DDMMYY, { showDate })
    private val timeFormat by setting("Time Format", TimeUtils.TimeFormat.HHMM, { showTime })
    private val timeUnit by setting("Time Unit", TimeUtils.TimeUnit.H12, { showTime })

    override fun SafeClientEvent.updateText() {
        if (showDate) {
            displayText.add(TimeUtils.getDate(dateFormat), GuiColors.text)
        }
        if (showTime) {
            displayText.add(TimeUtils.getTime(timeFormat, timeUnit), GuiColors.primary)
        }
    }
}
