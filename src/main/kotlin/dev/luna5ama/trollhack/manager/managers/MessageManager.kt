package dev.luna5ama.trollhack.manager.managers

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.modules.client.ChatSetting
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.threads.onMainThread
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Rate limits and rewrites outgoing chat.
 *
 * The 1.12 original queued [net.minecraft.network.protocol.game.ServerboundChatPacket]s and edited
 * their text in place. On 26.2 the packet carries a signature computed over its content, so editing
 * it would invalidate the signature; the queue holds the raw strings instead and only hands them to
 * [net.minecraft.client.multiplayer.ClientPacketListener.sendChat] once the modifiers have run, so
 * the message is signed after it is final.
 */
object MessageManager : Manager() {
    private val messageQueue = TreeSet<QueuedMessage>().synchronized()
    private val timer = TickTimer()
    private val activeModifiers = TreeSet<MessageModifier>().synchronized()

    var lastPlayerMessage = ""

    /**
     * Set while this manager is handing a message to the connection, so
     * MixinClientPacketListener lets it through instead of re-queueing it.
     */
    @JvmStatic
    @Volatile
    var sending = false
        private set

    init {
        safeConcurrentListener<TickEvent.Post>(true) {
            if (messageQueue.isEmpty()) {
                // Reset the id counter so it can't creep toward the 32 bit limit
                QueuedMessage.idCounter.set(Int.MIN_VALUE)
            } else {
                if (timer.tick(ChatSetting.delay.toLong())) {
                    messageQueue.pollFirst()?.let { queued ->
                        synchronized(activeModifiers) {
                            for (modifier in activeModifiers) {
                                modifier.apply(queued)
                            }
                        }
                        if (queued.message.isNotBlank()) {
                            sendDirect(queued.message)
                            timer.reset()
                        }
                    }
                }

                // Drop the lowest priority messages once the queue is over the limit
                while (messageQueue.size > ChatSetting.maxMessageQueueSize) {
                    messageQueue.pollLast()
                }
            }
        }
    }

    /**
     * Hands [message] straight to the connection, bypassing the queue.
     */
    fun sendDirect(message: String) {
        onMainThread {
            val connection = mc.connection ?: return@onMainThread
            sending = true
            try {
                if (message.startsWith('/')) {
                    connection.sendCommand(message.removePrefix("/"))
                } else {
                    connection.sendChat(message)
                }
            } finally {
                sending = false
            }
        }
    }

    fun addMessageToQueue(message: String, source: Any, priority: Int = 0) {
        messageQueue.add(QueuedMessage(priority, source, message))
    }

    /**
     * Called from MixinClientPacketListener for chat the player typed themselves.
     *
     * @return true if the message was queued and the vanilla send should be cancelled
     */
    @JvmStatic
    fun onPlayerSendChat(content: String): Boolean {
        if (sending) return false

        if (content != lastPlayerMessage) {
            addMessageToQueue(content, this)
        } else {
            addMessageToQueue(content, mc.player ?: this, Int.MAX_VALUE - 1)
        }
        lastPlayerMessage = content

        return true
    }

    class QueuedMessage(
        private val priority: Int,
        val source: Any,
        var message: String
    ) : Comparable<QueuedMessage> {
        private val id = idCounter.getAndIncrement()

        override fun compareTo(other: QueuedMessage): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }

        companion object {
            val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }

    fun AbstractModule.newMessageModifier(
        filter: (QueuedMessage) -> Boolean = { true },
        modifier: (QueuedMessage) -> String
    ) = MessageModifier(modulePriority, filter, modifier)

    class MessageModifier(
        private val priority: Int,
        private val filter: (QueuedMessage) -> Boolean = { true },
        private val modifier: (QueuedMessage) -> String
    ) : Comparable<MessageModifier> {
        private val id = idCounter.getAndIncrement()

        fun enable() {
            activeModifiers.add(this)
        }

        fun disable() {
            activeModifiers.remove(this)
        }

        fun apply(queuedMessage: QueuedMessage) {
            if (filter.invoke(queuedMessage)) {
                queuedMessage.message = modifier.invoke(queuedMessage)
            }
        }

        override fun compareTo(other: MessageModifier): Int {
            val result = -priority.compareTo(other.priority)
            return if (result != 0) result
            else id.compareTo(other.id)
        }

        private companion object {
            val idCounter = AtomicInteger(Int.MIN_VALUE)
        }
    }
}
