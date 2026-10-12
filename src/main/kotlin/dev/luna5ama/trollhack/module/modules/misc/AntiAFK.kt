package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.unformatted
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.InteractionHand
import kotlin.random.Random

/**
 * The original's Walk action used Baritone pathing, which does not exist on 26.2;
 * the swing/jump/turn actions, auto-reply and idle timeout all work as before.
 */
internal object AntiAFK : Module(
    name = "Anti AFK",
    category = Category.MISC,
    description = "Prevents being kicked for AFK"
) {
    private val delay by setting("Action Delay", 50, 5..100, 5)
    private val variation by setting("Variation", 25, 0..50, 5)
    private val autoReply by setting("Auto Reply", true)
    private val swing = setting("Swing", true)
    private val jump = setting("Jump", true)
    private val turn = setting("Turn", true)
    private val inputTimeout by setting(
        "Idle Timeout",
        0,
        0..15,
        1,
        description = "Starts AntiAFK after being idle for longer than specific minutes, 0 to disable"
    )

    private val inputTimer = TickTimer(TimeUnit.MINUTES)
    private val actionTimer = TickTimer(TimeUnit.TICKS)
    private var nextActionDelay = 0

    private enum class Action {
        SWING, JUMP, TURN
    }

    override fun getHudInfo(): String {
        if (inputTimeout == 0) return ""
        // TickTimer stores nanoTime, not millis
        return ((System.nanoTime() - inputTimer.time) / 1_000_000_000L).toString()
    }

    init {
        onEnable {
            inputTimer.reset()
        }

        listener<PacketEvent.Receive>(10000) {
            if (!autoReply) return@listener

            val content = when (val packet = it.packet) {
                is ClientboundSystemChatPacket -> packet.content.unformatted
                is ClientboundPlayerChatPacket -> packet.unsignedContent?.unformatted ?: packet.body.content
                else -> return@listener
            }

            if (MessageDetection.Direct.RECEIVE detect content) {
                sendServerMessage("/r I am currently AFK and using Troll Hack!")
            }
        }

        listener<InputEvent.Mouse> {
            if (inputTimeout > 0 && (mc.options.keyAttack.isDown || mc.options.keyUse.isDown)) {
                inputTimer.reset()
            }
        }

        listener<InputEvent.Keyboard> {
            if (inputTimeout > 0 && (mc.options.keyJump.isDown || mc.options.keyShift.isDown
                    || mc.options.keyUp.isDown || mc.options.keyDown.isDown
                    || mc.options.keyLeft.isDown || mc.options.keyRight.isDown)
            ) {
                inputTimer.reset()
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (inputTimeout > 0) {
                if (player.realSpeed > 0.2) inputTimer.reset()
                if (!inputTimer.tick(inputTimeout.toLong())) return@safeParallelListener
            }

            if (actionTimer.tickAndReset(nextActionDelay.toLong())) {
                nextActionDelay = delay + if (variation == 0) 0 else (0..variation).random()

                when (getAction()) {
                    Action.SWING -> player.swing(InteractionHand.MAIN_HAND)
                    Action.JUMP -> player.jumpFromGround()
                    Action.TURN -> player.yRot = Random.nextDouble(-180.0, 180.0).toFloat()
                    null -> {}
                }
            }
        }
    }

    private fun getAction(): Action? {
        val enabled = ArrayList<Action>(3)
        if (swing.value) enabled.add(Action.SWING)
        if (jump.value) enabled.add(Action.JUMP)
        if (turn.value) enabled.add(Action.TURN)
        return enabled.randomOrNull()
    }
}
