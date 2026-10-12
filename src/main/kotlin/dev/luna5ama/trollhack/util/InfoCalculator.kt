package dev.luna5ama.trollhack.util

import net.minecraft.world.level.Level

object InfoCalculator {
    private val mc = Wrapper.minecraft

    fun getServerType() = if (mc.hasSingleplayerServer()) "Singleplayer" else mc.currentServer?.ip
        ?: "Main Menu"

    fun ping() = mc.player?.let { mc.connection?.getPlayerInfo(it.uuid)?.latency ?: 1 } ?: -1

    fun dimension() = when (mc.player?.level()?.dimension()) {
        Level.NETHER -> "Nether"
        Level.OVERWORLD -> "Overworld"
        Level.END -> "End"
        else -> "No Dimension"
    }
}
