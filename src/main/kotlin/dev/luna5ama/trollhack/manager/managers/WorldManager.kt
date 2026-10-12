package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.BlockBreakEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket

/**
 * Posts the server-authoritative world events. 26.2 removed IWorldEventListener,
 * so server block changes are read straight off the packets
 * (client-side changes come from MixinClientLevel instead).
 */
object WorldManager : Manager() {
    init {
        safeListener<PacketEvent.Receive> { event ->
            when (val packet = event.packet) {
                is ClientboundBlockUpdatePacket -> {
                    val pos = packet.pos
                    WorldEvent.ServerBlockUpdate(pos, world.getBlockState(pos), packet.blockState).post()
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    packet.runUpdates { pos, state ->
                        val immutable = pos.immutable()
                        WorldEvent.ServerBlockUpdate(immutable, world.getBlockState(immutable), state).post()
                    }
                }

                is ClientboundBlockDestructionPacket -> {
                    BlockBreakEvent(packet.id, packet.pos, packet.progress).post()
                }
            }
        }
    }
}
