package dev.luna5ama.trollhack.manager.managers

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.util.TpsCalculator
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.StepFuture
import dev.luna5ama.trollhack.util.inventory.removeHoldingItem
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.*

object InventoryTaskManager : Manager() {
    private val taskQueue = PriorityQueue<InventoryTask>()
    private val timer = TickTimer()
    private var lastTask: InventoryTask? = null

    private val queueLock = Any()

    init {
        safeListener<RunGameLoopEvent.Render> {
            if (lastTask == null && taskQueue.isEmpty()) {
                InventoryTask.resetIdCounter()
                return@safeListener
            }
            if (!timer.tick(0L)) return@safeListener

            lastTaskOrNext()?.let {
                runTask(it)
            }
        }

        listener<WorldEvent.Unload> {
            reset()
        }
    }

    fun addTask(task: InventoryTask) {
        synchronized(queueLock) {
            taskQueue.add(task)
        }
    }

    fun runNow(event: SafeClientEvent, task: InventoryTask) {
        synchronized(InventoryTaskManager) {
            event {
                if (!player.inventoryMenu.carried.isEmpty) {
                    removeHoldingItem()
                }

                while (!task.finished) {
                    task.runTask(event)?.let {
                        handleFuture(it)
                    }
                }

                timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
            }
        }
    }

    private fun SafeClientEvent.lastTaskOrNext(): InventoryTask? {
        return lastTask ?: run {
            val newTask = synchronized(queueLock) {
                taskQueue.poll()?.also { lastTask = it }
            } ?: return null

            if (!player.inventoryMenu.carried.isEmpty) {
                removeHoldingItem()
                return null
            }

            newTask
        }
    }

    private fun SafeClientEvent.runTask(task: InventoryTask) {
        synchronized(InventoryTaskManager) {
            if (mc.gui.screen() is AbstractContainerScreen<*> && !task.runInGui && !player.inventoryMenu.carried.isEmpty) {
                timer.reset(500L)
                return
            }

            if (task.delay == 0L) {
                runNow(this, task)
            } else {
                task.runTask(this)?.let {
                    handleFuture(it)
                    timer.reset((task.delay * TpsCalculator.multiplier).toLong())
                }
            }

            if (task.finished) {
                timer.reset((task.postDelay * TpsCalculator.multiplier).toLong())
                lastTask = null
                return
            }
        }
    }

    private fun handleFuture(future: StepFuture) {
        // 26.2 has no click confirmations; futures confirm instantly
    }

    private fun reset() {
        synchronized(queueLock) {
            timer.reset(-1000000L)
            lastTask?.cancel()
            lastTask = null
            taskQueue.forEach {
                it.cancel()
            }
            taskQueue.clear()
        }
    }
}
