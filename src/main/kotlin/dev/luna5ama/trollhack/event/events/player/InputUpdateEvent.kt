package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.client.player.ClientInput

class InputUpdateEvent(val movementInput: ClientInput) : Event, EventPosting by Companion {
    companion object : EventBus()
}
