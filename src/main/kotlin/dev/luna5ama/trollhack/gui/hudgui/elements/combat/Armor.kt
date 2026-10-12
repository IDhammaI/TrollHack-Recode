package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.inventory.duraPercentage
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

internal object Armor : AbstractHudElement(
    name = "Armor",
    category = Category.COMBAT,
    description = "Displays armor durability"
) {
    private val duraPercentage0 by setting("Durability Percentage", true)

    override val hudWidth: Float get() = 4.0f * 18.0f
    override val hudHeight: Float get() = if (duraPercentage0) 28.0f else 18.0f

    private var armor = listOf<ItemStack>()

    init {
        safeParallelListener<TickEvent.Post> {
            armor = armorEquipmentSlots.map { player.getItemBySlot(it) }
        }
    }

    private val armorEquipmentSlots = arrayOf(
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    )

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        super.renderHud(graphics)
        val font = Wrapper.minecraft.font

        var x = 0
        for (stack in armor) {
            if (!stack.isEmpty) {
                graphics.item(stack, x + 1, 1)
                graphics.itemDecorations(font, stack, x + 1, 1)

                if (duraPercentage0 && stack.isDamageableItem) {
                    val percentage = stack.duraPercentage
                    val color = when {
                        percentage > 50 -> GuiColors.text
                        percentage > 20 -> ColorYellow
                        else -> ColorRed
                    }
                    val text = "$percentage%"
                    graphics.text(font, text, x + 9 - font.width(text) / 2, 19, color.toArgb())
                }
            }
            x += 18
        }
    }

    private val ColorYellow = dev.luna5ama.trollhack.graphics.color.ColorRGB(255, 220, 60)
    private val ColorRed = dev.luna5ama.trollhack.graphics.color.ColorRGB(255, 80, 80)
}
