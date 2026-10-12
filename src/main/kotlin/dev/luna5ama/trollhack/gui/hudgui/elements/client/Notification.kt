package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.event.events.ModuleToggleEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.HAlign
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.gui.hudgui.TrollHudGui
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.text.format
import it.unimi.dsi.fastutil.HashCommon
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Client notification queue.
 *
 * Setting names, defaults, ranges, colours, layout numbers and the three stage animation
 * (300ms slide in -> 200ms fade in -> hold -> 200ms fade out -> 300ms slide out, all OUT_CUBIC)
 * are a 1:1 port of the 1.12 element.
 *
 * 26.2 deviations:
 * - The original drew the queue from a `MixinEntityRenderer` inject at the end of
 *   `updateCameraAndRender`, which is why it had a separate public `render()` next to `renderHud()`:
 *   the queue rendered above everything and independently of the `Hud` module. The 26.2 equivalent
 *   of that hook is `Render2DEvent.Troll`, which `MixinGui` posts from the TAIL of
 *   `Gui.extractRenderState` - after the open screen has been extracted and on its own stratum - so
 *   this element subscribes to it directly instead of relying on `GuiManager`'s gated dispatch.
 *   [renderHud] is therefore left with nothing but the HUD editor's "Example Notification" preview,
 *   which is what keeps the queue from being painted twice while the editor is open.
 * - `GuiGraphicsExtractor.fill` only takes ints, so the animated rectangle edges are rounded to
 *   whole units instead of the original's sub-pixel float quads. Same rounding convention as the
 *   rest of the ported gui (`BasicWindow`).
 * - `TrollFont.drawString` truncates x/y to int at scale 1.0, so the text baseline lands at y = 12
 *   rather than the original's exact 12.5. Same as every other ported text element.
 * - The original cached the measured message width in a `FrameFloat`. That helper registers every
 *   instance in a global `ArrayList` forever and would be constructed off the render thread by
 *   [send]; here the width is simply measured once per [Message.render] call, which happens exactly
 *   once per frame and therefore yields the same value with no shared mutable state and no leak.
 *   Vanilla font measuring stays on the render thread this way.
 *
 * The only non-cosmetic change to the original's logic is in [render]: the expired entries are
 * dropped from [map] after `removeIf` returns instead of from inside its predicate, because
 * `CopyOnWriteArrayList.removeIf` holds its own lock across the predicate and the original's nesting
 * is the exact reverse of [send]'s lock order. Same frame, same result, no ABBA deadlock.
 */
