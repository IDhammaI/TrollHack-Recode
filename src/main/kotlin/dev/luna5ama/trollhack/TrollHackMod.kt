package dev.luna5ama.trollhack

import dev.luna5ama.trollhack.command.CommandManager
import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.gui.GuiManager
import dev.luna5ama.trollhack.manager.ManagerLoader
import dev.luna5ama.trollhack.module.ModuleManager
import dev.luna5ama.trollhack.util.ConfigUtils
import dev.luna5ama.trollhack.util.threads.TimerScope
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File

object TrollHackMod : ClientModInitializer {
    const val NAME = "Troll Hack"
    const val ID = "trollhack"
    const val VERSION = "2.0.0"
    const val DIRECTORY = ID

    @JvmField
    val logger: Logger = LogManager.getLogger(NAME)

    @JvmStatic
    @get:JvmName("isReady")
    var ready = false; private set

    override fun onInitializeClient() {
        logger.info("Initializing $NAME $VERSION")

        val directory = File("${DIRECTORY}/")
        if (!directory.exists()) directory.mkdir()

        ManagerLoader.loadAll()
        ModuleManager.loadAll()
        CommandManager.loadAll()
        GuiManager.loadAll()

        // Must run before the config load: postInit seeds each module's enabled state from its
        // default, so doing it afterwards would throw away whatever the user had saved.
        ModuleManager.postInitAll()
        ConfigUtils.loadAll()
        TimerScope.start()

        ready = true

        Runtime.getRuntime().addShutdownHook(Thread {
            ShutdownEvent.post()
            ConfigUtils.saveAll()
        })

        logger.info("$NAME initialized!")
    }
}
