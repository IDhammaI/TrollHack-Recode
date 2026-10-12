package dev.luna5ama.trollhack.manager.managers

import com.google.common.collect.MapMaker
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.Manager
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object PlayerPacketManager : Manager() {
    private val ignoreUpdateSet = Collections.newSetFromMap<ServerboundMovePlayerPacket>(MapMaker().weakKeys().makeMap())
    private val pendingPacket = AtomicReference<Packet?>()

    var position: Vec3 = Vec3.ZERO; private set
    var prevPosition: Vec3 = Vec3.ZERO; private set

    var eyePosition: Vec3 = Vec3.ZERO; private set
    var boundingBox: AABB = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0); private set

    var rotation = Vec2f.ZERO; private set
    var prevRotation = Vec2f.ZERO; private set

    val rotationX: Float
        get() = rotation.x

    val rotationY: Float
        get() = rotation.y

    init {
        listener<PacketEvent.PostSend>(-6969) {
            val packet = it.packet
            if (packet !is ServerboundMovePlayerPacket) return@listener
            if (ignoreUpdateSet.remove(packet)) return@listener

            runSafe {
                if (packet.hasPosition()) {
                    val x = packet.getX(player.x)
                    val y = packet.getY(player.y)
                    val z = packet.getZ(player.z)

                    position = Vec3(x, y, z)
                    eyePosition = Vec3(x, y + player.getEyeHeight(), z)

                    val halfWidth = player.bbWidth / 2.0
                    boundingBox = AABB(
                        x - halfWidth, y, z - halfWidth,
                        x + halfWidth, y + player.bbHeight, z + halfWidth,
                    )
                }

                if (packet.hasRotation()) {
                    rotation = Vec2f(packet.getYRot(player.yRot), packet.getXRot(player.xRot))
                }
            }
        }

        safeListener<TickEvent.Pre>(Int.MAX_VALUE) {
            prevPosition = position
            prevRotation = rotation
        }
    }

    fun ignoreUpdate(packet: ServerboundMovePlayerPacket) {
        ignoreUpdateSet.add(packet)
    }

    fun applyPacket(event: OnUpdateWalkingPlayerEvent.Pre) {
        val packet = pendingPacket.getAndSet(null)
        if (packet != null) {
            event.apply(packet)
        }
    }

    inline fun AbstractModule.sendPlayerPacket(block: Packet.Builder.() -> Unit) {
        sendPlayerPacket(this.modulePriority, block)
    }

    inline fun sendPlayerPacket(priority: Int, block: Packet.Builder.() -> Unit) {
        Packet.Builder(priority).apply(block).build()?.let {
            sendPlayerPacket(it)
        }
    }

    fun sendPlayerPacket(packet: Packet) {
        pendingPacket.updateAndGet {
            if (it == null || it.priority < packet.priority) {
                packet
            } else {
                it
            }
        }
    }

    class Packet private constructor(
        val priority: Int,
        val position: Vec3?,
        val rotation: Vec2f?,
        val onGround: Boolean?,
        val cancelMove: Boolean,
        val cancelRotate: Boolean,
        val cancelAll: Boolean
    ) {
        class Builder(private val priority: Int) {
            private var position: Vec3? = null
            private var rotation: Vec2f? = null
            private var onGround: Boolean? = null

            private var cancelMove = false
            private var cancelRotate = false
            private var cancelAll = false
            private var empty = true

            fun onGround(onGround: Boolean) {
                this.onGround = onGround
                this.empty = false
            }

            fun move(position: Vec3) {
                this.position = position
                this.cancelMove = false
                this.empty = false
            }

            fun rotate(rotation: Vec2f) {
                this.rotation = rotation
                this.cancelRotate = false
                this.empty = false
            }

            fun cancelAll() {
                this.cancelMove = true
                this.cancelRotate = true
                this.cancelAll = true
                this.empty = false
            }

            fun cancelMove() {
                this.position = null
                this.cancelMove = true
                this.empty = false
            }

            fun cancelRotate() {
                this.rotation = null
                this.cancelRotate = true
                this.empty = false
            }

            fun build() =
                if (!empty) Packet(
                    priority,
                    position,
                    rotation,
                    onGround,
                    cancelMove,
                    cancelRotate,
                    cancelAll
                ) else null
        }
    }
}
