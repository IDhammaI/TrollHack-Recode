package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.fastmc.common.floorToInt
import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.TrollFont
import dev.luna5ama.trollhack.gui.GuiColors
import dev.luna5ama.trollhack.gui.hudgui.AbstractHudElement
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.setting.GuiConfig.setting
import dev.luna5ama.trollhack.util.EntityUtils
import dev.luna5ama.trollhack.util.EntityUtils.isNeutral
import dev.luna5ama.trollhack.util.EntityUtils.isPassive
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.entity.LivingEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ported from the 1.12 Radar, which drew everything with legacy immediate mode GL
 * (`GL_POINTS`/`GL_LINES`/`GL_QUADS` through `RenderUtils2D`, plus `GlStateManager.translate/rotate`).
 * 26.2 has no immediate mode path at all, so the whole thing is rebuilt on
 * [GuiGraphicsExtractor.fill] quads plus the `Matrix3x2fStack` pose. The layout numbers, setting
 * names/defaults/ordering/ranges, colours and the compass orientation are unchanged.
 *
 * Deviations forced by 26.2, all of them in *how* a shape is emitted, not in what it looks like:
 *
 * - **Sub-pixel space.** `fill` only takes `Int` coordinates, but the radar geometry is fractional
 *   (a 48px circle, chunk cells at `48 / range` pixels each). Every shape is therefore emitted with
 *   the pose scaled by 1/[SUB_PIXEL], with coordinates multiplied by [SUB_PIXEL], which restores
 *   1/8 px precision. Rotation and uniform scale commute, so pre-scaling does not disturb the
 *   compass rotation applied on top.
 * - **Filled circle.** The original was a triangle fan; a fan cannot be expressed with axis aligned
 *   quads, so the disc is emitted as 1px horizontal scanlines whose half width is `sqrt(r^2 - y^2)`
 *   at sub-pixel precision. Visually equivalent to (slightly rounder than) the original 75-gon.
 * - **Circle outline.** Kept as the original's 75 segment line loop: [circleSegments] uses the very
 *   same formula `max(round(radius * 0.5 * PI), 16)`, and each chord is one 1.5px thick quad placed
 *   by rotating the pose one segment at a time. `GL_LINE_SMOOTH` is gone, so the ring is not
 *   anti-aliased.
 * - **Entity dots.** `glPointSize` + `GL_POINT_SMOOTH` no longer exist. GL point sprites were always
 *   screen aligned regardless of the modelview rotation, so the entity offsets are rotated in code
 *   and each dot is drawn in the unrotated frame - that keeps the original behaviour (dots do not
 *   spin with the map). `GL_POINT_SMOOTH` drew a round dot, so the dot is kept round by emitting it
 *   as non overlapping horizontal scanlines (same technique as the filled circle, so a translucent
 *   entity colour is never blended over itself). Only the anti-aliased edge is lost.
 * - **Grid lines.** 1px quads instead of `GL_LINES`, also without `GL_LINE_SMOOTH`. All four edges
 *   of every cell are emitted just like the original did, so shared edges double blend exactly as
 *   they did before.
 * - **Line/point thickness unit.** 1.12 sized `glPointSize`/`glLineWidth` in *framebuffer* pixels,
 *   which meant they ignored both the gui scale and the element's `Scale` setting. There is no
 *   equivalent in a quad based emitter, so `Point Size`, the 1px grid line and the 1.5px ring are
 *   sized in the element's own gui units, exactly like every other component in this port
 *   (`AbstractHudElement.renderFrame` does the same). At gui scale 2 they are therefore twice as
 *   thick on screen as 1.12's were; the numbers, and how they scale with `Scale`, stay consistent
 *   with the rest of the ported gui.
 * - **Unloaded chunk test.** `chunk.isEmpty || !chunk.isLoaded` became
 *   `world.chunkSource.hasChunk(x, z)`; 26.2 returns a shared empty chunk instead of a per-position
 *   `Chunk` with those flags.
 * - **New chunk overlay is inert.** The original fed off `ChunkManager`, whose whole heuristic was
 *   "received an `SPacketChunkData` with `isFullChunk == false`". 1.13 removed partial chunk packets
 *   outright, so that signal does not exist in 26.2 and the port has no `ChunkManager` at all. The
 *   "New Chunk"/"New Chunk Color" settings are kept so the settings panel and the config keys stay
 *   1:1, but [isNewChunk] answers false until a manager with a 26.2 detection strategy is ported.
 * - **Partial ticks.** `RenderUtils3D.partialTicks` is replaced by
 *   `deltaTracker.getGameTimeDeltaPartialTick(true)`, the same value MixinGui hands to the 2D pass.
 */
