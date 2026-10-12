package dev.luna5ama.trollhack.module.modules.chat

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module

/**
 * 26.2 keeps its own trimming inside ChatComponent instead of exposing the chat line lists,
 * so this raises vanilla's hard cap of 100 retained lines from MixinChatComponent rather than
 * rebuilding the history list the way the 1.12 version did.
 */
internal object ExtraChatHistory : Module(
    name = "Extra Chat History",
    alias = arrayOf("InfiniteChat", "InfiniteChatHistory"),
    category = Category.CHAT,
    description = "Show more messages in the chat history",
    visible = false
) {
    private val maxMessages by setting("Max Message", 1000, 100..5000, 100)

    @JvmStatic
    fun limitOr(vanilla: Int): Int {
        return if (isEnabled) maxMessages else vanilla
    }
}
