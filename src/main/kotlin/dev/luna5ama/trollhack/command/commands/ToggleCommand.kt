package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.ChatFormatting

object ToggleCommand : ClientCommand(
    name = "toggle",
    alias = arrayOf("switch", "t"),
    description = "Toggle a module on and off!"
) {
    init {
        module("module") { moduleArg ->
            execute {
                val module = moduleArg.value
                module.toggle()
                NoSpamMessage.sendMessage(
                    module.nameAsString +
                        if (module.isEnabled) " ${ChatFormatting.GREEN}enabled"
                        else " ${ChatFormatting.RED}disabled"
                )
            }
        }
    }
}
