package dev.luna5ama.trollhack.util

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer

object Wrapper {
    @JvmStatic
    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    @JvmStatic
    val player: LocalPlayer?
        get() = minecraft.player

    @JvmStatic
    val world: ClientLevel?
        get() = minecraft.level
}
