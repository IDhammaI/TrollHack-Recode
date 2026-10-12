package dev.luna5ama.trollhack.gui.hudgui.elements.player

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

internal object InventoryViewer : AbstractHudElement(
    name = "Inventory Viewer",
    category = Category.PLAYER,
    description = "Displays your inventory"
) {
    private val background by setting("Background", true)
    private val border by setting("Border", false)

    override val hudWidth: Float get() = 162.0f
    override val hudHeight: Float get() = 54.0f

    private var stacks = listOf<ItemStack>()

    init {
        safeParallelListener<TickEvent.Post> {
            // Storage slots 9..35 (3 rows of 9)
            stacks = (9..35).map { player.inventory.getItem(it) }
        }
    }

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        super.renderHud(graphics)
        val font = Wrapper.minecraft.font

        if (background) {
            graphics.fill(0, 0, 162, 54, GuiColors.backGround.toArgb())
        }
        if (border) {
            graphics.outline(0, 0, 162, 54, GuiColors.outline.toArgb())
        }

        for ((index, stack) in stacks.withIndex()) {
            if (stack.isEmpty) continue
            val x = (index % 9) * 18 + 1
            val y = (index / 9) * 18 + 1
            graphics.item(stack, x, y)
            graphics.itemDecorations(font, stack, x, y)
        }
    }
}
