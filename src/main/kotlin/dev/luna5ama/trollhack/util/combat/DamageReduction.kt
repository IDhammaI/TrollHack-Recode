package dev.luna5ama.trollhack.util.combat

import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.util.inventory.getEnchantmentLevel
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.math.max
import kotlin.math.min

class DamageReduction(val entity: LivingEntity) {
    private val armorValue = entity.armorValue.toFloat()
    private val toughness = entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS).toFloat()
    private val resistanceMultiplier: Float
    private val genericMultiplier: Float
    private val blastMultiplier: Float

    init {
        var genericEPF = 0
        var blastEPF = 0

        for (slot in armorEquipmentSlots) {
            val itemStack = entity.getItemBySlot(slot)
            if (itemStack.isEmpty) continue
            genericEPF += itemStack.getEnchantmentLevel(Enchantments.PROTECTION)
            blastEPF += itemStack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION) * 2
        }

        resistanceMultiplier = entity.getEffect(MobEffects.RESISTANCE)?.let {
            max(1.0f - (it.amplifier + 1) * 0.2f, 0.0f)
        } ?: run {
            if (CombatSetting.assumeResistance) {
                0.8f
            } else {
                1.0f
            }
        }

        genericMultiplier = (1.0f - min(genericEPF, 20) / 25.0f)
        blastMultiplier = (1.0f - min(genericEPF + blastEPF, 20) / 25.0f)
    }

    fun calcDamage(damage: Float, isExplosion: Boolean): Float {
        val toughnessFactor = 2.0f + toughness / 4.0f
        val realArmor = (armorValue - damage / toughnessFactor).coerceIn(armorValue * 0.2f, 20.0f)
        return damage * (1.0f - realArmor / 25.0f) *
            resistanceMultiplier *
            if (isExplosion) blastMultiplier
            else genericMultiplier
    }

    private companion object {
        val armorEquipmentSlots = arrayOf(
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
        )
    }
}
