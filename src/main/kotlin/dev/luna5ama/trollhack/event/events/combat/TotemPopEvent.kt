package dev.luna5ama.trollhack.event.events.combat

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.world.entity.player.Player

sealed class TotemPopEvent(val name: String, val count: Int) : Event {
    class Pop(val entity: Player, count: Int) : TotemPopEvent(entity.name.string, count), EventPosting by Companion {
        companion object : EventBus()
    }

    class Death(val entity: Player, count: Int) : TotemPopEvent(entity.name.string, count), EventPosting by Companion {
        companion object : EventBus()
    }

    class Clear(name: String, count: Int) : TotemPopEvent(name, count), EventPosting by Companion {
        companion object : EventBus()
    }
}
