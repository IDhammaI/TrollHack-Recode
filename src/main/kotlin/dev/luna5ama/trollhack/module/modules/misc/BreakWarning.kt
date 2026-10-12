package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.world.getCollisionBox
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import kotlin.math.abs

internal object BreakWarning : Module(
    name = "Break Warning",
    description = "Sends warning when someone breaking blocks near you",
    category = Category.MISC
) {
    private val ignoreFriends by setting("Ignore Friends", true)
    private val burrow by setting("Burrow", BlockMode.ANY)
    private val surround by setting("Surround", BlockMode.ANY)
    private val nearby by setting("Nearby", BlockMode.OFF)
    private val range by setting("Range", 8, 1..16, 1, { nearby != BlockMode.OFF })

    private enum class BlockMode(override val displayName: CharSequence) : DisplayEnum {
        OFF("Off") {
            override fun checkBlock(block: Block): Boolean {
                return false
            }
        },
        ANY("Any") {
            override fun checkBlock(block: Block): Boolean {
                return true
            }
        },
        OBSIDIAN("Obsidian") {
            override fun checkBlock(block: Block): Boolean {
                return block == Blocks.OBSIDIAN
            }
        };

        abstract fun checkBlock(block: Block): Boolean
    }

    init {
        safeConcurrentListener<PacketEvent.PostReceive> {
            val packet = it.packet
            if (packet !is ClientboundBlockDestructionPacket) return@safeConcurrentListener
            val breaker = getPlayer(packet.id) ?: return@safeConcurrentListener

            if (trySendBurrow(packet, breaker)) return@safeConcurrentListener
            if (trySendSurround(packet, breaker)) return@safeConcurrentListener
            if (trySendNearby(packet, breaker)) return@safeConcurrentListener
        }
    }

    private fun SafeClientEvent.getPlayer(id: Int): Player? {
        if (id == player.id) return null
        val entity = EntityManager.getPlayerByID(id) ?: return null
        if (ignoreFriends && FriendManager.isFriend(entity.gameProfile.name)) return null
        return entity
    }

    private fun SafeClientEvent.trySendBurrow(packet: ClientboundBlockDestructionPacket, breaker: Player): Boolean {
        if (burrow == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.pos)
        if (!burrow.checkBlock(blockState.block)) return false

        val box = world.getCollisionBox(packet.pos) ?: return false
        if (!player.boundingBox.intersects(box.move(packet.pos))) return false

        NoSpamMessage.sendWarning(
            hash(packet, 1),
            "$chatName ${breaker.gameProfile.name} is breaking your burrow!"
        )

        return true
    }

    private fun SafeClientEvent.trySendSurround(packet: ClientboundBlockDestructionPacket, breaker: Player): Boolean {
        if (surround == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.pos)
        if (!surround.checkBlock(blockState.block)) return false

        val holeInfo = HoleManager.getHoleInfo(player)

        if (holeInfo.isHole) {
            if (!holeInfo.surroundPos.contains(packet.pos)) return false
        } else {
            val playerPos = player.betterPosition
            val xDiff = abs(packet.pos.x - playerPos.x)
            val zDiff = abs(packet.pos.z - playerPos.z)
            if (packet.pos.y != playerPos.y || xDiff > 1 || zDiff > 1 || xDiff + zDiff != 1) return false
        }

        NoSpamMessage.sendWarning(
            hash(packet, 2),
            "$chatName ${breaker.gameProfile.name} is breaking your surround!"
        )

        return true
    }

    private fun SafeClientEvent.trySendNearby(packet: ClientboundBlockDestructionPacket, breaker: Player): Boolean {
        if (nearby == BlockMode.OFF) return false

        val blockState = world.getBlockState(packet.pos)
        if (!nearby.checkBlock(blockState.block)) return false

        if (player.distanceSqToCenter(packet.pos) > range * range) return false

        NoSpamMessage.sendWarning(
            hash(packet, 3),
            "$chatName ${breaker.gameProfile.name} is breaking block near you!"
        )

        return true
    }

    private fun hash(packet: ClientboundBlockDestructionPacket, type: Int): Int {
        var result = BreakWarning.hashCode()
        result = 31 * result + packet.id
        result = 31 * result + type
        return result
    }
}
