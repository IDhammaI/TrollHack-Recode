package dev.luna5ama.trollhack.event.events

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.world.entity.LivingEntity

sealed class EntityEvent(val entity: LivingEntity) : Event {
    class UpdateHealth(entity: LivingEntity, val prevHealth: Float, val health: Float) : EntityEvent(entity),
        EventPosting by Companion {
        companion object : EventBus()
    }

    class Death(entity: LivingEntity) : EntityEvent(entity), EventPosting by Companion {
        companion object : EventBus()
    }
}
