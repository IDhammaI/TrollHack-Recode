package dev.luna5ama.trollhack.manager

import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.HealthManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.InventoryTaskManager
import dev.luna5ama.trollhack.manager.managers.MessageManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.manager.managers.TotemPopManager
import dev.luna5ama.trollhack.manager.managers.UUIDManager
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.manager.managers.WorldManager

/**
 * Explicit registry of all managers.
 */
internal object BuiltInManagers {
    val list: List<Manager> = listOf(
        CombatManager,
        EntityManager,
        FriendManager,
        HealthManager,
        HoleManager,
        HotbarSwitchManager,
        InventoryTaskManager,
        MessageManager,
        PlayerPacketManager,
        TimerManager,
        TotemPopManager,
        UUIDManager,
        WaypointManager,
        WorldManager,
    )
}
