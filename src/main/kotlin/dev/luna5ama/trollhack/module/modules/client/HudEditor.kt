package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

internal object HudEditor : Module(
    name = "Hud Editor",
    description = "Opens the HUD editor",
    category = Category.CLIENT,
    visible = false,
    alwaysListening = true
) {
    init {
        onEnable {
            mc.execute {
                ClickGUI.disable()
                if (mc.gui.screen() !is TrollHudGui) {
                    mc.gui.setScreen(TrollHudGui)
                }
            }
        }

        onDisable {
            mc.execute {
                if (mc.gui.screen() is TrollHudGui) {
                    mc.gui.setScreen(null)
                }
            }
        }
    }
}
