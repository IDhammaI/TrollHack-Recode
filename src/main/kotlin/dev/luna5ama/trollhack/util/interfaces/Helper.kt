package dev.luna5ama.trollhack.util.interfaces

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer

interface Helper {
    val mc: Minecraft
        get() = Minecraft.getInstance()

    val world: ClientLevel?
        get() = mc.level

    val player: LocalPlayer?
        get() = mc.player

    val playerController: MultiPlayerGameMode?
        get() = mc.gameMode

    val connection: ClientPacketListener?
        get() = mc.connection
}
