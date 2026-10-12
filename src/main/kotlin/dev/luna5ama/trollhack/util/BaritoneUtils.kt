package dev.luna5ama.trollhack.util

import net.minecraft.world.level.block.Block

/**
 * Baritone is not available on Minecraft 26.2 yet.
 * This stub keeps the rest of the client compiling; everything reports "not present".
 */
object BaritoneUtils {
    val isAvailable: Boolean get() = false

    val prefix: String get() = "#"

    val isPathing: Boolean get() = false

    val isActive: Boolean get() = false

    var settingEnsureY = false

    val primaryProcessName: String get() = ""

    val baritoneCachedBlocks: List<Block> get() = emptyList()

    fun cancelEverything() {
    }

    fun pause() {
    }

    fun unpause() {
    }
}
