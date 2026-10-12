package dev.luna5ama.trollhack.graphics.font

import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

/**
 * Facade over the vanilla font system rendering with the bundled Lexend Deca TTF
 * (assets/trollhack/font/lexend.json). Mirrors the old MainFontRenderer API surface
 * used by the gui so components port over with the same metrics calls.
 */
object TrollFont {
    private val FONT_ID = Identifier.fromNamespaceAndPath("trollhack", "lexend")
    private val FONT_DESC = FontDescription.Resource(FONT_ID)

    val style: Style = Style.EMPTY.withFont(FONT_DESC)
    private val boldStyle: Style = style.withBold(true)

    private val font get() = Wrapper.minecraft.font

    fun component(text: CharSequence, bold: Boolean = false): Component =
        Component.literal(text.toString()).withStyle(if (bold) boldStyle else style)

    fun getWidth(text: CharSequence, scale: Float = 1.0f, bold: Boolean = false): Float =
        font.width(component(text, bold)) * scale

    fun getHeight(scale: Float = 1.0f): Float =
        font.lineHeight * scale

    fun drawString(
        graphics: GuiGraphicsExtractor,
        text: CharSequence,
        posX: Float,
        posY: Float,
        color: ColorRGB,
        scale: Float = 1.0f,
        drawShadow: Boolean = false,
        bold: Boolean = false
    ) {
        if (text.isEmpty()) return

        if (scale != 1.0f) {
            graphics.pose().pushMatrix()
            graphics.pose().translate(posX, posY)
            graphics.pose().scale(scale, scale)
            graphics.text(font, component(text, bold), 0, 0, color.toArgb(), drawShadow)
            graphics.pose().popMatrix()
        } else {
            graphics.text(font, component(text, bold), posX.toInt(), posY.toInt(), color.toArgb(), drawShadow)
        }
    }
}
