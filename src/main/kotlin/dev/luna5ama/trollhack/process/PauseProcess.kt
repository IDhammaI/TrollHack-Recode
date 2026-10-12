package dev.luna5ama.trollhack.process

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.module.AbstractModule

/**
 * Baritone is not available on 26.2 yet; this keeps the pause bookkeeping
 * so modules can query [isPausing], while the actual pathing pause is a no-op.
 */
object PauseProcess {

    private val pauseModules = HashMap<AbstractModule, Long>()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var lastPausingModule: AbstractModule? = null

    fun isActive(): Boolean {
        cleanUp()
        return pauseModules.isNotEmpty()
    }

    private fun cleanUp() {
        if (timer.tickAndReset(1L)) {
            pauseModules.entries.removeIf { it.key.isDisabled || System.currentTimeMillis() - it.value > 3000L }
        }
    }

    fun AbstractModule.pauseBaritone() {
        lastPausingModule = this
        pauseModules[this] = System.currentTimeMillis()
    }

    fun AbstractModule.unpauseBaritone() {
        pauseModules.remove(this)
    }

    fun isPausing(module: AbstractModule): Boolean {
        cleanUp()
        return pauseModules.containsKey(module)
    }
}
