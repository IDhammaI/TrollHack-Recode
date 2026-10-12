package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.atFalse
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket

internal object PacketCancel : Module(
    name = "Packet Cancel",
    description = "Cancels specific packets used for various actions",
    category = Category.PLAYER
) {
    private val all0 = setting("All", false)
    private val all by all0
    private val packetInput by setting("Player Input", true, all0.atFalse())
    private val packetPlayer by setting("Move Player", true, all0.atFalse())
    private val packetEntityAction by setting("Player Command", true, all0.atFalse())
    private val packetUseEntity by setting("Interact", true, all0.atFalse())
    private val packetVehicleMove by setting("Move Vehicle", true, all0.atFalse())

    private var numPackets = 0

    override fun getHudInfo(): String {
        return numPackets.toString()
    }

    init {
        listener<PacketEvent.Send> {
            if (all
                || it.packet is ServerboundPlayerInputPacket && packetInput
                || it.packet is ServerboundMovePlayerPacket && packetPlayer
                || it.packet is ServerboundPlayerCommandPacket && packetEntityAction
                || it.packet is ServerboundInteractPacket && packetUseEntity
                || it.packet is ServerboundMoveVehiclePacket && packetVehicleMove
            ) {
                it.cancel()
                numPackets++
            }
        }

        onDisable {
            numPackets = 0
        }
    }
}
