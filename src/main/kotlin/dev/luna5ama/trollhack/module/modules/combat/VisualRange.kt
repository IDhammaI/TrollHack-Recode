package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.player.Player

internal object VisualRange : Module(
    name = "Visual Range",
    description = "Shows players who enter and leave range in chat",
    category = Category.COMBAT
) {
    private const val NAME_FORMAT = "\$NAME"

    private val playSound by setting("Play Sound", false)
    private val leaving0 = setting("Count Leaving", false)
    private val leaving by leaving0
    private val friends by setting("Friends", true)
    private val uwuAura by setting("UwU Aura", false)
    private val logToFile by setting("Log To File", false)
    private val enterMessage by setting("Enter Message", "$NAME_FORMAT spotted!")
    private val leaveMessage by setting("Leave Message", "$NAME_FORMAT left!", leaving0.atTrue())

    private val playerSet = LinkedHashSet<Player>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        onDisable {
            playerSet.clear()
        }

        listener<WorldEvent.Unload> {
            playerSet.clear()
        }

        listener<WorldEvent.Entity.Remove> {
            if (it.entity !is Player) return@listener
            if (playerSet.remove(it.entity)) {
                onLeave(it.entity)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!timer.tickAndReset(1L)) return@safeParallelListener

            for (entityPlayer in world.players()) {
                if (entityPlayer.isFakeOrSelf) continue // Self / Freecam / FakePlayer check
                if (!friends && FriendManager.isFriend(entityPlayer.gameProfile.name)) continue // Friend check

                if (playerSet.add(entityPlayer)) {
                    onEnter(entityPlayer)
                }
            }
        }
    }

    private fun onEnter(player: Player) {
        val message = enterMessage.replaceName(player)

        sendNotification(player, message)
        if (logToFile) WaypointManager.add(player.flooredPosition, message)
        if (uwuAura) sendServerMessage("/w ${player.gameProfile.name} hi uwu")
    }

    private fun onLeave(player: Player) {
        if (!leaving) return
        val message = leaveMessage.replaceName(player)

        sendNotification(player, message)
        if (logToFile) WaypointManager.add(player.flooredPosition, message)
        if (uwuAura) sendServerMessage("/w ${player.gameProfile.name} bye uwu")
    }

    private fun String.replaceName(player: Player) =
        replace(NAME_FORMAT, getColor(player) format player.gameProfile.name)

    private fun getColor(player: Player) =
        if (FriendManager.isFriend(player.gameProfile.name)) ChatFormatting.GREEN
        else ChatFormatting.RED

    private fun sendNotification(player: Player, message: String) {
        if (playSound) mc.soundManager.play(
            SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
        )
        NoSpamMessage.sendMessage(VisualRange.hashCode() xor player.gameProfile.name.hashCode(), message)
    }
}
