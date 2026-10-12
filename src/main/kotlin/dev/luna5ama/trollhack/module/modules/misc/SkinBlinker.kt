package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import net.minecraft.world.entity.player.PlayerModelPart
import java.util.EnumSet

internal object SkinBlinker : Module(
    name = "Skin Blinker",
    description = "Toggle your skin layers rapidly for a cool skin effect",
    category = Category.MISC
) {
    private val cape = setting("Cape", false)
    private val jacket = setting("Jacket", true)
    private val leftSleeve = setting("Left Sleeve", true)
    private val rightSleeve = setting("Right Sleeve", true)
    private val leftPantsLeg = setting("Left Pants Leg", true)
    private val rightPantsLeg = setting("Right Pants Leg", true)
    private val hat = setting("Hat", true)
    private val mode by setting("Mode", FlickerMode.HORIZONTAL)
    private val delay by setting("Delay", 10, 0..500, 5, description = "Skin layer toggle delay, in milliseconds")

    private enum class FlickerMode(override val displayName: CharSequence) : DisplayEnum {
        HORIZONTAL("Horizontal"),
        VERTICAL("Vertical"),
        RANDOM("Random")
    }

    private val enabledParts = EnumSet.of(
        PlayerModelPart.JACKET,
        PlayerModelPart.LEFT_SLEEVE,
        PlayerModelPart.RIGHT_SLEEVE,
        PlayerModelPart.LEFT_PANTS_LEG,
        PlayerModelPart.RIGHT_PANTS_LEG,
        PlayerModelPart.HAT
    )

    private val horizontalParts = arrayOf(
        PlayerModelPart.LEFT_SLEEVE,
        PlayerModelPart.LEFT_PANTS_LEG,
        PlayerModelPart.JACKET,
        PlayerModelPart.HAT,
        PlayerModelPart.CAPE,
        PlayerModelPart.RIGHT_PANTS_LEG,
        PlayerModelPart.RIGHT_SLEEVE
    )
    private val verticalParts = arrayOf(
        PlayerModelPart.HAT,
        PlayerModelPart.JACKET,
        PlayerModelPart.CAPE,
        PlayerModelPart.LEFT_SLEEVE,
        PlayerModelPart.RIGHT_SLEEVE,
        PlayerModelPart.LEFT_PANTS_LEG,
        PlayerModelPart.RIGHT_PANTS_LEG
    )

    private val timer = TickTimer()
    private var lastIndex = 0

    override fun getHudInfo(): String {
        return mode.displayString
    }

    init {
        // Per-frame event: RunGameLoopEvent.Start keeps the ms-precision delay
        safeListener<RunGameLoopEvent.Start> {
            if (!timer.tickAndReset(delay.toLong())) return@safeListener

            val part = when (mode) {
                FlickerMode.RANDOM -> PlayerModelPart.entries.toTypedArray().random()
                FlickerMode.VERTICAL -> verticalParts[lastIndex]
                FlickerMode.HORIZONTAL -> horizontalParts[lastIndex]
            }

            if (enabledParts.contains(part)) {
                // 26.2 has no switchModelPartEnabled; toggle then broadcast so the
                // server-synced customisation byte actually updates
                mc.options.setModelPart(part, !mc.options.isModelPartEnabled(part))
                mc.options.broadcastOptions()
            }
            lastIndex = (lastIndex + 1) % 7
        }

        onDisable {
            for (model in PlayerModelPart.entries) {
                mc.options.setModelPart(model, true)
            }
            mc.options.broadcastOptions()
        }

        cape.valueListeners.add { _, it -> togglePart(PlayerModelPart.CAPE, it) }
        jacket.valueListeners.add { _, it -> togglePart(PlayerModelPart.JACKET, it) }
        leftSleeve.valueListeners.add { _, it -> togglePart(PlayerModelPart.LEFT_SLEEVE, it) }
        rightSleeve.valueListeners.add { _, it -> togglePart(PlayerModelPart.RIGHT_SLEEVE, it) }
        leftPantsLeg.valueListeners.add { _, it -> togglePart(PlayerModelPart.LEFT_PANTS_LEG, it) }
        rightPantsLeg.valueListeners.add { _, it -> togglePart(PlayerModelPart.RIGHT_PANTS_LEG, it) }
        hat.valueListeners.add { _, it -> togglePart(PlayerModelPart.HAT, it) }
    }

    private fun togglePart(part: PlayerModelPart, enabled: Boolean) {
        if (enabled) enabledParts.add(part)
        else enabledParts.remove(part)
    }
}
