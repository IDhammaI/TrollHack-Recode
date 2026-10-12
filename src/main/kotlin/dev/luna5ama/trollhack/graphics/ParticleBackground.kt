package dev.luna5ama.trollhack.graphics

import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The drifting "constellation" field behind the GUI: points that wander and draw a line to any
 * neighbour they come close to, brightening around the cursor.
 *
 * The original was a GLSL fragment shader (assets/trollhack/shaders/gui/Particle.fsh) evaluating
 * the field per pixel. 26.2's gui layer only exposes axis-aligned quads, and a custom
 * RenderPipeline would need its own shader plumbing, so this recreates the same look on the CPU:
 * the points and their links are the same idea, but it is a reimplementation rather than a
 * pixel-exact port of the shader.
 */
object ParticleBackground {

    private const val COUNT = 90
    private const val LINK_DISTANCE = 90.0f
    private const val MOUSE_RADIUS = 140.0f
    private const val SEGMENT = 3.0f

    private val xs = FloatArray(COUNT)
    private val ys = FloatArray(COUNT)
    private val vxs = FloatArray(COUNT)
    private val vys = FloatArray(COUNT)

    private var seeded = false
    private var lastUpdate = 0L
    private var lastWidth = 0.0f
    private var lastHeight = 0.0f

    private fun seed(width: Float, height: Float) {
        for (i in 0 until COUNT) {
            xs[i] = Random.nextFloat() * width
            ys[i] = Random.nextFloat() * height
            // Slow drift, in troll units per second
            vxs[i] = (Random.nextFloat() - 0.5f) * 16.0f
            vys[i] = (Random.nextFloat() - 0.5f) * 16.0f
        }
        seeded = true
        lastWidth = width
        lastHeight = height
    }

    private fun update(width: Float, height: Float) {
        val now = System.currentTimeMillis()
        if (lastUpdate == 0L) lastUpdate = now
        val dt = ((now - lastUpdate) / 1000.0f).coerceAtMost(0.1f)
        lastUpdate = now

        for (i in 0 until COUNT) {
            xs[i] += vxs[i] * dt
            ys[i] += vys[i] * dt

            // Wrap around the edges so the field never thins out
            if (xs[i] < 0.0f) xs[i] += width
            if (xs[i] > width) xs[i] -= width
            if (ys[i] < 0.0f) ys[i] += height
            if (ys[i] > height) ys[i] -= height
        }
    }

    /**
     * @param alphaMul overall opacity, so the field fades in and out with the GUI
     */
    fun render(
        graphics: GuiGraphicsExtractor,
        width: Float,
        height: Float,
        mouseX: Float,
        mouseY: Float,
        alphaMul: Float
    ) {
        if (alphaMul <= 0.0f || width <= 0.0f || height <= 0.0f) return

        if (!seeded || abs(width - lastWidth) > 1.0f || abs(height - lastHeight) > 1.0f) {
            seed(width, height)
        }
        update(width, height)

        for (i in 0 until COUNT) {
            val x = xs[i]
            val y = ys[i]

            // Points near the cursor glow
            val md = distance(x, y, mouseX, mouseY)
            val boost = if (md < MOUSE_RADIUS) 1.0f - md / MOUSE_RADIUS else 0.0f
            val dotAlpha = ((0.25f + boost * 0.75f) * 255.0f * alphaMul).roundToInt().coerceIn(0, 255)

            if (dotAlpha > 0) {
                val s = if (boost > 0.5f) 2 else 1
                graphics.fill(x.toInt(), y.toInt(), x.toInt() + s, y.toInt() + s, argbWhite(dotAlpha))
            }

            for (j in i + 1 until COUNT) {
                val d = distance(x, y, xs[j], ys[j])
                if (d >= LINK_DISTANCE) continue

                val closeness = 1.0f - d / LINK_DISTANCE
                val nearMouse = max(boost, run {
                    val md2 = distance(xs[j], ys[j], mouseX, mouseY)
                    if (md2 < MOUSE_RADIUS) 1.0f - md2 / MOUSE_RADIUS else 0.0f
                })
                val alpha = (closeness * closeness * (0.10f + nearMouse * 0.5f) * 255.0f * alphaMul)
                    .roundToInt()
                    .coerceIn(0, 255)
                if (alpha <= 0) continue

                drawLink(graphics, x, y, xs[j], ys[j], d, argbWhite(alpha))
            }
        }
    }

    /**
     * The gui layer has no line primitive and its matrix stack cannot rotate, so a link is
     * stepped out as a run of small quads along the segment.
     */
    private fun drawLink(
        graphics: GuiGraphicsExtractor,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        length: Float,
        argb: Int
    ) {
        val steps = (length / SEGMENT).toInt().coerceAtLeast(1)
        val dx = (x2 - x1) / steps
        val dy = (y2 - y1) / steps

        var px = x1
        var py = y1
        for (s in 0 until steps) {
            val ix = px.toInt()
            val iy = py.toInt()
            graphics.fill(ix, iy, ix + 1, iy + 1, argb)
            px += dx
            py += dy
        }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    private fun argbWhite(alpha: Int) = (alpha shl 24) or 0xFFFFFF
}
