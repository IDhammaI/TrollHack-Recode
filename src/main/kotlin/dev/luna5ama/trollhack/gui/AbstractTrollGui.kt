package dev.luna5ama.trollhack.gui

import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Marker base for all TrollHack screens (ClickGUI, HudEditor). Used by config
 * auto-save to detect when a TrollHack GUI is open. [WindowScreen] extends this.
 */
abstract class AbstractTrollGui(title: String) : Screen(Component.literal(title))
