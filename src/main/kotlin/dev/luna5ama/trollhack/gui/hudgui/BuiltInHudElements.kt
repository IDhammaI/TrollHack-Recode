package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.gui.hudgui.elements.client.ActiveModules
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Username
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Watermark
import dev.luna5ama.trollhack.gui.hudgui.elements.combat.Armor
import dev.luna5ama.trollhack.gui.hudgui.elements.combat.CombatItemCount
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.CPS
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.Fps
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.MemoryUsage
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.Ping
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.ServerBrand
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.TPS
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.Time
import dev.luna5ama.trollhack.gui.hudgui.elements.player.DirectionHud
import dev.luna5ama.trollhack.gui.hudgui.elements.player.Durability
import dev.luna5ama.trollhack.gui.hudgui.elements.player.InventoryViewer
import dev.luna5ama.trollhack.gui.hudgui.elements.player.PlayerSpeed
import dev.luna5ama.trollhack.gui.hudgui.elements.player.Rotation
import dev.luna5ama.trollhack.gui.hudgui.elements.player.TimerSpeed
import dev.luna5ama.trollhack.gui.hudgui.elements.world.BiomeHud
import dev.luna5ama.trollhack.gui.hudgui.elements.world.Coordinate
import dev.luna5ama.trollhack.gui.hudgui.elements.world.EntityList
import dev.luna5ama.trollhack.gui.hudgui.elements.world.Radar
import dev.luna5ama.trollhack.gui.hudgui.elements.world.TextRadar
import dev.luna5ama.trollhack.gui.hudgui.elements.world.WorldTime

/**
 * Explicit registry of all built in HUD elements;
 * must be registered before ConfigUtils.loadAll so saved settings apply.
 */
internal object BuiltInHudElements {
    val list: List<AbstractHudElement> = listOf(
        // Client
        ActiveModules,
        Notification,
        Username,
        Watermark,

        // Combat
        Armor,
        CombatItemCount,

        // Misc
        CPS,
        Fps,
        MemoryUsage,
        Ping,
        ServerBrand,
        TPS,
        Time,

        // Player
        DirectionHud,
        Durability,
        InventoryViewer,
        PlayerSpeed,
        Rotation,
        TimerSpeed,

        // World
        BiomeHud,
        Coordinate,
        EntityList,
        Radar,
        TextRadar,
        WorldTime,
    )
}
