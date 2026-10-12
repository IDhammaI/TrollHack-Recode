package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.core.BlockPos

internal object TeleportLogger : Module(
    name = "Teleport Logger",
    category = Category.MISC,
    description = "Logs when a player teleports somewhere"
) {
    private val saveToWaypoints = setting("Save To Waypoints", true)
    private val removeInRange = setting("Remove In Range", true)
    private val printAdd = setting("Print Add", true)
    private val printRemove = setting("Print Remove", true, removeInRange.atTrue())
    private val minimumDistance = setting("Minimum Distance", 512, 128..2048, 128)

    private val teleportedPlayers = HashMap<String, BlockPos>()

    init {
        safeParallelListener<TickEvent.Post> {
            for (otherPlayer in world.players()) {
                if (otherPlayer.isFakeOrSelf) continue

                val name = otherPlayer.gameProfile.name
                val distance = player.distanceTo(otherPlayer)

                /* 8 chunk render distance * 16 */
                if (removeInRange.value && distance < 128) {
                    teleportedPlayers.remove(name)?.let {
                        val removed = WaypointManager.remove(it)

                        if (printRemove.value) {
                            if (removed) {
                                NoSpamMessage.sendMessage(
                                    "$chatName Removed $name, they are now ${MathUtils.round(distance, 1)} blocks away"
                                )
                            } else {
                                NoSpamMessage.sendError("$chatName Error removing $name from coords, their position wasn't saved anymore")
                            }
                        }
                    }

                    continue
                }

                if (distance < minimumDistance.value || teleportedPlayers.containsKey(name)) {
                    continue
                }

                val coords = logCoordinates(otherPlayer.flooredPosition, "$name Teleport Spot")
                teleportedPlayers[name] = coords
                if (printAdd.value) {
                    NoSpamMessage.sendMessage("$chatName $name teleported, ${getSaveText()} ${coords.x}, ${coords.y}, ${coords.z}")
                }
            }
        }
    }

    private fun logCoordinates(coordinate: BlockPos, name: String): BlockPos {
        return if (saveToWaypoints.value) WaypointManager.add(coordinate, name).pos
        else coordinate
    }

    private fun getSaveText(): String {
        return if (saveToWaypoints.value) "saved their coordinates at"
        else "their coordinates are"
    }
}
