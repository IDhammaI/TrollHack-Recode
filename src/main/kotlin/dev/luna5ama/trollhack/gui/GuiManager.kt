package dev.luna5ama.trollhack.gui

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.hudgui.BuiltInHudElements
import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.module.modules.client.Hud
import dev.luna5ama.trollhack.util.KeyboardUtils
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.collections.AliasSet
import org.lwjgl.glfw.GLFW
import kotlin.system.measureTimeMillis

/**
 * Registry + render dispatcher for HUD elements.
 */
object GuiManager : AlwaysListening {
    private val hudElementSet = AliasSet<AbstractHudElement>()

    val hudElements: Collection<AbstractHudElement> get() = hudElementSet.distinct()

    internal fun loadAll() {
        val time = measureTimeMillis {
            for (element in BuiltInHudElements.list) {
                register(element)
            }

            // Build the ClickGUI windows now, not on first open: their geometry settings have to
            // exist before ConfigUtils.loadAll() runs or the saved layout is silently dropped.
            dev.luna5ama.trollhack.gui.clickgui.TrollClickGui.build()
        }

        TrollHackMod.logger.info("${BuiltInHudElements.list.size} HUD elements loaded, took ${time}ms")
    }

    internal fun register(element: AbstractHudElement) {
        hudElementSet.add(element)
        element.onRegister()
    }

    fun getHudElementOrNull(name: String?) = name?.let { hudElementSet[it] }

    init {
        listener<Render2DEvent.Troll> { event ->
            if (Hud.isDisabled) return@listener
            // The editor draws elements itself (with frames)
            if (Wrapper.minecraft.gui.screen() is TrollHudGui) return@listener

            for (element in hudElements) {
                if (!element.visible) continue

                event.graphics.pose().pushMatrix()
                event.graphics.pose().translate(element.renderPosX, element.renderPosY)
                event.graphics.pose().scale(element.scale, element.scale)
                element.renderHud(event.graphics)
                event.graphics.pose().popMatrix()
            }
        }

        listener<InputEvent.Keyboard> { event ->
            if (!event.state) return@listener
            if (Wrapper.minecraft.gui.screen() != null) return@listener
            if (event.key == KeyboardUtils.KEY_NONE) return@listener
            if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_F3)) return@listener

            for (element in hudElements) {
                if (element.bind.isDown(event.key)) {
                    element.visible = !element.visible
                }
            }
        }
    }
}
