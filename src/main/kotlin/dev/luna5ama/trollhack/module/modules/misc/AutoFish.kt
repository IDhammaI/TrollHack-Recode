package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.startUseItem
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.world.isWater
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.world.item.Items
import java.lang.Math.random
import kotlin.math.abs

internal object AutoFish : Module(
    name = "Auto Fish",
    category = Category.MISC,
    description = "Automatically catch fish"
) {
    private val mode = setting("Mode", Mode.BOUNCE)
    private val autoCast = setting("Auto Cast", true)
    private val castDelay = setting(
        "Auto Cast Delay",
        5,
        1..20,
        1,
        autoCast.atTrue(),
        description = "Delay before starting fishing when holding a fishing rod, in seconds"
    )
    private val catchDelay =
        setting("Catch Delay", 300, 50..2000, 50, description = "Delay before catching the fish, in milliseconds")
    private val recastDelay = setting(
        "Recast Delay",
        450,
        50..2000,
        50,
        description = "Delay before recasting the fishing rod, in milliseconds"
    )
    private val variation =
        setting("Variation", 100, 0..1000, 50, description = "Randomize the delays in specific range, in milliseconds")

    @Suppress("UNUSED")
    private enum class Mode {
        BOUNCE, SPLASH, ANY_SPLASH, ALL
    }

    private var catching = false
    private var recasting = false
    private val timer = TickTimer()

    init {
        safeListener<PacketEvent.Receive> {
            if (player.fishing == null || !isStabled()) return@safeListener
            val packet = it.packet
            if (mode.value == Mode.BOUNCE || packet !is ClientboundSoundPacket) return@safeListener
            if (isSplash(packet)) catch()
        }

        safeListener<TickEvent.Post> {
            if (player.mainHandItem.item != Items.FISHING_ROD) { // Not holding a rod, do nothing
                reset()
                return@safeListener
            }

            if (player.fishing == null) {
                if (recasting) { // Recast the fishing rod
                    if (timer.tickAndReset(recastDelay.value)) {
                        mc.startUseItem()
                        reset()
                    }
                    // The vendored TickTimer has no per-call TimeUnit overload; this timer
                    // runs in milliseconds, so the seconds-based cast delay is converted here
                } else if (autoCast.value && timer.tickAndReset(castDelay.value * 1000L)) {
                    mc.startUseItem()
                    reset()
                }
            } else if (isStabled() && isOnWater()) {
                if (catching) { // Catch the fish
                    if (timer.tickAndReset(catchDelay.value)) {
                        mc.startUseItem()
                        recast()
                    }
                } else {
                    // Bounce detection
                    if ((mode.value == Mode.BOUNCE || mode.value == Mode.ALL) && isBouncing()) {
                        catch()
                    }
                }
            } else if (isStabled()) {
                // Hooked a block: recast
                mc.startUseItem()
                reset()
            }
        }

        onToggle {
            reset()
        }
    }

    private fun SafeClientEvent.isStabled(): Boolean {
        val hook = player.fishing ?: return false
        if (recasting) return false
        val motion = hook.deltaMovement
        return abs(motion.x) + abs(motion.z) < 0.01
    }

    private fun SafeClientEvent.isOnWater(): Boolean {
        val hook = player.fishing ?: return false
        val pos = hook.blockPosition()
        return world.isWater(pos) || world.isWater(pos.below())
    }

    private fun SafeClientEvent.isSplash(packet: ClientboundSoundPacket): Boolean {
        val hook = player.fishing ?: return false
        if (mode.value == Mode.SPLASH && hook.distanceTo(packet.x, packet.y, packet.z) > 2) return false

        val soundName = packet.sound.value().location().path.lowercase()
        return (mode.value != Mode.SPLASH && isAnySplash(soundName)) || soundName.contains("fishing_bobber.splash")
    }

    private fun isAnySplash(soundName: String): Boolean {
        return soundName.contains("generic.splash")
            || soundName.contains("hostile.splash")
            || soundName.contains("player.splash")
    }

    private fun SafeClientEvent.isBouncing(): Boolean {
        val hook = player.fishing ?: return false
        if (!isOnWater()) return false
        return hook.deltaMovement.y !in -0.05..0.05
    }

    private fun catch() {
        if (catching) return
        resetTimer()
        catching = true
        recasting = false
    }

    private fun recast(extraDelay: Long = 0L) {
        if (recasting) return
        resetTimer()
        timer.reset(extraDelay)
        catching = false
        recasting = true
    }

    private fun reset() {
        resetTimer()
        catching = false
        recasting = false
    }

    private fun resetTimer() {
        val offset = if (variation.value > 0) (random() * (variation.value * 2) - variation.value).toLong() else 0
        timer.reset(offset)
    }
}
