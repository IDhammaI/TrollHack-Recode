package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.EntityUtils.isHostile
import dev.luna5ama.trollhack.util.EntityUtils.isNeutral
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import net.minecraft.world.entity.item.ItemEntity
import java.util.TreeMap

internal object EntityList : LabelHud(
    name = "Entity List",
    category = Category.WORLD,
    description = "Displays nearby entities"
) {
    private val items by setting("Items", true)
    private val passiveMobs by setting("Passive Mobs", true)
    private val neutralMobs by setting("Neutral Mobs", true)
    private val hostileMobs by setting("Hostile Mobs", true)
    private val maxEntries by setting("Max Entries", 8, 4..32, 1)
    private val range by setting("Range", 64, 16..256, 8)

    override fun SafeClientEvent.updateText() {
        val rangeSq = range.toDouble() * range.toDouble()
        val counts = TreeMap<String, Int>()

        for (entity in EntityManager.entity) {
            if (player.distanceSqTo(entity) > rangeSq) continue

            val valid = when {
                entity is ItemEntity -> items
                entity.isPassive -> passiveMobs
                entity.isNeutral -> neutralMobs
                entity.isHostile -> hostileMobs
                else -> false
            }
            if (!valid) continue

            val name = entity.type.description.string
            counts.merge(name, 1, Int::plus)
        }

        var shown = 0
        for ((name, count) in counts) {
            if (shown >= maxEntries) {
                displayText.addLine("...and ${counts.size - shown} more", GuiColors.primary)
                break
            }
            displayText.add(name, GuiColors.text)
            displayText.addLine("x$count", GuiColors.primary)
            shown++
        }
    }
}