internal object Radar : AbstractHudElement(
    name = "Radar",
    category = Category.WORLD,
    description = "Shows entities and new chunks"
) {
    private val page = setting("Page", Page.ENTITY)

    private val entity0 = setting("Entity", true, page.atValue(Page.ENTITY))
    private val entity by entity0
    private val players by setting("Players", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val passive by setting("Passive Mobs", false, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val neutral by setting("Neutral Mobs", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val hostile by setting("Hostile Mobs", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val invisible by setting("Invisible", true, page.atValue(Page.ENTITY) and entity0.atTrue())
    private val pointSize by setting(
        "Point Size",
        4.0f,
        1.0f..16.0f,
        0.5f,
        page.atValue(Page.ENTITY) and entity0.atTrue()
    )

    private val chunk0 = setting("Chunk", false, page.atValue(Page.CHUNK))
    private val chunk by chunk0
    private val newChunk0 = setting("New Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val newChunk by newChunk0
    private val newChunkColor by setting(
        "New Chunk Color",
        ColorRGB(255, 31, 31, 63),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and newChunk0.atTrue()
    )
    private val unloadedChunk0 = setting("Unloaded Chunk", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val unloadedChunk by unloadedChunk0
    private val unloadedChunkColor by setting(
        "Unloaded Chunk Color",
        ColorRGB(255, 127, 127, 127),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and unloadedChunk0.atTrue()
    )
    private val chunkGrid0 = setting("Chunk Grid", true, page.atValue(Page.CHUNK) and chunk0.atTrue())
    private val chunkGrid by chunkGrid0
    private val gridColor by setting(
        "Grid Color",
        ColorRGB(127, 127, 127, 63),
        true,
        page.atValue(Page.CHUNK) and chunk0.atTrue() and chunkGrid0.atTrue()
    )

    private val radarRange by setting("Radar Range", 64, 8..512, 1)

    private enum class Page {
        ENTITY, CHUNK
    }

    override val hudWidth: Float get() = 100.0f
    override val hudHeight: Float get() = 100.0f

    private const val halfSize = 50.0f
    private const val radius = 48.0f
    private const val outlineWidth = 1.5f

    /** Sub-pixel units per gui pixel; a power of two so 1/[SUB_PIXEL] is an exact float. */
    private const val SUB_PIXEL = 8.0f
    private const val INV_SUB_PIXEL = 1.0f / SUB_PIXEL

    private val quarterTurn = 90.0f.toRadians()

    override fun renderHud(graphics: GuiGraphicsExtractor) {
        super.renderHud(graphics)

        runSafe {
            val partialTicks = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
            val angle = (-player.yRot + 180.0f).toRadians()

            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(halfSize, halfSize)

            drawBorder(graphics)

            if (chunk) {
                pose.pushMatrix()
                pose.rotate(angle)
                drawChunk(graphics, partialTicks)
                pose.popMatrix()
            }

            // Drawn outside the rotation on purpose: GL point sprites were screen aligned, so the
            // offsets get rotated in code instead (see the class doc).
            if (entity) drawEntity(graphics, partialTicks, angle)

            pose.pushMatrix()
            pose.rotate(angle)
            drawLabels(graphics)
            pose.popMatrix()

            pose.popMatrix()
        }
    }

    private fun drawBorder(graphics: GuiGraphicsExtractor) {
        drawCircleFilled(graphics, radius, GuiColors.backGround)
        drawCircleOutline(graphics, radius, outlineWidth, GuiColors.text)
    }

    private fun SafeClientEvent.drawEntity(graphics: GuiGraphicsExtractor, partialTicks: Float, angle: Float) {
        val playerPos = EntityUtils.getInterpolatedPos(player, partialTicks)
        val posMultiplier = radius / radarRange

        val sinAngle = sin(angle)
        val cosAngle = cos(angle)
        val half = pointSize * 0.5f

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(INV_SUB_PIXEL, INV_SUB_PIXEL)

        // Player marker
        drawPoint(graphics, 0.0f, 0.0f, half, GuiColors.text.toArgb())

        for (target in getEntityList()) {
            val diff = EntityUtils.getInterpolatedPos(target, partialTicks).subtract(playerPos)
            if (abs(diff.y) > 24.0) continue

            val x = (diff.x * posMultiplier).toFloat()
            val y = (diff.z * posMultiplier).toFloat()

            drawPoint(
                graphics,
                x * cosAngle - y * sinAngle,
                x * sinAngle + y * cosAngle,
                half,
                getColor(target).toArgb()
            )
        }

        pose.popMatrix()
    }

    private fun getEntityList(): List<LivingEntity> {
        val playerTargets = arrayOf(players, true, true) // Enable friends and sleeping
        val mobTargets = arrayOf(true, passive, neutral, hostile) // Enable mobs
        return EntityUtils.getTargetList(playerTargets, mobTargets, invisible, radarRange.toFloat(), ignoreSelf = true)
    }

    /**
     * The 1.12 dot was a `GL_POINT_SMOOTH` sprite, i.e. a round dot of `Point Size` pixels centred
     * on the vertex. It is rebuilt here out of non overlapping 1px scanlines whose half width is
     * `sqrt(r^2 - dy^2)` (the same emitter shape as [drawCircleFilled]), so the dot stays round and
     * a translucent entity colour is never blended over itself. `Point Size` 1 collapses to the
     * single 1px quad it always was.
     */
    private fun drawPoint(graphics: GuiGraphicsExtractor, x: Float, y: Float, half: Float, argb: Int) {
        val rows = max(ceil(half * 2.0f).toInt(), 1)
        val rowHeight = half * 2.0f / rows

        for (row in 0 until rows) {
            val y1 = y - half + row * rowHeight
            val y2 = y1 + rowHeight
            val offset = (y1 + y2) * 0.5f - y
            val halfWidthSq = half * half - offset * offset
            if (halfWidthSq <= 0.0f) continue

            val halfWidth = sqrt(halfWidthSq)
            fillSubPixel(graphics, x - halfWidth, y1, x + halfWidth, y2, argb)
        }
    }

    private fun SafeClientEvent.drawChunk(graphics: GuiGraphicsExtractor, partialTicks: Float) {
        val interpolatedPos = EntityUtils.getInterpolatedPos(player, partialTicks)
        val playerChunkX = (interpolatedPos.x / 16.0).floorToInt()
        val playerChunkZ = (interpolatedPos.z / 16.0).floorToInt()

        val posMultiplier = radius / radarRange
        val diffX = (playerChunkX * 16 - interpolatedPos.x) * posMultiplier
        val diffZ = (playerChunkZ * 16 - interpolatedPos.z) * posMultiplier

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(INV_SUB_PIXEL, INV_SUB_PIXEL)

        // Grid first, filled cells on top - same order as the original's two batched draws.
        if (chunkGrid) drawChunkGrid(graphics, diffX, diffZ)
        drawChunkFilled(graphics, playerChunkX, playerChunkZ, diffX, diffZ)

        pose.popMatrix()
    }

    private fun drawChunkGrid(graphics: GuiGraphicsExtractor, diffX: Double, diffZ: Double) {
        val argb = gridColor.toArgb()
        val half = 0.5f

        forEachChunkInRange(diffX, diffZ) { _, _, x1, y1, x2, y2 ->
            fillSubPixel(graphics, x1 - half, y1, x1 + half, y2, argb)
            fillSubPixel(graphics, x1, y2 - half, x2, y2 + half, argb)
            fillSubPixel(graphics, x2 - half, y1, x2 + half, y2, argb)
            fillSubPixel(graphics, x1, y1 - half, x2, y1 + half, argb)
        }
    }

    private fun SafeClientEvent.drawChunkFilled(
        graphics: GuiGraphicsExtractor,
        playerChunkX: Int,
        playerChunkZ: Int,
        diffX: Double,
        diffZ: Double
    ) {
        if (!unloadedChunk && !newChunk) return

        val unloadedArgb = unloadedChunkColor.toArgb()
        val newArgb = newChunkColor.toArgb()
        val chunkSource = world.chunkSource

        forEachChunkInRange(diffX, diffZ) { offsetX, offsetZ, x1, y1, x2, y2 ->
            val chunkX = offsetX + playerChunkX
            val chunkZ = offsetZ + playerChunkZ

            if (unloadedChunk && !chunkSource.hasChunk(chunkX, chunkZ)) {
                fillSubPixel(graphics, x1, y1, x2, y2, unloadedArgb)
            }

            if (newChunk && isNewChunk(chunkX, chunkZ)) {
                fillSubPixel(graphics, x1, y1, x2, y2, newArgb)
            }
        }
    }

    /**
     * The original walked the chunk square once and cached the surviving cells in an
     * `IntArrayList`/`FloatArrayList` pair so the grid batch and the quad batch could share it.
     * Without immediate mode batching there is nothing to share, so the (very cheap) cull is simply
     * run once per pass; the visited cells and their order are identical.
     */
    private fun forEachChunkInRange(
        diffX: Double,
        diffZ: Double,
        block: (offsetX: Int, offsetZ: Int, x1: Float, y1: Float, x2: Float, y2: Float) -> Unit
    ) {
        val chunkDist = radarRange / 16
        val posMultiplier = radius / radarRange
        val chunkPosMultiplier = posMultiplier * 16.0
        val rangeSq = (radarRange * posMultiplier) * (radarRange * posMultiplier)

        for (chunkX in -chunkDist..chunkDist) {
            for (chunkZ in -chunkDist..chunkDist) {
                val x1 = (chunkX * chunkPosMultiplier + diffX).toFloat()
                val y1 = (chunkZ * chunkPosMultiplier + diffZ).toFloat()
                val x2 = ((chunkX + 1) * chunkPosMultiplier + diffX).toFloat()
                val y2 = ((chunkZ + 1) * chunkPosMultiplier + diffZ).toFloat()

                if (calcMaxDistSq(x1, y1, x2, y2) >= rangeSq) continue

                block(chunkX, chunkZ, x1, y1, x2, y2)
            }
        }
    }

    private fun calcMaxDistSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val maxX = if (x1 + x2 >= 0.0f) x2 else x1
        val maxY = if (y1 + y2 >= 0.0f) y2 else y1
        return maxX * maxX + maxY * maxY
    }

    /**
     * Always false: see the class doc. 1.13+ has no partial chunk packet, which was the entire
     * signal the 1.12 `ChunkManager` used, and this port has no `ChunkManager` yet.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun isNewChunk(chunkX: Int, chunkZ: Int): Boolean = false

    private fun drawLabels(graphics: GuiGraphicsExtractor) {
        drawLabel(graphics, "-Z")
        drawLabel(graphics, "+X")
        drawLabel(graphics, "+Z")
        drawLabel(graphics, "-X")
    }

    private fun drawLabel(graphics: GuiGraphicsExtractor, name: String) {
        TrollFont.drawString(
            graphics,
            name,
            TrollFont.getWidth(name, 0.8f) * -0.5f,
            -radius,
            GuiColors.primary,
            0.8f
        )
        graphics.pose().rotate(quarterTurn)
    }

    private fun getColor(entity: LivingEntity): ColorRGB {
        return if (entity.isPassive || FriendManager.isFriend(entity.name.string)) {
            // Green
            ColorRGB(32, 224, 32, 224)
        } else if (entity.isNeutral) {
            // Yellow
            ColorRGB(255, 240, 32)
        } else {
            // Red
            ColorRGB(255, 32, 32)
        }
    }

    // ---- Shape emitters ----

    private fun drawCircleFilled(graphics: GuiGraphicsExtractor, r: Float, color: ColorRGB) {
        val argb = color.toArgb()
        val rows = ceil(r).toInt()

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(INV_SUB_PIXEL, INV_SUB_PIXEL)

        for (row in -rows until rows) {
            val yCenter = row + 0.5f
            val halfWidthSq = r * r - yCenter * yCenter
            if (halfWidthSq <= 0.0f) continue

            val halfWidth = sqrt(halfWidthSq)
            fillSubPixel(graphics, -halfWidth, row.toFloat(), halfWidth, (row + 1).toFloat(), argb)
        }

        pose.popMatrix()
    }

    private fun drawCircleOutline(graphics: GuiGraphicsExtractor, r: Float, lineWidth: Float, color: ColorRGB) {
        val argb = color.toArgb()
        val segments = circleSegments(r)
        val segmentAngle = (2.0 * PI / segments).toFloat()
        val halfChord = r * sin(segmentAngle * 0.5f)
        val chordDist = r * cos(segmentAngle * 0.5f)
        val halfWidth = lineWidth * 0.5f

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.scale(INV_SUB_PIXEL, INV_SUB_PIXEL)
        pose.rotate(segmentAngle * 0.5f)

        repeat(segments) {
            fillSubPixel(graphics, -halfChord, -chordDist - halfWidth, halfChord, -chordDist + halfWidth, argb)
            pose.rotate(segmentAngle)
        }

        pose.popMatrix()
    }

    /** The original `RenderUtils2D.calcSegments` for a full 360 degree arc. */
    private fun circleSegments(r: Float): Int = max((r * 0.5 * PI).roundToInt(), 16)

    /** Emits a quad given in gui pixels; the caller must already have scaled the pose by 1/[SUB_PIXEL]. */
    private fun fillSubPixel(graphics: GuiGraphicsExtractor, x1: Float, y1: Float, x2: Float, y2: Float, argb: Int) {
        graphics.fill(
            (x1 * SUB_PIXEL).roundToInt(),
            (y1 * SUB_PIXEL).roundToInt(),
            (x2 * SUB_PIXEL).roundToInt(),
            (y2 * SUB_PIXEL).roundToInt(),
            argb
        )
    }
}
