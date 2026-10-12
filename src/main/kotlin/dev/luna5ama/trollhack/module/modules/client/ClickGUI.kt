package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.gui.clickgui.TrollClickGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import org.lwjgl.glfw.GLFW

internal object ClickGUI : Module(
    name = "Click GUI",
    description = "Opens the Click GUI",
    category = Category.CLIENT,
    visible = false,
    alwaysListening = true
) {
    init {
        bind.value.setBind(GLFW.GLFW_KEY_Y)

        onEnable {
            mc.execute {
                if (mc.gui.screen() !is TrollClickGui) {
                    mc.gui.setScreen(TrollClickGui)
                }
            }
        }

        onDisable {
            mc.execute {
                if (mc.gui.screen() is TrollClickGui) {
                    mc.gui.setScreen(null)
                }
            }
        }
    }
}
