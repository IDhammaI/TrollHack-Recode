package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Text based HUD element; subclasses rebuild [displayText] every tick in [updateText].
 */
abstract class LabelHud(
    name: String,
    alias: Array<String> = emptyArray(),
    category: Category,
    description: String,
    alwaysListening: Boolean = false,
    enabledByDefault: Boolean = false,
    defaultDockingH: dev.luna5ama.trollhack.graphics.HAlign = dev.luna5ama.trollhack.graphics.HAlign.LEFT,
    defaultDockingV: dev.luna5ama.trollhack.graphics.VAlign = dev.luna5ama.trollhack.graphics.VAlign.TOP,
    defaultPosX: Float = 2.0f,
    defaultPosY: Float = 2.0f
) : AbstractHudElement(
    name, alias, category, description, alwaysListening, enabledByDefault,
    defaultDockingH, defaultDockingV, defaultPosX, defaultPosY
) {

    protected val displayText = HudText()

    final override val hudWidth: Float get() = displayText.width + 4.0f
    final override val hudHeight: Float get() = displayText.height + 2.0f

    init {
        safeParallelListener<TickEvent.Post> {
            displayText.clear()
            updateText()
            displayText.commit()
        }
    }

    protected abstract fun SafeClientEvent.updateText()

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        super.renderHud(graphics)
        displayText.draw(graphics, dockingH)
    }
}
