package dev.luna5ama.trollhack.util.text

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.manager.managers.MessageManager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.threads.onMainThread
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object MessageSendUtils {
    private val mc get() = Wrapper.minecraft

    fun sendChatMessage(message: String) {
        sendRawMessage(coloredName(ChatFormatting.LIGHT_PURPLE) + message)
    }

    fun sendWarningMessage(message: String) {
        sendRawMessage(coloredName(ChatFormatting.GOLD) + message)
    }

    fun sendErrorMessage(message: String) {
        sendRawMessage(coloredName(ChatFormatting.DARK_RED) + message)
    }

    fun sendWarning(message: String) {
        sendRawMessage(coloredName(ChatFormatting.GOLD) + message)
    }

    fun sendTrollCommand(command: String) {
        CommandManager.runCommand(command.removePrefix(CommandManager.prefix))
    }

    fun AbstractModule.sendServerMessage(message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, this, modulePriority)
    }

    fun Any.sendServerMessage(message: String) {
        if (message.isBlank()) return
        MessageManager.addMessageToQueue(message, this, 0)
    }

    fun sendRawMessage(message: String) {
        onMainThread {
            mc.gui.hud.getChat().addClientSystemMessage(Component.literal(message))
        }
    }

    private fun coloredName(textFormatting: ChatFormatting) =
        "${ChatFormatting.GRAY}[$textFormatting${TrollHackMod.NAME}${ChatFormatting.GRAY}]${ChatFormatting.RESET} "
}
