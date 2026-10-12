package dev.luna5ama.trollhack.util.threads

import dev.fastmc.common.ParallelUtils
import dev.luna5ama.trollhack.TrollHackMod
import kotlinx.coroutines.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import kotlin.math.max

private val defaultContext =
    CoroutineName("Troll Hack Default") +
        Dispatchers.Default +
        CoroutineExceptionHandler { _, throwable ->
            TrollHackMod.logger.error("Uncaught exception in Troll Hack Default Scope", throwable)
        }

/**
 * Common scope with [Dispatchers.Default]
 */
object DefaultScope : CoroutineScope by CoroutineScope(defaultContext) {
    val context = defaultContext
}

@OptIn(ExperimentalCoroutinesApi::class)
private val concurrentContext =
    CoroutineName("Troll Hack Concurrent") +
        Dispatchers.Default.limitedParallelism(max(ParallelUtils.CPU_THREADS / 2, 1)) +
        CoroutineExceptionHandler { _, throwable ->
            TrollHackMod.logger.error("Uncaught exception in Troll Hack Concurrent Scope", throwable)
        }

object ConcurrentScope :
    CoroutineScope by CoroutineScope(concurrentContext) {
    val context = concurrentContext
}

/**
 * Return true if the job is active, or false is not active or null
 */
val Job?.isActiveOrFalse get() = this?.isActive ?: false

suspend inline fun delay(timeMillis: Int) {
    delay(timeMillis.toLong())
}

private val backgroundPool = ScheduledThreadPoolExecutor(
    ParallelUtils.CPU_THREADS,
    CountingThreadFactory("Troll Hack Background") {
        isDaemon = true
        priority = 3
    }
)

private val backgroundContext =
    CoroutineName("Troll Hack Background") +
        backgroundPool.asCoroutineDispatcher() +
        CoroutineExceptionHandler { _, throwable ->
            TrollHackMod.logger.error("Uncaught exception in Troll Hack Background Scope", throwable)
        }

object BackgroundScope : CoroutineScope by CoroutineScope(backgroundContext) {
    val pool = backgroundPool
    val context = backgroundContext
}
