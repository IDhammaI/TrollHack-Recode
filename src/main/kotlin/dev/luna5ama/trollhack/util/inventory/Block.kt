package dev.luna5ama.trollhack.util.inventory

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

val shulkerList: Set<Block> = HashSet<Block>().apply {
    add(Blocks.SHULKER_BOX)
    // 26.2 groups dyed variants in a ColorCollection instead of 16 constants
    addAll(Blocks.DYED_SHULKER_BOX.asList())
}

val blockBlacklist: Set<Block> = HashSet<Block>().apply {
    add(Blocks.ENDER_CHEST)
    add(Blocks.CHEST)
    add(Blocks.TRAPPED_CHEST)
    add(Blocks.CRAFTING_TABLE)
    add(Blocks.ANVIL)
    add(Blocks.CHIPPED_ANVIL)
    add(Blocks.DAMAGED_ANVIL)
    add(Blocks.BREWING_STAND)
    add(Blocks.HOPPER)
    add(Blocks.DROPPER)
    add(Blocks.DISPENSER)
    add(Blocks.OAK_TRAPDOOR)
    add(Blocks.ENCHANTING_TABLE)
    addAll(shulkerList)
}

val Block.item: Item get() = this.asItem()

val Block.id: Int
    get() = BuiltInRegistries.BLOCK.getId(this)
