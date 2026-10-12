package dev.luna5ama.trollhack.event.events.player

import dev.luna5ama.trollhack.event.*
import net.minecraft.client.player.LocalPlayer

sealed class PlayerMoveEvent : Event {
    class Pre(private val player: LocalPlayer) : PlayerMoveEvent(), ICancellable by Cancellable(),
        EventPosting by Companion {
        private val prevX = player.deltaMovement.x
        private val prevY = player.deltaMovement.y
        private val prevZ = player.deltaMovement.z

        val isModified: Boolean
            get() = x != prevX
                || y != prevY
                || z != prevZ

        var x = Double.NaN
            get() = get(field, player.deltaMovement.x)

        var y = Double.NaN
            get() = get(field, player.deltaMovement.y)

        var z = Double.NaN
            get() = get(field, player.deltaMovement.z)

        private fun get(x: Double, y: Double): Double {
            return when {
                cancelled -> 0.0
                !x.isNaN() -> x
                else -> y
            }
        }

        companion object : NamedProfilerEventBus("trollPlayerMove")
    }

    object Post : PlayerMoveEvent(), EventPosting by EventBus()
}
