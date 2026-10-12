package dev.luna5ama.trollhack.gui.hudgui.elements.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import java.util.concurrent.ConcurrentLinkedDeque

internal object CPS : LabelHud(
    name = "CPS",
    category = Category.MISC,
    description = "Displays clicks per second"
) {
    private val clickTimes = ConcurrentLinkedDeque<Long>()

    init {
        listener<InputEvent.Mouse> {
            if (it.button == 0 && it.state) {
                clickTimes.add(System.currentTimeMillis())
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        val cutoff = System.currentTimeMillis() - 1000L
        while (clickTimes.isNotEmpty() && clickTimes.peekFirst() < cutoff) {
            clickTimes.pollFirst()
        }

        displayText.add(clickTimes.size.toString(), GuiColors.text)
        displayText.add("CPS", GuiColors.primary)
    }
}
