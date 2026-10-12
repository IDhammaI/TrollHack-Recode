package dev.luna5ama.trollhack.module.modules.player

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.TpsCalculator
import net.minecraft.network.protocol.game.ServerboundSwingPacket

/**
 * Degradation: on 1.12 this module additionally cancelled the client side swing call itself
 * (MixinEntityPlayerSP injected into EntityPlayerSP.swingArm and called [checkSwingDelay]),
 * which also suppressed the local swing animation. This port has no such hook on 26.2 and only
 * filters the outgoing ServerboundSwingPacket. Every setting is kept 1:1 - the packet rate
 * limiting they control is fully intact; [checkSwingDelay] is kept public for a future hook.
 */
internal object SwingLimiter : Module(
    name = "Swing Limiter",
    category = Category.PLAYER,
    description = "Limits swing packet",
    modulePriority = 2000
) {
    private val ticks by setting("Ticks", 10, 0..40, 1)
    private val tpsSync by setting("Tps Sync", true)

    private val swingTimer = TickTimer()

    init {
        listener<PacketEvent.Send>(-9999) {
            if (it.packet is ServerboundSwingPacket) {
                if (checkSwingDelay()) {
                    swingTimer.reset()
                } else {
                    it.cancel()
                }
            }
        }
    }

    @JvmStatic
    fun checkSwingDelay(): Boolean {
        return swingTimer.tick(
            if (tpsSync) (ticks * TpsCalculator.multiplier * 50.0f).toLong()
            else ticks * 50L
        )
    }
}
