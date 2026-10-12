package dev.luna5ama.trollhack.module

import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.util.collections.AliasSet
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.interfaces.Helper
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.system.measureTimeMillis

object ModuleManager : Helper {
    private val moduleSet = AliasSet<AbstractModule>()
    private val modulesDelegate = AsyncCachedValue(5L, TimeUnit.SECONDS) {
        moduleSet.distinct().sortedBy { it.rootName }
    }
    val modules by modulesDelegate

    /**
     * Loads all built in modules.
     *
     * Fabric can't cheaply scan the classpath like the old Forge class discovery,
     * so modules are registered from the explicit list in [BuiltInModules].
     */
    internal fun loadAll() {
        val time = measureTimeMillis {
            for (module in BuiltInModules.list) {
                register(module)
            }
        }

        TrollHackMod.logger.info("${BuiltInModules.list.size} modules loaded, took ${time}ms")
    }

    internal fun postInitAll() {
        for (module in moduleSet) {
            module.postInit()
        }
    }

    /**
     * onEnable blocks whose module was restored as enabled by the config load.
     *
     * The initial load runs from the client entrypoint, which Minecraft calls from inside its own
     * constructor, so mc.options, mc.window and mc.gui do not exist yet. Modules that set up against
     * them (FullBright reading gamma, SkinBlinker touching model parts, ...) would NPE and lose their
     * saved enabled state, so their setup is held here and run on the first frame instead.
     */
    private val deferredEnables = ConcurrentLinkedQueue<Pair<AbstractModule, () -> Unit>>()

    internal fun deferEnable(module: AbstractModule, block: () -> Unit) {
        deferredEnables.add(module to block)
    }

    /** Runs anything [deferEnable] held back. Called once the game loop is running. */
    fun runDeferredEnables() {
        while (true) {
            val (module, block) = deferredEnables.poll() ?: return
            try {
                block()
            } catch (e: Throwable) {
                TrollHackMod.logger.error("Failed running deferred enable for ${module.internalName}", e)
            }
        }
    }

    internal fun register(module: AbstractModule) {
        moduleSet.add(module)
        if (module.enabledByDefault || module.alwaysEnabled) module.enable()
        if (module.alwaysListening) module.subscribe()

        modulesDelegate.update()
    }

    internal fun unregister(module: AbstractModule) {
        moduleSet.remove(module)
        module.unsubscribe()

        modulesDelegate.update()
    }

    fun getModuleOrNull(moduleName: String?) = moduleName?.let { moduleSet[it] }
}
