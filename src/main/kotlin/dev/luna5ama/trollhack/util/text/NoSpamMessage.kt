package dev.luna5ama.trollhack.util.text

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.util.interfaces.Helper
import dev.luna5ama.trollhack.util.threads.onMainThread
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object NoSpamMessage : Helper {
    private val lastMessages = Int2ObjectOpenHashMap<String>()

    fun sendMessage(message: String) {
        send(coloredName(ChatFormatting.LIGHT_PURPLE, message), message.hashCode())
    }

    fun sendMessage(identifier: Any, message: String) {
        send(coloredName(ChatFormatting.LIGHT_PURPLE, message), identifier.hashCode())
    }

    fun sendWarning(message: String) {
        send(coloredName(ChatFormatting.GOLD, message), message.hashCode())
    }

    fun sendWarning(identifier: Any, message: String) {
        send(coloredName(ChatFormatting.GOLD, message), identifier.hashCode())
    }

    fun sendError(message: String) {
        send(coloredName(ChatFormatting.DARK_RED, message), message.hashCode())
    }

    fun sendError(identifier: Any, message: String) {
        send(coloredName(ChatFormatting.DARK_RED, message), identifier.hashCode())
    }

    private fun send(message: String, id: Int) {
        onMainThread {
            // 26.2 removed id based chat line replacement; suppress exact repeats per id instead
            if (lastMessages.put(id, message) != message) {
                mc.gui.hud.getChat().addClientSystemMessage(Component.literal(message))
            }
        }
    }

    private fun coloredName(textFormatting: ChatFormatting, message: String): String {
        return "${ChatFormatting.GRAY}[$textFormatting${TrollHackMod.NAME}${ChatFormatting.GRAY}]${ChatFormatting.RESET} $message"
    }
}
