package dev.luna5ama.trollhack.module.modules.chat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.unformatted
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

internal object AutoReply : Module(
    name = "Auto Reply",
    description = "Automatically reply to direct messages",
    category = Category.CHAT
) {
    private val customMessage = setting("Custom Message", false)
    private val customText = setting("Custom Text", "unchanged", customMessage.atTrue())

    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        listener<PacketEvent.Receive>(9000) {
            val content = when (val packet = it.packet) {
                is ClientboundSystemChatPacket -> packet.content.unformatted
                is ClientboundPlayerChatPacket -> packet.unsignedContent?.unformatted ?: packet.body.content
                else -> return@listener
            }
            if (MessageDetection.Direct.RECEIVE detectNot content) return@listener

            if (customMessage.value) {
                sendServerMessage("/r " + customText.value)
            } else {
                sendServerMessage("/r I just automatically replied, thanks to Troll Hack's AutoReply module!")
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (timer.tickAndReset(5L) && customMessage.value && customText.value.equals("unchanged", true)) {
                NoSpamMessage.sendWarning("$chatName Warning: In order to use the custom $name, please change the CustomText setting in ClickGUI")
            }
        }
    }
}
