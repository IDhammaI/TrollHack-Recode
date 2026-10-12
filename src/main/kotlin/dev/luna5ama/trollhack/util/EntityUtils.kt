package dev.luna5ama.trollhack.util

import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.math.vector.toBlockPos
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ambient.AmbientCreature
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.animal.squid.Squid
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.phys.Vec3
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object EntityUtils {
    private val mc get() = Minecraft.getInstance()

    val SafeClientEvent.viewEntity: Entity get() = mc.cameraEntity ?: player

    val viewEntity: Entity? get() = mc.cameraEntity ?: mc.player

    val Entity.eyePosition: Vec3 get() = Vec3(this.x, this.y + this.eyeHeight, this.z)
    val Entity.lastTickPos: Vec3 get() = Vec3(this.xOld, this.yOld, this.zOld)
    val Entity.flooredPosition get() = BlockPos(this.x.floorToInt(), this.y.floorToInt(), this.z.floorToInt())
    val Entity.betterPosition
        get() = BlockPos(
            this.x.floorToInt(),
            (this.y + 0.25).floorToInt(),
            this.z.floorToInt()
        )

    val Entity.isPassive
        get() = this is AgeableMob
            || this is AmbientCreature
            || this is Squid

    val Entity.isNeutral get() = isNeutralMob(this) && !isMobAggressive(this)

    val Entity.isHostile get() = isMobAggressive(this)

    val Entity.isTamed
        get() = this is TamableAnimal && this.isTame || this is AbstractHorse && this.isTamed

    val Entity.isInOrAboveLiquid
        get() = this.isInWater || this.isInLava || level().containsAnyLiquid(
            boundingBox.expandTowards(
                0.0,
                -1.0,
                0.0
            )
        )

    val Player.isFriend get() = FriendManager.isFriend(this.name.string)

    val Player.isSelf get() = this == mc.player || this == mc.cameraEntity

    val Player.isFakeOrSelf get() = this == mc.player || this == mc.cameraEntity || this.id < 0

    val Player.isFlying: Boolean
        get() = this.isFallFlying || this.abilities.flying

    private fun isNeutralMob(entity: Entity) = entity is ZombifiedPiglin
        || entity is Wolf
        || entity is EnderMan
        || entity is IronGolem

    private fun isMobAggressive(entity: Entity) = when (entity) {
        is ZombifiedPiglin -> {
            entity.isAggressive || entity.isAngry
        }
        is Wolf -> {
            entity.isAngry && mc.player != entity.owner
        }
        is EnderMan -> {
            entity.isCreepy
        }
        is IronGolem -> {
            entity.lastHurtByMob != null
        }
        else -> {
            entity.type.category == MobCategory.MONSTER
        }
    }

    fun mobTypeSettings(entity: Entity, mobs: Boolean, passive: Boolean, neutral: Boolean, hostile: Boolean): Boolean {
        return mobs && (passive && entity.isPassive || neutral && entity.isNeutral || hostile && entity.isHostile)
    }

    /**
     * Find the entities interpolated position
     */
    fun getInterpolatedPos(entity: Entity, ticks: Float): Vec3 =
        entity.lastTickPos.add(getInterpolatedAmount(entity, ticks))

    /**
     * Find the entities interpolated amount
     */
    fun getInterpolatedAmount(entity: Entity, ticks: Float): Vec3 =
        entity.position().subtract(entity.lastTickPos).scale(ticks.toDouble())

    fun getTargetList(
        player: Array<Boolean>,
        mobs: Array<Boolean>,
        invisible: Boolean,
        range: Float,
        ignoreSelf: Boolean = true
    ): ArrayList<LivingEntity> {
        val entityList = ArrayList<LivingEntity>()
        val selfPlayer = mc.player ?: return entityList

        for (entity in EntityManager.livingBase) {
            /* Entity type check */
            if (ignoreSelf && entity == selfPlayer) continue
            if (entity == mc.cameraEntity) continue
            if (entity is Player) {
                if (!player[0]) continue
                if (!playerTypeCheck(entity, player[1], player[2])) continue
            } else if (!mobTypeSettings(entity, mobs[0], mobs[1], mobs[2], mobs[3])) continue

            if (selfPlayer.isPassenger && entity == selfPlayer.vehicle) continue // Riding entity check
            if (selfPlayer.distanceTo(entity) > range) continue // Distance check
            if (entity.health <= 0) continue // HP check
            if (!invisible && entity.isInvisible) continue
            entityList.add(entity)
        }
        return entityList
    }

    fun playerTypeCheck(player: Player, friend: Boolean, sleeping: Boolean) =
        (friend || !FriendManager.isFriend(player.name.string))
            && (sleeping || !player.isSleeping)

    fun SafeClientEvent.getDroppedItems(item: Item, range: Float): ArrayList<ItemEntity> {
        val entityList = ArrayList<ItemEntity>()
        for (entity in EntityManager.entity) {
            if (entity !is ItemEntity) continue /* Entities that are dropped item */
            if (entity.item.item != item) continue /* Dropped items that has given item id */
            if (entity.distanceTo(player) > range) continue /* Entities within specified blocks radius */

            entityList.add(entity)
        }
        return entityList
    }

    fun SafeClientEvent.getDroppedItem(item: Item, range: Float) =
        getDroppedItems(item, range)
            .minByOrNull { player.distanceTo(it) }
            ?.position()
            ?.toBlockPos()

    /**
     * 26.2 sneak state is communicated with input packets, there are no
     * dedicated sneak entity action packets anymore.
     */
    inline fun LocalPlayer.spoofSneak(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val current = this.input.keyPresses
        if (!current.shift()) {
            connection.send(ServerboundPlayerInputPacket(current.withShift(true)))
            block.invoke()
            connection.send(ServerboundPlayerInputPacket(current))
        } else {
            block.invoke()
        }
    }

    inline fun LocalPlayer.spoofUnSneak(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        val current = this.input.keyPresses
        if (current.shift()) {
            connection.send(ServerboundPlayerInputPacket(current.withShift(false)))
            block.invoke()
            connection.send(ServerboundPlayerInputPacket(current))
        } else {
            block.invoke()
        }
    }

    fun net.minecraft.world.entity.player.Input.withShift(shift: Boolean): net.minecraft.world.entity.player.Input {
        return net.minecraft.world.entity.player.Input(
            forward(), backward(), left(), right(), jump(), shift, sprint()
        )
    }
}
