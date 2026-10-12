@file:Suppress("UNUSED")

package dev.luna5ama.trollhack.util

import dev.luna5ama.trollhack.event.SafeClientEvent
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand

enum class SwingMode {
    CLIENT {
        override fun swingHand(event: SafeClientEvent, hand: InteractionHand) {
            event.player.swing(hand)
        }
    },
    PACKET {
        override fun swingHand(event: SafeClientEvent, hand: InteractionHand) {
            event.connection.send(ServerboundSwingPacket(hand))
        }
    };

    abstract fun swingHand(event: SafeClientEvent, hand: InteractionHand)
}
