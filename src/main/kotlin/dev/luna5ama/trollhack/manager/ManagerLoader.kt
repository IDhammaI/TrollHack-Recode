package dev.luna5ama.trollhack.manager

import dev.luna5ama.trollhack.TrollHackMod
import kotlin.system.measureTimeMillis

internal object ManagerLoader {
    private val managers = ArrayList<Manager>()

    fun register(manager: Manager) {
        managers.add(manager)
    }

    fun loadAll() {
        val time = measureTimeMillis {
            for (manager in BuiltInManagers.list) {
                manager.subscribe()
            }
        }

        TrollHackMod.logger.info("${BuiltInManagers.list.size} managers loaded, took ${time}ms")
    }
}
