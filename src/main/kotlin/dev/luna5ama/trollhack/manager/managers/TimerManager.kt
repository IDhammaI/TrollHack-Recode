package dev.luna5ama.trollhack.manager.managers

import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.extension.lastValueOrNull
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.threads.runSafe
import java.util.*

object TimerManager : Manager() {
    private val modifiers = TreeMap<AbstractModule, Modifier>().synchronized()

    var globalTicks = Int.MIN_VALUE; private set
    var tickLength = 50.0f; private set

    /**
     * Called from Minecraft.getTickTargetMillis via mixin.
     * Vanilla 26.2 has no writable tick length; the override is applied per-frame here.
     */
    @JvmStatic
    fun getModifiedTickLength(defaultMs: Float): Float {
        return runSafe {
            synchronized(modifiers) {
                modifiers.values.removeIf { it.endTick < globalTicks }
                modifiers.lastValueOrNull()?.tickLength
            }
        } ?: defaultMs
    }

    init {
        listener<RunGameLoopEvent.Start>(Int.MAX_VALUE, true) {
            tickLength = getModifiedTickLength(50.0f)
        }

        listener<TickEvent.Pre>(Int.MAX_VALUE, true) {
            globalTicks++
        }
    }

    fun AbstractModule.resetTimer() {
        modifiers.remove(this)
    }

    fun AbstractModule.modifyTimer(tickLength: Float, timeoutTicks: Int = 1) {
        runSafe {
            val partialTicks = Wrapper.minecraft.deltaTracker.getGameTimeDeltaPartialTick(true)
            modifiers[this@modifyTimer] =
                Modifier(tickLength, globalTicks + Math.round(partialTicks) + timeoutTicks)
        }
    }

    private class Modifier(
        val tickLength: Float,
        val endTick: Int
    )
}
