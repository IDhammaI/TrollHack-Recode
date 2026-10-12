package dev.luna5ama.trollhack.gui.hudgui

import dev.luna5ama.trollhack.graphics.HAlign
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.gui.GuiColors
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Vanilla-font replacement for the original TextComponent used by label HUD elements.
 * Built off-thread in updateText(); width measurement happens on the render thread
 * because vanilla font measuring can bake glyph textures (one frame of lag on change).
 */
class HudText {
    private class Part(val text: String, val color: ColorRGB)

    private val building = ArrayList<ArrayList<Part>>()

    @Volatile
    private var lines: List<List<Part>> = emptyList()

    // Measured on the render thread during draw
    @Volatile
    var width = 0f; private set

    @Volatile
    var height = 0f; private set

    val isEmpty get() = lines.isEmpty()

    fun clear() {
        building.clear()
    }

    fun add(text: CharSequence, color: ColorRGB = GuiColors.text) {
        if (building.isEmpty()) building.add(ArrayList())
        building.last().add(Part(text.toString(), color))
    }

    fun addLine(text: CharSequence, color: ColorRGB = GuiColors.text) {
        add(text, color)
        building.add(ArrayList())
    }

    fun newLine() {
        building.add(ArrayList())
    }

    fun commit() {
        lines = building
            .filter { it.isNotEmpty() }
            .map { it.toList() }
    }

    fun draw(graphics: GuiGraphicsExtractor, align: HAlign) {
        val lineHeight = TrollFont.getHeight().toInt()
        val snapshot = lines

        // Measure on the render thread (glyphs may get baked here)
        var maxWidth = 0
        val lineWidths = IntArray(snapshot.size)
        for ((index, line) in snapshot.withIndex()) {
            var lineWidth = 0
            for (part in line) {
                lineWidth += TrollFont.getWidth(part.text).toInt() + if (part === line.last()) 0 else SPACE_WIDTH
            }
            lineWidths[index] = lineWidth
            if (lineWidth > maxWidth) maxWidth = lineWidth
        }

        width = maxWidth.toFloat()
        height = (snapshot.size * (lineHeight + 1)).toFloat()

        var y = 1
        for ((index, line) in snapshot.withIndex()) {
            var x = when (align) {
                HAlign.LEFT -> 2
                HAlign.CENTER -> (maxWidth - lineWidths[index]) / 2 + 2
                HAlign.RIGHT -> maxWidth - lineWidths[index] + 2
            }

            for (part in line) {
                TrollFont.drawString(
                    graphics, part.text, x.toFloat(), y.toFloat(), part.color, drawShadow = true
                )
                x += TrollFont.getWidth(part.text).toInt() + SPACE_WIDTH
            }

            y += lineHeight + 1
        }
    }

    private companion object {
        const val SPACE_WIDTH = 4
    }
}
