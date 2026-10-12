@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.common

/**
 * Minimal re-implementation of the fastmc-common time utilities used by TrollHack.
 * Kept in the original package so ported code compiles unchanged.
 */
enum class TimeUnit(val multiplier: Long) {
    NANOSECONDS(1L),
    MICROSECONDS(1_000L),
    MILLISECONDS(1_000_000L),
    TICKS(50_000_000L),
    SECONDS(1_000_000_000L),
    MINUTES(60_000_000_000L),
    HOURS(3_600_000_000_000L);
}

class TickTimer(val timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
    var time = System.nanoTime()
        private set

    fun tick(delay: Int): Boolean {
        return tick(delay.toLong())
    }

    fun tick(delay: Long): Boolean {
        return System.nanoTime() - time >= delay * timeUnit.multiplier
    }

    fun tickAndReset(delay: Int): Boolean {
        return tickAndReset(delay.toLong())
    }

    fun tickAndReset(delay: Long): Boolean {
        return tick(delay).also {
            if (it) reset()
        }
    }

    fun reset() {
        time = System.nanoTime()
    }

    fun reset(offset: Long) {
        time = System.nanoTime() + offset
    }

    fun timeElapsed(): Long {
        return (System.nanoTime() - time) / timeUnit.multiplier
    }
}

object ParallelUtils {
    @JvmField
    val CPU_THREADS = Runtime.getRuntime().availableProcessors()
}
