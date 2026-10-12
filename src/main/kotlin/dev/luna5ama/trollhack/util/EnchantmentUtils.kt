package dev.luna5ama.trollhack.util

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.enchantment.ItemEnchantments
import kotlin.math.abs

object EnchantmentUtils {

    fun getAllEnchantments(itemStack: ItemStack): List<LeveledEnchantment> {
        val component = if (itemStack.item == Items.ENCHANTED_BOOK) {
            itemStack.get(DataComponents.STORED_ENCHANTMENTS)
        } else {
            itemStack.get(DataComponents.ENCHANTMENTS)
        } ?: ItemEnchantments.EMPTY

        if (component.isEmpty) return emptyList()

        val enchantmentList = ArrayList<LeveledEnchantment>(component.size())
        for (entry in component.entrySet()) {
            enchantmentList.add(LeveledEnchantment(entry.key, entry.intValue.toShort()))
        }
        return enchantmentList
    }

    fun getEnchantmentAlias(enchantment: Holder<Enchantment>): String {
        return getEnumEnchantment(enchantment)?.alias ?: "Null"
    }

    fun getEnumEnchantment(enchantment: Holder<Enchantment>): EnumEnchantments? {
        return EnumEnchantments.entries.firstOrNull { enchantment.`is`(it.key) }
    }

    class LeveledEnchantment(val enchantment: Holder<Enchantment>, val level: Short) {
        val isSingleLevel = enchantment.value().maxLevel == 1
        val isMax = level >= enchantment.value().maxLevel
        val is32K = abs(level.toInt()) >= 32000
        val alias = getEnchantmentAlias(enchantment)
        val levelText = if (isSingleLevel) "" else if (is32K) "32K" else if (isMax) "MAX" else "$level"
    }

    enum class EnumEnchantments(val key: ResourceKey<Enchantment>, val alias: String) {
        PROTECTION(Enchantments.PROTECTION, "PRO"),
        FIRE_PROTECTION(Enchantments.FIRE_PROTECTION, "FRP"),
        FEATHER_FALLING(Enchantments.FEATHER_FALLING, "FEA"),
        BLAST_PROTECTION(Enchantments.BLAST_PROTECTION, "BLA"),
        PROJECTILE_PROTECTION(Enchantments.PROJECTILE_PROTECTION, "PJP"),
        RESPIRATION(Enchantments.RESPIRATION, "RES"),
        AQUA_AFFINITY(Enchantments.AQUA_AFFINITY, "AQU"),
        THORNS(Enchantments.THORNS, "THR"),
        DEPTH_STRIDER(Enchantments.DEPTH_STRIDER, "DEP"),
        FROST_WALKER(Enchantments.FROST_WALKER, "FRO"),
        BINDING_CURSE(Enchantments.BINDING_CURSE, "BIN"),
        SHARPNESS(Enchantments.SHARPNESS, "SHA"),
        SMITE(Enchantments.SMITE, "SMI"),
        BANE_OF_ARTHROPODS(Enchantments.BANE_OF_ARTHROPODS, "BAN"),
        KNOCKBACK(Enchantments.KNOCKBACK, "KNB"),
        FIRE_ASPECT(Enchantments.FIRE_ASPECT, "FIA"),
        LOOTING(Enchantments.LOOTING, "LOO"),
        SWEEPING(Enchantments.SWEEPING_EDGE, "SWE"),
        EFFICIENCY(Enchantments.EFFICIENCY, "EFF"),
        SILK_TOUCH(Enchantments.SILK_TOUCH, "SIL"),
        UNBREAKING(Enchantments.UNBREAKING, "UNB"),
        FORTUNE(Enchantments.FORTUNE, "FOT"),
        POWER(Enchantments.POWER, "POW"),
        PUNCH(Enchantments.PUNCH, "PUN"),
        FLAME(Enchantments.FLAME, "FLA"),
        INFINITY(Enchantments.INFINITY, "INF"),
        LUCK_OF_THE_SEA(Enchantments.LUCK_OF_THE_SEA, "LUC"),
        LURE(Enchantments.LURE, "LUR"),
        MENDING(Enchantments.MENDING, "MEN"),
        VANISHING_CURSE(Enchantments.VANISHING_CURSE, "VAN")
    }
}
