package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus
import dev.luna5ama.trollhack.graphics.Renderer3D

class Render3DEvent(
    val renderer: Renderer3D,
    val partialTicks: Float
) : Event, EventPosting by Companion {
    companion object : NamedProfilerEventBus("trollRender3D")
}
