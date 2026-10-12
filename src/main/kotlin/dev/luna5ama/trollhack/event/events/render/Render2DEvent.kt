package dev.luna5ama.trollhack.event.events.render

import dev.luna5ama.trollhack.event.Event
import dev.luna5ama.trollhack.event.EventPosting
import dev.luna5ama.trollhack.event.NamedProfilerEventBus
import net.minecraft.client.gui.GuiGraphicsExtractor

sealed class Render2DEvent(val graphics: GuiGraphicsExtractor, val partialTicks: Float) : Event {

    class Mc(graphics: GuiGraphicsExtractor, partialTicks: Float) : Render2DEvent(graphics, partialTicks),
        EventPosting by Companion {
        companion object : NamedProfilerEventBus("mc")
    }

    class Absolute(graphics: GuiGraphicsExtractor, partialTicks: Float) : Render2DEvent(graphics, partialTicks),
        EventPosting by Companion {
        companion object : NamedProfilerEventBus("absolute")
    }

    class Troll(graphics: GuiGraphicsExtractor, partialTicks: Float) : Render2DEvent(graphics, partialTicks),
        EventPosting by Companion {
        companion object : NamedProfilerEventBus("troll")
    }
}
