package dev.luna5ama.trollhack.module.modules.chat

import dev.luna5ama.trollhack.event.events.ChatEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.TimeUtils
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.text.EnumTextColor
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.network.chat.Component

internal object ChatTimestamp : Module(
    name = "Chat Timestamp",
    category = Category.CHAT,
    description = "Shows the time a message was sent beside the message",
    visible = false
) {
    private val color by setting("Color", EnumTextColor.GRAY)
    private val separator by setting("Separator", Separator.ARROWS)
    private val timeFormat by setting("Time Format", TimeUtils.TimeFormat.HHMM)
    private val timeUnit by setting("Time Unit", TimeUtils.TimeUnit.H12)

    init {
        // Runs last so the stamp ends up in front of whatever other modules did to the message
        listener<ChatEvent>(-69696420) {
            it.component = Component.literal(formattedTime).append(it.component)
        }
    }

    val formattedTime: String
        get() = "${separator.left}${color format TimeUtils.getTime(timeFormat, timeUnit)}${separator.right} "

    val time: String
        get() = "${separator.left}${TimeUtils.getTime(timeFormat, timeUnit)}${separator.right} "

    @Suppress("unused")
    private enum class Separator(override val displayName: CharSequence, val left: String, val right: String) :
        DisplayEnum {
        ARROWS("< >", "<", ">"),
        SQUARE_BRACKETS("[ ]", "[", "]"),
        CURLY_BRACKETS("{ }", "{", "}"),
        ROUND_BRACKETS("( )", "(", ")"),
        NONE("None", "", "")
    }
}
