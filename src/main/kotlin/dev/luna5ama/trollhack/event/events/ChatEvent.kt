package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Cancellable
import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.ICancellable
import net.minecraft.network.chat.Component

/**
 * Posted for every message the chat is about to display, from MixinChatComponent.
 *
 * Listeners may cancel the event to hide the message, or reassign [component] to rewrite it.
 * Rewriting has to happen here rather than on the packet because 26.2's chat packets are
 * immutable records and player chat is signed over its content.
 */
class ChatEvent(var component: Component) : Event, ICancellable by Cancellable(), EventPosting by Companion {
    companion object : EventBus()
}
