package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.client.gui.screens.Screen

sealed class GuiEvent : Event {
    abstract val screen: Screen?

    class Closed(override val screen: Screen) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }

    class Displayed(override var screen: Screen?) : GuiEvent(), EventPosting by Companion {
        companion object : EventBus()
    }
}
