package dev.luna5ama.trollhack.util.inventory

import dev.luna5ama.trollhack.event.SafeClientEvent
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot

object InstantFuture : StepFuture {
    override fun timeout(timeout: Long): Boolean {
        return true
    }

    override fun confirm() {
    }
}

class Click(
    private val windowID: Int,
    private val slotProvider: SafeClientEvent.() -> Slot?,
    private val mouseButton: SafeClientEvent.() -> Int?,
    private val type: ContainerInput
) : Step {
    constructor(windowID: Int, slotProvider: SafeClientEvent.() -> Slot?, mouseButton: Int, type: ContainerInput) : this(
        windowID,
        slotProvider,
        { mouseButton },
        type
    )

    constructor(windowID: Int, slot: Slot, mouseButton: Int, type: ContainerInput) : this(
        windowID,
        { slot },
        mouseButton,
        type
    )

    override fun run(event: SafeClientEvent): StepFuture {
        val slot = slotProvider.invoke(event)
        val mouseButton = mouseButton.invoke(event)
        return if (slot != null && mouseButton != null) {
            ClickFuture(event.clickSlot(windowID, slot, mouseButton, type))
        } else {
            InstantFuture
        }
    }
}

/**
 * 26.2 removed inventory transaction confirmations; clicks are considered
 * confirmed as soon as they are sent (vanilla resyncs on mismatch).
 */
class ClickFuture(
    val id: Short,
) : StepFuture {
    private val time = System.currentTimeMillis()
    private var confirmed = true

    override fun timeout(timeout: Long): Boolean {
        return confirmed || System.currentTimeMillis() - time > timeout
    }

    override fun confirm() {
        confirmed = true
    }
}

interface Step {
    fun run(event: SafeClientEvent): StepFuture
}

interface StepFuture {
    fun timeout(timeout: Long): Boolean
    fun confirm()
}
