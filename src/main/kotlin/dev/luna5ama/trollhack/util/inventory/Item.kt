package dev.luna5ama.trollhack.util.inventory

import dev.fastmc.common.ceilToInt
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

val ItemStack.originalName: String get() = this.hoverName.string

val Item.id get() = BuiltInRegistries.ITEM.getId(this)

val Item.block: Block get() = Block.byItem(this)

val Item.isWeapon get() = this.components().has(DataComponents.WEAPON) || this is AxeItem

val Item.isTool: Boolean
    get() = this.components().has(DataComponents.TOOL)
        || this.components().has(DataComponents.WEAPON)

val ItemStack.foodValue: Int
    get() = this.get(DataComponents.FOOD)?.nutrition ?: 0

val ItemStack.saturation: Float
    get() = this.get(DataComponents.FOOD)?.saturation ?: 0.0f

val ItemStack.attackDamage: Float
    get() = this.baseAttackDamage + this.getEnchantmentLevel(Enchantments.SHARPNESS).let {
        if (it > 0) it * 0.5f + 0.5f
        else 0.0f
    }

val ItemStack.baseAttackDamage: Float
    get() {
        var damage = 1.0
        val modifiers = this.get(DataComponents.ATTRIBUTE_MODIFIERS) ?: return damage.toFloat()

        for (entry in modifiers.modifiers()) {
            if (entry.attribute() == Attributes.ATTACK_DAMAGE) {
                damage += entry.modifier().amount()
            }
        }

        return damage.toFloat()
    }

val Item.regName: String
    get() = BuiltInRegistries.ITEM.getKey(this).toString()

val ItemStack.regName: String
    get() = this.item.regName

val ItemStack.durability: Int
    get() = this.maxDamage - this.damageValue

val ItemStack.duraPercentage: Int
    get() = if (this.maxDamage == 0) 100
    else (this.durability * 100.0f / this.maxDamage.toFloat()).ceilToInt()

fun ItemStack.getEnchantmentLevel(enchantment: ResourceKey<Enchantment>): Int {
    val registry = dev.luna5ama.trollhack.util.Wrapper.minecraft.connection
        ?.registryAccess()
        ?.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
        ?: return 0

    val holder = registry.get(enchantment).orElse(null) ?: return 0
    return EnchantmentHelper.getItemEnchantmentLevel(holder, this)
}

fun LivingEntity.isHolding(hand: InteractionHand, item: Item): Boolean {
    return this.getItemInHand(hand).item == item
}

fun LivingEntity.isHolding(hand: InteractionHand, block: Block): Boolean {
    return this.getItemInHand(hand).item.block == block
}

val Item.isAir: Boolean get() = this == Blocks.AIR.asItem()
