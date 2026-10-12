package dev.luna5ama.trollhack.util.accessor

import dev.luna5ama.trollhack.mixins.accessor.AccessorClientCommonPacketListener
import dev.luna5ama.trollhack.mixins.accessor.AccessorClientInput
import dev.luna5ama.trollhack.mixins.accessor.AccessorClientLevel
import dev.luna5ama.trollhack.mixins.accessor.AccessorMinecraft
import dev.luna5ama.trollhack.mixins.accessor.AccessorMultiPlayerGameMode
import dev.luna5ama.trollhack.mixins.accessor.AccessorServerboundMovePlayerPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.ClientInput
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.Vec2

val ClientPacketListener.serverBrand: String?
    get() = (this as AccessorClientCommonPacketListener).`trollhack$getServerBrand`()

fun ClientInput.setMoveVector(x: Float, y: Float) {
    (this as AccessorClientInput).`trollhack$setMoveVector`(Vec2(x, y))
}

fun ClientInput.setMoveVector(vec: Vec2) {
    (this as AccessorClientInput).`trollhack$setMoveVector`(vec)
}

fun MultiPlayerGameMode.syncCurrentPlayItem() {
    (this as AccessorMultiPlayerGameMode).`trollhack$syncCurrentPlayItem`()
}

var MultiPlayerGameMode.blockHitDelay: Int
    get() = (this as AccessorMultiPlayerGameMode).`trollhack$getDestroyDelay`()
    set(value) {
        (this as AccessorMultiPlayerGameMode).`trollhack$setDestroyDelay`(value)
    }

/**
 * Runs [block] inside a block state prediction window, passing the sequence number
 * required by ServerboundUseItemOnPacket / ServerboundPlayerActionPacket.
 */
inline fun <T> ClientLevel.withPrediction(block: (sequence: Int) -> T): T {
    val handler = (this as AccessorClientLevel).`trollhack$getBlockStatePredictionHandler`()
    return handler.startPredicting().use {
        block(it.currentSequence())
    }
}

var Minecraft.rightClickDelay: Int
    get() = (this as AccessorMinecraft).`trollhack$getRightClickDelay`()
    set(value) {
        (this as AccessorMinecraft).`trollhack$setRightClickDelay`(value)
    }

fun Minecraft.startAttack(): Boolean {
    return (this as AccessorMinecraft).`trollhack$startAttack`()
}

fun Minecraft.startUseItem() {
    (this as AccessorMinecraft).`trollhack$startUseItem`()
}

/** 26.2's replacement for 1.12's sendClickBlockToController */
fun Minecraft.continueAttack(down: Boolean) {
    (this as AccessorMinecraft).`trollhack$continueAttack`(down)
}

var ServerboundMovePlayerPacket.onGroundOverride: Boolean
    get() = this.isOnGround
    set(value) {
        (this as AccessorServerboundMovePlayerPacket).`trollhack$setOnGround`(value)
    }

fun ServerboundMovePlayerPacket.setX(value: Double) {
    (this as AccessorServerboundMovePlayerPacket).`trollhack$setX`(value)
}

fun ServerboundMovePlayerPacket.setY(value: Double) {
    (this as AccessorServerboundMovePlayerPacket).`trollhack$setY`(value)
}

fun ServerboundMovePlayerPacket.setZ(value: Double) {
    (this as AccessorServerboundMovePlayerPacket).`trollhack$setZ`(value)
}

fun ServerboundMovePlayerPacket.setYRot(value: Float) {
    (this as AccessorServerboundMovePlayerPacket).`trollhack$setYRot`(value)
}

fun ServerboundMovePlayerPacket.setXRot(value: Float) {
    (this as AccessorServerboundMovePlayerPacket).`trollhack$setXRot`(value)
}
