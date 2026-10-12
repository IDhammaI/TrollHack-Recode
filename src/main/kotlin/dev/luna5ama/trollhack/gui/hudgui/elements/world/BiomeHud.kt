package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition

internal object BiomeHud : LabelHud(
    name = "Biome",
    category = Category.WORLD,
    description = "Displays current biome"
) {
    override fun SafeClientEvent.updateText() {
        val key = world.getBiome(player.flooredPosition).unwrapKey().orElse(null)
        val name = key?.identifier()?.path
            ?.split('_')
            ?.joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            ?: "Unknown"

        displayText.add(name, GuiColors.text)
    }
}
