package dev.luna5ama.trollhack.util.combat

import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.util.inventory.attackDamage
import dev.luna5ama.trollhack.util.inventory.getEnchantmentLevel
import dev.luna5ama.trollhack.util.inventory.isWeapon
import dev.luna5ama.trollhack.util.inventory.isTool
import dev.luna5ama.trollhack.util.inventory.operation.swapToSlot
import dev.luna5ama.trollhack.util.inventory.slot.filterByStack
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.enchantment.Enchantments
import java.util.*
import kotlin.math.max
import kotlin.math.round

object CombatUtils : AlwaysListening {
    private val cachedArmorValues = WeakHashMap<LivingEntity, ArmorInfo>()

    fun SafeClientEvent.calcDamageFromPlayer(entity: Player, assumeCritical: Boolean = false): Float {
        val itemStack = entity.mainHandItem
        var damage = itemStack.attackDamage
        if (assumeCritical) damage *= 1.5f

        return calcDamage(player, damage)
    }

    fun SafeClientEvent.calcDamageFromMob(entity: Monster): Float {
        val damage = entity.getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
        return calcDamage(player, damage)
    }

    fun calcDamage(
        entity: LivingEntity,
        damageIn: Float = 100f,
        isExplosion: Boolean = false,
        roundDamage: Boolean = false
    ): Float {
        val armorInfo = cachedArmorValues[entity] ?: return 0.0f
        var damage = getDamageAfterAbsorb(damageIn, armorInfo.armorValue, armorInfo.toughness)

        entity.getEffect(MobEffects.RESISTANCE)?.let {
            damage *= max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        }

        damage *= getProtectionModifier(entity, isExplosion)

        return if (roundDamage) round(damage) else damage
    }

    /**
     * 1.12-style armor formula (server implementations on anarchy servers still follow this).
     * The 26.2 CombatRules version requires a live DamageSource + ServerLevel, so re-implement.
     */
    private fun getDamageAfterAbsorb(damage: Float, totalArmor: Float, armorToughness: Float): Float {
        val toughness = 2.0f + armorToughness / 4.0f
        val realArmor = (totalArmor - damage / toughness).coerceIn(totalArmor * 0.2f, 20.0f)
        return damage * (1.0f - realArmor / 25.0f)
    }

    private fun getProtectionModifier(entity: LivingEntity, isExplosion: Boolean): Float {
        var modifier = 0

        for (slot in armorEquipmentSlots) {
            val armor = entity.getItemBySlot(slot)
            if (armor.isEmpty) continue
            modifier += armor.getEnchantmentLevel(Enchantments.PROTECTION)
            if (isExplosion) {
                modifier += armor.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
            }
        }

        modifier = modifier.coerceIn(0, 20)

        return 1.0f - modifier / 25.0f
    }

    private val armorEquipmentSlots = arrayOf(
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD
    )

    fun SafeClientEvent.equipBestWeapon(preferWeapon: PreferWeapon = PreferWeapon.NONE, allowTool: Boolean = false) {
        player.hotbarSlots.filterByStack {
            val item = it.item
            item.components().has(DataComponents.WEAPON) || allowTool && item.isTool
        }.maxByOrNull {
            val itemStack = it.item
            val item = itemStack.item
            val damage = itemStack.attackDamage

            when {
                preferWeapon == PreferWeapon.SWORD && item !is AxeItem && item.isWeapon -> damage * 10.0f
                preferWeapon == PreferWeapon.AXE && item is AxeItem -> damage * 10.0f
                else -> damage
            }
        }?.let {
            swapToSlot(it)
        }
    }

    val LivingEntity.scaledHealth: Float
        get() = this.health + this.absorptionAmount * (this.health / this.maxHealth)

    val LivingEntity.totalHealth: Float
        get() = this.health + this.absorptionAmount

    init {
        safeParallelListener<TickEvent.Post> {
            for (entity in EntityManager.entity) {
                if (entity !is LivingEntity) continue
                val armorValue = entity.armorValue.toFloat()
                val toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS).toFloat()

                cachedArmorValues.getOrPut(entity, ::ArmorInfo).update(armorValue, toughness)
            }
        }

        listener<ConnectionEvent.Disconnect> {
            cachedArmorValues.clear()
        }
    }

    private class ArmorInfo {
        var armorValue = 0.0f; private set
        var toughness = 0.0f; private set

        fun update(armorValue: Float, toughness: Float) {
            this.armorValue = armorValue
            this.toughness = toughness
        }
    }

    enum class PreferWeapon {
        SWORD, AXE, NONE
    }
}
