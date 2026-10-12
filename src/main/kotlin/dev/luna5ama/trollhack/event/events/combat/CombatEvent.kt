package dev.luna5ama.trollhack.event.events.combat

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventBus
import dev.luna5ama.trollhack.event.EventPosting
import net.minecraft.world.entity.LivingEntity

sealed class CombatEvent : Event {
    abstract val entity: LivingEntity?

    class UpdateTarget(val prevEntity: LivingEntity?, override val entity: LivingEntity?) : CombatEvent(),
        EventPosting by Companion {
        companion object : EventBus()
    }
}
