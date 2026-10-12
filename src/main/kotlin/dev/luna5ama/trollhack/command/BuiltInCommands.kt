package dev.luna5ama.trollhack.command

import dev.luna5ama.trollhack.command.commands.BindCommand
import dev.luna5ama.trollhack.command.commands.HelpCommand
import dev.luna5ama.trollhack.command.commands.ToggleCommand

/**
 * Explicit registry of all built in commands.
 */
internal object BuiltInCommands {
    val list: List<ClientCommand> = listOf(
        BindCommand,
        HelpCommand,
        ToggleCommand,
    )
}
