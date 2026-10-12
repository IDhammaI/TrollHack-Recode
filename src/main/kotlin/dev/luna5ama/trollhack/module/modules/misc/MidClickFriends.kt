package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.events.InputEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.BackgroundScope
import kotlinx.coroutines.launch
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

internal object MidClickFriends : Module(
    name = "Mid Click Friends",
    category = Category.MISC,
    description = "Middle click players to friend or unfriend them",
    visible = false
) {
    private val timer = TickTimer()
    private var lastPlayer: RemotePlayer? = null

    init {
        listener<InputEvent.Mouse> {
            // 0 is left, 1 is right, 2 is middle
            if (it.state || it.button != 2) return@listener

            val hitResult = mc.hitResult ?: return@listener
            if (hitResult.type != HitResult.Type.ENTITY) return@listener
            val target = (hitResult as EntityHitResult).entity as? RemotePlayer ?: return@listener

            if (timer.tickAndReset(5000L) || target != lastPlayer && timer.tickAndReset(500L)) {
                val name = target.gameProfile.name
                if (FriendManager.isFriend(name)) remove(name)
                else add(name)
                lastPlayer = target
            }
        }
    }

    private fun remove(name: String) {
        if (FriendManager.removeFriend(name)) {
            NoSpamMessage.sendMessage("§b$name§r has been unfriended.")
        }
    }

    private fun add(name: String) {
        BackgroundScope.launch {
            if (FriendManager.addFriend(name)) NoSpamMessage.sendMessage("§b$name§r has been friended.")
            else NoSpamMessage.sendMessage("Failed to find UUID of $name")
        }
    }
}
