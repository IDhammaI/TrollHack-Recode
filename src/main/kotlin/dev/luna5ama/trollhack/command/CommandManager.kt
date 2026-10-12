package dev.luna5ama.trollhack.command

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.ClientExecuteEvent
import dev.luna5ama.trollhack.event.IListenerOwner
import dev.luna5ama.trollhack.module.modules.client.CommandSetting
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.formatValue
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.onMainThread
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

object CommandManager : AbstractCommandManager<ClientExecuteEvent>() {
    val prefix: String get() = CommandSetting.prefix

    /**
     * Loads all built in commands from the explicit registry in [BuiltInCommands].
     */
    internal fun loadAll() {
        val time = measureTimeMillis {
            for (command in BuiltInCommands.list) {
                register(command)
            }
        }

        TrollHackMod.logger.info("${BuiltInCommands.list.size} commands loaded, took ${time}ms")
    }

    override fun register(builder: CommandBuilder<ClientExecuteEvent>): Command<ClientExecuteEvent> {
        synchronized(lockObject) {
            (builder as? IListenerOwner)?.subscribe()
            return super.register(builder)
        }
    }

    override fun unregister(builder: CommandBuilder<ClientExecuteEvent>): Command<ClientExecuteEvent>? {
        synchronized(lockObject) {
            (builder as? IListenerOwner)?.unsubscribe()
            return super.unregister(builder)
        }
    }

    fun runCommand(string: String) {
        ConcurrentScope.launch {
            val args = tryParseArgument(string) ?: return@launch
            TrollHackMod.logger.debug("Running command with args: [${args.joinToString()}]")

            try {
                try {
                    invoke(ClientExecuteEvent(args))
                } catch (e: CommandNotFoundException) {
                    handleCommandNotFoundException(args.first())
                } catch (e: SubCommandNotFoundException) {
                    handleSubCommandNotFoundException(string, args, e)
                }
            } catch (e: Exception) {
                NoSpamMessage.sendMessage("Error occurred while running command! (${e.message}), check the log for info!")
                TrollHackMod.logger.warn("Error occurred while running command!", e)
            }
        }
    }

    fun tryParseArgument(string: String) = try {
        parseArguments(string)
    } catch (e: IllegalArgumentException) {
        NoSpamMessage.sendMessage(e.message.toString())
        null
    }

    override suspend fun invoke(event: ClientExecuteEvent) {
        val name = event.args.getOrNull(0) ?: throw IllegalArgumentException("Arguments can not be empty!")
        val command = getCommand(name)
        val finalArg = command.finalArgs.firstOrNull { it.checkArgs(event.args) }
            ?: throw SubCommandNotFoundException(event.args, command)

        onMainThread {
            runBlocking {
                finalArg.invoke(event)
            }
        }
    }

    private fun handleCommandNotFoundException(command: String) {
        NoSpamMessage.sendMessage(
            "Unknown command: ${formatValue("$prefix$command")}. " +
                "Run ${formatValue("${prefix}help")} for a list of commands."
        )
    }

    private suspend fun handleSubCommandNotFoundException(
        string: String,
        args: Array<String>,
        e: SubCommandNotFoundException
    ) {
        val bestCommand = e.command.finalArgs.maxByOrNull { it.countArgs(args) }

        var message = "Invalid syntax: ${formatValue("$prefix$string")}\n"

        if (bestCommand != null) message += "Did you mean ${formatValue("$prefix${bestCommand.printArgHelp()}")}?\n"

        message += "\nRun ${formatValue("${prefix}help ${e.command.name}")} for a list of available arguments."

        NoSpamMessage.sendMessage(message)
    }
}
