package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import java.util.concurrent.ConcurrentLinkedDeque

internal object PlayerSpeed : LabelHud(
    name = "Player Speed",
    category = Category.PLAYER,
    description = "Displays your movement speed"
) {
    private val speedUnit by setting("Speed Unit", SpeedUnit.MPS)
    private val averageTicks by setting("Average Speed Ticks", 10, 1..50, 1)

    private val samples = ConcurrentLinkedDeque<Double>()

    @Suppress("UNUSED")
    private enum class SpeedUnit(override val displayName: CharSequence, val multiplier: Double, val unit: String) : DisplayEnum {
        MPS("m/s", 20.0, "m/s"),
        KMH("km/h", 72.0, "km/h")
    }

    init {
        safeParallelListener<TickEvent.Post> {
            samples.add(player.realSpeed)
            while (samples.size > averageTicks) {
                samples.pollFirst()
            }
        }
    }

    override fun SafeClientEvent.updateText() {
        val average = if (samples.isEmpty()) 0.0 else samples.sum() / samples.size
        displayText.add("%.2f".format(average * speedUnit.multiplier), GuiColors.text)
        displayText.add(speedUnit.unit, GuiColors.primary)
    }
}
