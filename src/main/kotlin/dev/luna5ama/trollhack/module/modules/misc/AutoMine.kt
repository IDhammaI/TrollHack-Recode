package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.continueAttack

/**
 * The original drove ore selection (iron/diamond/gold/coal/logs) through Baritone's
 * `mine` command, which does not exist on 26.2. Only the Manual mode is ported; the
 * ore settings are omitted rather than left as controls that do nothing.
 */
internal object AutoMine : Module(
    name = "Auto Mine",
    description = "Keeps mining the block you are looking at",
    category = Category.MISC
) {
    init {
        safeListener<TickEvent.Pre> {
            mc.continueAttack(true)
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }
    }
}
