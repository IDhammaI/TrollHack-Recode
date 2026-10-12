package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.player.InteractEvent
import dev.luna5ama.trollhack.event.events.player.PlayerAttackEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.combat.CombatUtils
import dev.luna5ama.trollhack.util.combat.CombatUtils.equipBestWeapon
import dev.luna5ama.trollhack.util.inventory.equipBestTool
import net.minecraft.world.entity.LivingEntity

internal object AutoTool : Module(
    name = "Auto Tool",
    description = "Automatically switch to the best tools when mining or attacking",
    category = Category.MISC
) {
    private val swapWeapon by setting("Switch Weapon", false)
    private val preferWeapon by setting("Prefer", CombatUtils.PreferWeapon.SWORD)

    init {
        safeListener<InteractEvent.Block.LeftClick> {
            if (player.isCreative) return@safeListener
            val state = world.getBlockState(it.pos)
            if (state.getDestroySpeed(world, it.pos) != -1.0f) {
                equipBestTool(state)
            }
        }

        safeListener<PlayerAttackEvent> {
            if (swapWeapon && it.entity is LivingEntity) {
                equipBestWeapon(preferWeapon)
            }
        }
    }
}
