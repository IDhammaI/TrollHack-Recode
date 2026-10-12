package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.GuiEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.mixins.accessor.AccessorDisconnectedScreen
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.DisconnectionDetails
import net.minecraft.network.chat.Component
import kotlin.math.max

internal object AutoReconnect : Module(
    name = "Auto Reconnect",
    description = "Automatically reconnects after being disconnected",
    category = Category.MISC,
    alwaysListening = true
) {
    private val delay by setting("Delay", 5.0f, 0.5f..100.0f, 0.5f)

    private var prevServerData: ServerData? = null

    init {
        listener<GuiEvent.Closed> {
            if (it.screen is ConnectScreen) prevServerData = mc.currentServer
        }

        listener<GuiEvent.Displayed> {
            if (isDisabled) return@listener
            if (prevServerData == null && mc.currentServer == null) return@listener

            val screen = it.screen
            if (screen is DisconnectedScreen && screen !is TrollDisconnectedScreen) {
                val accessor = screen as AccessorDisconnectedScreen
                it.screen = TrollDisconnectedScreen(
                    accessor.`trollhack$getParent`(),
                    screen.title,
                    accessor.`trollhack$getDetails`()
                )
            }
        }
    }

    private class TrollDisconnectedScreen(
        private val parentScreen: Screen,
        title: Component,
        details: DisconnectionDetails
    ) : DisconnectedScreen(parentScreen, title, details) {

        private val time = System.currentTimeMillis()

        override fun tick() {
            super.tick()
            if (System.currentTimeMillis() - time >= delay * 1000.0f) {
                val mc = Wrapper.minecraft
                val server = mc.currentServer ?: prevServerData ?: return
                ConnectScreen.startConnecting(
                    parentScreen,
                    mc,
                    ServerAddress.parseString(server.ip),
                    server,
                    false,
                    null
                )
            }
        }

        override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
            super.extractRenderState(graphics, mouseX, mouseY, a)
            val ms = max(delay * 1000.0f - (System.currentTimeMillis() - time), 0.0f).toInt()
            graphics.centeredText(
                Wrapper.minecraft.font,
                "Reconnecting in ${ms}ms",
                width / 2,
                height - 32,
                0xFFFFFFFF.toInt()
            )
        }
    }
}
