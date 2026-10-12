package dev.luna5ama.trollhack.event

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.command.execute.ExecuteEvent
import dev.luna5ama.trollhack.command.execute.IExecuteEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer

abstract class AbstractClientEvent {
    val mc = Wrapper.minecraft
    abstract val world: ClientLevel?
    abstract val player: LocalPlayer?
    abstract val playerController: MultiPlayerGameMode?
    abstract val connection: ClientPacketListener?
}

open class ClientEvent : AbstractClientEvent() {
    final override val world: ClientLevel? = mc.level
    final override val player: LocalPlayer? = mc.player
    final override val playerController: MultiPlayerGameMode? = mc.gameMode
    final override val connection: ClientPacketListener? = mc.connection

    inline operator fun <T> invoke(block: ClientEvent.() -> T) = run(block)
}

open class SafeClientEvent internal constructor(
    override val world: ClientLevel,
    override val player: LocalPlayer,
    override val playerController: MultiPlayerGameMode,
    override val connection: ClientPacketListener
) : AbstractClientEvent() {
    inline operator fun <T> invoke(block: SafeClientEvent.() -> T) = run(block)

    companion object : ListenerOwner() {
        var instance: SafeClientEvent? = null; private set

        init {
            listener<ConnectionEvent.Disconnect>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<WorldEvent.Unload>(Int.MAX_VALUE, true) {
                reset()
            }

            listener<RunGameLoopEvent.Tick>(Int.MAX_VALUE, true) {
                update()
            }
        }

        fun update() {
            val world = Wrapper.world ?: return
            val player = Wrapper.player ?: return
            val playerController = Wrapper.minecraft.gameMode ?: return
            val connection = Wrapper.minecraft.connection ?: return

            instance = SafeClientEvent(world, player, playerController, connection)
        }

        fun reset() {
            instance = null
        }
    }
}

class ClientExecuteEvent(
    args: Array<String>
) : ClientEvent(), IExecuteEvent by ExecuteEvent(CommandManager, args)

class SafeExecuteEvent internal constructor(
    world: ClientLevel,
    player: LocalPlayer,
    playerController: MultiPlayerGameMode,
    connection: ClientPacketListener,
    event: ClientExecuteEvent
) : SafeClientEvent(world, player, playerController, connection), IExecuteEvent by event