internal object Notification : AbstractHudElement(
    name = "Notification",
    category = Category.CLIENT,
    description = "Client notifications",
    enabledByDefault = true
) {
    private val moduleToggle by setting("Module Toggle", true)
    private val moduleToggleMessageTimeout by setting(
        "Module Toggle Message Timeout",
        3000,
        0..10000,
        100,
        { moduleToggle })
    private val defaultTimeout by setting("Default Timeout", 5000, 0..10000, 100)
    private val nvidia by setting("Nvidia Theme", false)
    private val backgroundAlpha by setting("Background Alpha", 180, 0..255, 1, { nvidia })

    override val hudWidth: Float get() = Message.minWidth

    override val hudHeight: Float get() = Message.height

    /** Default colour of the old MainFontRenderer.drawString overload. */
    private val TEXT_COLOR = ColorRGB(255, 255, 255)

    private val notifications = CopyOnWriteArrayList<Message>()
    private val map = Long2ObjectMaps.synchronize(Long2ObjectOpenHashMap<Message>())

    /** Scratch list for [render]'s map cleanup. Render thread only, never escapes the method. */
    private val expired = ArrayList<Message>()

    init {
        safeListener<ModuleToggleEvent> {
            if (moduleToggle) {
                val message = it.module.nameAsString +
                    if (it.module.isEnabled) ChatFormatting.RED format " disabled"
                    else ChatFormatting.GREEN format " enabled"
                send(Notification.hashCode() * 31 + it.module.hashCode(), message, moduleToggleMessageTimeout.toLong())
            }
        }

        // Stands in for the original's MixinEntityRenderer inject: the queue paints last, above any
        // open screen, and is not gated on the Hud module. Registered (not always-listening) so it
        // lives and dies with `visible`, which is exactly the original render()'s first line.
        listener<Render2DEvent.Troll> { event ->
            if (!visible) return@listener

            val graphics = event.graphics
            graphics.pose().pushMatrix()
            graphics.pose().translate(renderPosX, renderPosY)
            graphics.pose().scale(scale, scale)

            render(graphics)

            graphics.pose().popMatrix()
        }
    }

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        if (Wrapper.minecraft.gui.screen() is TrollHudGui && notifications.isEmpty()) {
            Message.run {
                if (dockingH == HAlign.LEFT) {
                    graphics.fillRect(minWidth - padding, 0.0f, minWidth, height, color)
                } else {
                    graphics.fillRect(0.0f, 0.0f, padding, height, color)
                }

                TrollFont.drawString(
                    graphics, "Example Notification", stringPosX, stringPosY,
                    TEXT_COLOR, drawShadow = true
                )
            }
        }
    }

    private fun render(graphics: GuiGraphicsExtractor) {
        notifications.removeIf {
            graphics.pose().pushMatrix()
            val y = it.render(graphics)
            graphics.pose().popMatrix()

            if (y == -1.0f) {
                // The original dropped the map entry right here. CopyOnWriteArrayList.removeIf holds
                // its own lock while the predicate runs, so taking the map monitor from inside it is
                // the exact reverse of send()'s "map monitor -> notifications.add" order and can
                // deadlock the render thread against a module toggle. Same frame, same result, no
                // nested locks.
                expired.add(it)
                true
            } else {
                graphics.pose().translate(0.0f, y)
                false
            }
        }

        if (expired.isNotEmpty()) {
            synchronized(map) {
                for (message in expired) {
                    if (map[message.id] === message) map.remove(message.id)
                }
            }
            expired.clear()
        }
    }

    fun send(message: String, length: Long = defaultTimeout.toLong()) {
        send(message.hashCode(), message, length)
    }

    fun send(identifier: Any, message: String, length: Long = defaultTimeout.toLong()) {
        send(identifier.hashCode().toLong(), message, length)
    }

    fun send(id: Long, message: String, length: Long = defaultTimeout.toLong()) {
        synchronized(map) {
            val existing = map[id]
            if (existing != null && !existing.isTimeout) {
                existing.update(message, length)
            } else {
                val new = Message(message, length, id)
                map[id] = new
                notifications.add(new)
            }
        }
    }

    private class Message(
        private var message: String,
        private var length: Long,
        val id: Long
    ) {
        private val startTime by lazy { System.currentTimeMillis() }
        val isTimeout get() = System.currentTimeMillis() - startTime > length

        /** Measured on the render thread at the start of every [render] call. */
        private var width = minWidth

        fun update(message: String, length: Long) {
            this.message = message
            this.length = length + (System.currentTimeMillis() - startTime)
        }

        fun render(graphics: GuiGraphicsExtractor): Float {
            width = max(minWidth, padding + padding + TrollFont.getWidth(message) + padding)

            if (dockingH != HAlign.LEFT && width > hudWidth) {
                graphics.pose().translate(hudWidth - width, 0.0f)
            }

            return when (val deltaTotal = Easing.toDelta(startTime)) {
                in 0L..299L -> {
                    val delta = deltaTotal / 300.0f
                    val progress = Easing.OUT_CUBIC.inc(delta)
                    renderStage1(graphics, progress)
                }
                in 300L..500L -> {
                    val delta = (deltaTotal - 300L) / 200.0f
                    val progress = Easing.OUT_CUBIC.inc(delta)
                    renderStage2(graphics, progress)
                }
                else -> {
                    if (deltaTotal < length) {
                        renderStage3(graphics)
                    } else {
                        when (val endDelta = deltaTotal - length) {
                            in 0L..199L -> {
                                val delta = (endDelta) / 200.0f
                                val progress = Easing.OUT_CUBIC.dec(delta)
                                renderStage2(graphics, progress)
                            }
                            in 200L..500L -> {
                                val delta = (endDelta - 200L) / 300.0f
                                val progress = Easing.OUT_CUBIC.dec(delta)
                                renderStage1(graphics, progress)
                            }
                            else -> {
                                -1.0f
                            }
                        }
                    }
                }
            }
        }

        private fun renderStage1(graphics: GuiGraphicsExtractor, progress: Float): Float {
            if (dockingH == HAlign.LEFT) {
                graphics.fillRect(0.0f, 0.0f, width * progress, height, color)
            } else {
                graphics.fillRect(minWidth * (1.0f - progress), 0.0f, width, height, color)
            }

            return (height + space) * progress
        }

        private fun renderStage2(graphics: GuiGraphicsExtractor, progress: Float): Float {
            graphics.fillRect(0.0f, 0.0f, width, height, backGroundColor)

            val textColor = ColorRGB(255, 255, 255, (255.0f * progress).toInt())
            TrollFont.drawString(graphics, message, stringPosX, stringPosY, textColor, drawShadow = true)

            if (dockingH == HAlign.LEFT) {
                graphics.fillRect((width - padding) * progress, 0.0f, width, height, color)
            } else {
                graphics.fillRect(0.0f, 0.0f, padding + (width - padding) * (1.0f - progress), height, color)
            }

            return height + space
        }

        private fun renderStage3(graphics: GuiGraphicsExtractor): Float {
            graphics.fillRect(0.0f, 0.0f, width, height, backGroundColor)

            if (dockingH == HAlign.LEFT) {
                graphics.fillRect(width - padding, 0.0f, width, height, color)
            } else {
                graphics.fillRect(0.0f, 0.0f, padding, height, color)
            }

            TrollFont.drawString(graphics, message, stringPosX, stringPosY, TEXT_COLOR, drawShadow = true)

            return height + space
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Message

            return id == other.id
        }

        override fun hashCode(): Int {
            return HashCommon.long2int(id)
        }

        companion object {
            val color get() = if (nvidia) ColorRGB(118, 185, 0) else GuiColors.primary.alpha(255)
            val backGroundColor get() = if (nvidia) ColorRGB(0, 0, 0, backgroundAlpha) else GuiColors.backGround

            val minWidth get() = 150.0f

            val height get() = TrollFont.getHeight() * 4.0f
            val space get() = 4.0f

            val padding get() = 4.0f
            val stringPosX get() = if (dockingH == HAlign.LEFT) padding else padding + padding
            val stringPosY get() = height * 0.5f - 1.0f - TrollFont.getHeight() * 0.5f
        }
    }
}

/** Float rect helper; 26.2 only offers an int [GuiGraphicsExtractor.fill]. */
private fun GuiGraphicsExtractor.fillRect(x1: Float, y1: Float, x2: Float, y2: Float, color: ColorRGB) {
    fill(x1.roundToInt(), y1.roundToInt(), x2.roundToInt(), y2.roundToInt(), color.toArgb())
}
