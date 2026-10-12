package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.accessor.rightClickDelay
import dev.luna5ama.trollhack.util.atFalse
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.or
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.EndCrystalItem
import net.minecraft.world.item.ExperienceBottleItem
import net.minecraft.world.item.FireworkRocketItem
import net.minecraft.world.item.Items

internal object FastUse : Module(
    name = "Fast Use",
    category = Category.PLAYER,
    description = "Use items faster"
) {
    val multiUse by setting("Multi Use", 1, 1..10, 1)
    val delay by setting("Delay", 0, 0..10, 1)
    private val blocks = setting("Blocks", false)
    private val allItems = setting("All Items", false)
    private val expBottles = setting("Exp Bottles", true, allItems.atFalse())
    private val endCrystals = setting("End Crystals", true, allItems.atFalse())
    private val fireworks = setting("Fireworks", false, allItems.atFalse())
    private val bow = setting("Bow", true, allItems.atFalse())
    private val chargeSetting = setting("Bow Charge", 3, 0..20, 1, allItems.atTrue() or bow.atTrue())
    private val chargeVariation = setting("Charge Variation", 5, 0..20, 1, allItems.atTrue() or bow.atTrue())

    private var lastUsedHand = InteractionHand.MAIN_HAND
    private var randomVariation = 0

    val bowCharge get() = if (isEnabled && (allItems.value || bow.value)) 72000.0 - (chargeSetting.value.toDouble() + chargeVariation.value / 2.0) else null

    init {
        safeListener<TickEvent.Post> {
            if (player.isSpectator) return@safeListener

            if ((allItems.value || bow.value)
                && player.isUsingItem
                && player.useItem.item == Items.BOW
                && player.getTicksUsingItem() >= getBowCharge()
            ) {
                randomVariation = 0
                playerController.releaseUsingItem(player)
            }
        }

        listener<PacketEvent.PostSend> {
            if (it.packet is ServerboundUseItemPacket) lastUsedHand = it.packet.hand
            else if (it.packet is ServerboundUseItemOnPacket) lastUsedHand = it.packet.hand
        }
    }

    private fun getBowCharge(): Int {
        if (randomVariation == 0) {
            randomVariation = if (chargeVariation.value == 0) 0 else (0..chargeVariation.value).random()
        }
        return chargeSetting.value + randomVariation
    }

    @JvmStatic
    fun updateRightClickDelay() {
        if (isEnabled) {
            runSafe {
                val itemStack = player.getItemInHand(lastUsedHand)
                if (itemStack.isEmpty) return@runSafe
                val item = itemStack.item
                if (allItems.value && item !is BlockItem
                    || blocks.value && item is BlockItem
                    || expBottles.value && item is ExperienceBottleItem
                    || endCrystals.value && item is EndCrystalItem
                    || fireworks.value && item is FireworkRocketItem
                ) {
                    mc.rightClickDelay = delay
                }
            }
        }
    }
}
