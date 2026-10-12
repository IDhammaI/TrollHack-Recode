package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.DefaultScope
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket

/**
 * 26.2 removed the 1.12 window-transaction system, so only the keep-alive delay
 * (the original's Normal mode) is portable.
 */
internal object PingSpoof : Module(
    name = "Ping Spoof",
    category = Category.MISC,
    description = "Adds delay to your ping packets"
) {
    private val delay by setting("Delay", 100, 0..1000, 5)
    private val multiplier by setting("Multiplier", 1, 1..100, 1)

    private val packetTimer = TickTimer()

    override fun getHudInfo(): String {
        return (delay * multiplier).toString()
    }

    init {
        onDisable {
            packetTimer.reset(-114514L)
        }

        listener<PacketEvent.Receive> { event ->
            val packet = event.packet
            if (packet !is ClientboundKeepAlivePacket) return@listener

            packetTimer.reset()
            event.cancel()

            val id = packet.id
            DefaultScope.launch {
                delay((delay * multiplier).toLong())
                onMainThreadSafe {
                    connection.send(ServerboundKeepAlivePacket(id))
                }
            }
        }
    }
}
