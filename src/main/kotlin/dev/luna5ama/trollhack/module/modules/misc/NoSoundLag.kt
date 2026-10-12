package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

internal object NoSoundLag : Module(
    name = "No Sound Lag",
    category = Category.MISC,
    description = "Prevents lag caused by sound machines"
) {
    init {
        listener<PacketEvent.Receive> {
            val packet = it.packet
            if (packet !is ClientboundSoundPacket) return@listener
            if (packet.source == SoundSource.PLAYERS
                && (packet.sound.value() === SoundEvents.ARMOR_EQUIP_GENERIC.value()
                    || packet.sound.value() === SoundEvents.SHIELD_BLOCK.value())
            ) {
                it.cancel()
            }
        }
    }
}
