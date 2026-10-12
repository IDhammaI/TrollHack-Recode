package dev.luna5ama.trollhack.module.modules.chat

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.TimeUtils
import dev.luna5ama.trollhack.util.accessor.serverBrand
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.ChatFormatting
import net.minecraft.world.Difficulty
import net.minecraft.world.level.Level

internal object AutoQMain : Module(
    name = "Auto QMain",
    description = "Automatically does '/queue main'",
    category = Category.CHAT,
    visible = false
) {
    private val delay by setting("Delay", 5000, 0..15000, 100)
    private val twoBeeCheck by setting("2B Check", true)
    private val command by setting("Command", "/queue main")

    private val timer = TickTimer()

    init {
        safeListener<TickEvent.Pre> {
            if (world.difficulty == Difficulty.PEACEFUL
                && world.dimension() == Level.END
                && (!twoBeeCheck || connection.serverBrand?.contains("2b2t") == true)
                && timer.tickAndReset(delay.toLong())
            ) {
                sendQueueMain()
            }
        }
    }

    private fun sendQueueMain() {
        NoSpamMessage.sendMessage(
            this,
            "$chatName Run ${ChatFormatting.GRAY format command} at ${
                ChatFormatting.GRAY format TimeUtils.getTime(
                    TimeUtils.TimeFormat.HHMMSS,
                    TimeUtils.TimeUnit.H24
                )
            }"
        )
        sendServerMessage(command)
    }
}
