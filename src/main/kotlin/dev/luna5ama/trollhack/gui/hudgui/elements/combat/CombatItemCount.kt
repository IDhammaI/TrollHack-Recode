package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.hudgui.HudText
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.inventory.ItemStackPredicate
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.threads.runSafeOrElse
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

/**
 * 1:1 port of the 1.12 Combat Item Count HUD element.
 *
 * Deviations forced by 26.2 / by this project's GUI stack, everything else (setting names,
 * defaults, ordering, the 20px cell pitch, the count+1 trick) is unchanged:
 *
 * - The original extended [dev.luna5ama.trollhack.gui.hudgui.LabelHud] and overrode
 *   `hudWidth`/`hudHeight`. In this port LabelHud declares both `final override`, so this element
 *   extends [AbstractHudElement] and re-implements LabelHud's three lines locally (a [HudText]
 *   rebuilt on `TickEvent.Post` and drawn with the element's `dockingH`). Behaviour is identical;
 *   the text-mode size uses the ported LabelHud's `width + 4` / `height + 2` padding so this
 *   element measures the same as every other ported label HUD (the 1.12 numbers were
 *   `getWidth()` / `getHeight(2)` against a different font renderer).
 * - 1.12 `Items.BED` was a single item with 16 metadata colours. 26.2 registers 16 separate bed
 *   items behind `Items.BED` (a `ColorCollection<Item>`), so the predicate matches every one of
 *   them - the same set the metadata-agnostic 1.12 predicate matched - and the icon uses the white
 *   bed, which is what metadata 0 in `ItemStack(Items.BED, -1)` rendered.
 * - 1.12 `Items.GOLDEN_APPLE` covered both the normal and the notch apple via metadata. 26.2 splits
 *   them into `GOLDEN_APPLE` / `ENCHANTED_GOLDEN_APPLE`, so both are counted and the icon uses
 *   `ENCHANTED_GOLDEN_APPLE`, matching the original's `ItemStack(Items.GOLDEN_APPLE, -1, 1)`.
 * - `player.dimension != 0` becomes `world.dimension() != Level.OVERWORLD`.
 * - `RenderUtils2D.drawItem(stack, x, y, text)` becomes `graphics.item` +
 *   `graphics.itemDecorations(font, stack, x, y, countText)`, which is exactly what
 *   `renderItemAndEffectIntoGUI` + `renderItemOverlayIntoGUI` did (same +19-2-width / +6+3 count
 *   offsets, same durability bar and cooldown overlay).
 * - `GlStateManager.pushMatrix`/`translate` becomes the JOML `graphics.pose()` stack (2-arg
 *   translate).
 */
internal object CombatItemCount : AbstractHudElement(
    name = "Combat Item Count",
    category = Category.COMBAT,
    description = "Counts combat items like gapples, crystal, etc"
) {
    /** All 16 bed items; 1.12 matched them all with a single metadata-agnostic `byItem(Items.BED)`. */
    private val bedItems: Set<Item> = Items.BED.asList().toHashSet()

    private val itemSettings = arrayOf(
        itemSetting("Arrow", ItemStackPredicate.byItem(Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW)),
        itemSetting("Bed", ItemStackPredicate { it.item in bedItems }) { world.dimension() != Level.OVERWORLD },
        itemSetting("Crystal", ItemStackPredicate.byItem(Items.END_CRYSTAL)),
        itemSetting("Gapple", ItemStackPredicate.byItem(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)),
        itemSetting("Totem", ItemStackPredicate.byItem(Items.TOTEM_OF_UNDYING)),
        itemSetting("Xp Bottle", ItemStackPredicate.byItem(Items.EXPERIENCE_BOTTLE)),
        itemSetting("Pearl", ItemStackPredicate.byItem(Items.ENDER_PEARL)),
        itemSetting("Chorus Fruit", ItemStackPredicate.byItem(Items.CHORUS_FRUIT))
    )

    private val showIcon by setting("Show Icon", true)
    private val horizontal by setting("Horizontal", true, { showIcon })

    /**
     * Built lazily: constructing an ItemStack reads the item's data components, which are not
     * bound yet while HUD elements are registered from the client entrypoint (that runs inside
     * Minecraft's own constructor). Touching them there crashes with "Components not bound yet".
     */
    private val itemStacks: Array<ItemStack> by lazy {
        arrayOf(
            ItemStack(Items.ARROW, -1),
            ItemStack(Items.BED.pick(DyeColor.WHITE), -1),
            ItemStack(Items.END_CRYSTAL, -1),
            ItemStack(Items.ENCHANTED_GOLDEN_APPLE, -1),
            ItemStack(Items.TOTEM_OF_UNDYING, -1),
            ItemStack(Items.EXPERIENCE_BOTTLE, -1),
            ItemStack(Items.ENDER_PEARL, -1),
            ItemStack(Items.CHORUS_FRUIT, -1)
        )
    }

    private val displayText = HudText()

    override val hudWidth: Float by FrameFloat {
        if (showIcon) {
            if (horizontal) 20.0f * itemSettings.count { it.isEnabled() }
            else 20.0f
        } else {
            displayText.width + 4.0f
        }
    }

    override val hudHeight: Float by FrameFloat {
        if (showIcon) {
            if (horizontal) 20.0f
            else 20.0f * itemSettings.count { it.isEnabled() }
        } else {
            displayText.height + 2.0f
        }
    }

    init {
        safeParallelListener<TickEvent.Post> {
            displayText.clear()
            updateText()
            displayText.commit()
        }
    }

    private fun SafeClientEvent.updateText() {
        val slots = player.allSlots

        for ((index, entry) in itemSettings.withIndex()) {
            val count = if (entry.isEnabled()) {
                slots.asSequence()
                    .filter { entry.predicate(it.item) }
                    .sumOf { it.item.count }
            } else {
                -1
            }

            if (showIcon) {
                itemStacks[index].count = count + 1 // Weird way to get around Minecraft item count check
            } else if (count > -1) {
                displayText.add(entry.name, GuiColors.text)
                displayText.addLine("x$count", GuiColors.primary)
            }
        }
    }

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        super.renderHud(graphics)

        if (showIcon) {
            val font = Wrapper.minecraft.font
            graphics.pose().pushMatrix()

            for (itemStack in itemStacks) {
                if (itemStack.count == 0) continue
                graphics.item(itemStack, 2, 2)
                graphics.itemDecorations(font, itemStack, 2, 2, (itemStack.count - 1).toString())
                if (horizontal) graphics.pose().translate(20.0f, 0.0f)
                else graphics.pose().translate(0.0f, 20.0f)
            }

            graphics.pose().popMatrix()
        } else {
            displayText.draw(graphics, dockingH)
        }
    }

    /**
     * The 1.12 original registered the per item boolean from inside the nested [ItemSetting] class,
     * relying on the enclosing object's `SettingRegister` receiver. Here `setting` is an imported
     * member extension on [dev.luna5ama.trollhack.gui.rgui.Component], so it is registered from the
     * object itself and handed to the holder - same name, same `true` default, same order.
     */
    private fun itemSetting(
        name: String,
        predicate: ItemStackPredicate,
        enabled: (SafeClientEvent.() -> Boolean)? = null
    ) = ItemSetting(name, predicate, setting(name, true), enabled)

    private class ItemSetting(
        val name: String,
        val predicate: ItemStackPredicate,
        private val setting: BooleanSetting,
        private val enabled: (SafeClientEvent.() -> Boolean)? = null
    ) {
        fun isEnabled(): Boolean {
            return runSafeOrElse(false) { setting.value && (enabled?.invoke(this) ?: true) }
        }
    }
}
