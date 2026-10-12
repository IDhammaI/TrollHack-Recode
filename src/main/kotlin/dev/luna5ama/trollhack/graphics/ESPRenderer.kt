package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB

/**
 * Collects boxes during the tick and draws them as filled / outlined / traced ESP.
 *
 * Same API as the 1.12 original with one deliberate change: [render] takes the frame's
 * [Renderer3D]. 26.2 has no global immediate mode GL state to draw into, so the renderer
 * is passed explicitly — callers already have it from
 * [dev.luna5ama.trollhack.event.events.render.Render3DEvent.renderer].
 */
class ESPRenderer {
    private var toRender0: MutableList<Info> = ArrayList()

    val toRender: List<Info>
        get() = toRender0

    var aFilled = 0
    var aOutline = 0
    var aTracer = 0
    var thickness = 2f
    var through = true
    var tracerOffset = 50

    val size: Int
        get() = toRender0.size

    fun add(entity: Entity, color: ColorRGB, partialTicks: Float) {
        add(entity, color, EnumFacingMask.ALL, partialTicks)
    }

    fun add(entity: Entity, color: ColorRGB, sides: Int, partialTicks: Float) {
        val offset = 1.0f - partialTicks
        val x = (entity.xOld - entity.x) * offset
        val y = (entity.yOld - entity.y) * offset
        val z = (entity.zOld - entity.z) * offset
        add(entity.boundingBox.move(x, y, z), color, sides)
    }

    fun add(pos: BlockPos, color: ColorRGB) {
        add(pos, color, EnumFacingMask.ALL)
    }

    fun add(pos: BlockPos, color: ColorRGB, sides: Int) {
        add(AABB(pos), color, sides)
    }

    fun add(box: AABB, color: ColorRGB) {
        add(box, color, EnumFacingMask.ALL)
    }

    fun add(box: AABB, color: ColorRGB, sides: Int) {
        add(Info(box, color, sides))
    }

    fun add(info: Info) {
        toRender0.add(info)
    }

    fun replaceAll(list: MutableList<Info>) {
        toRender0 = list
    }

    fun clear() {
        toRender0.clear()
    }

    fun render(renderer: Renderer3D, clear: Boolean) {
        val filled = aFilled != 0
        val outline = aOutline != 0
        val tracer = aTracer != 0
        if (toRender0.isEmpty() || (!filled && !outline && !tracer)) {
            if (clear) clear()
            return
        }

        for ((box, color, sides) in toRender0) {
            if (filled) {
                val a = (aFilled * (color.a / 255.0f)).toInt()
                renderer.boxFillSided(box, color.alpha(a), sides, through)
            }

            if (outline) {
                val a = (aOutline * (color.a / 255.0f)).toInt()
                renderer.boxOutline(box, color.alpha(a), thickness, through)
            }

            if (tracer) {
                val a = (aTracer * (color.a / 255.0f)).toInt()
                val offset = (tracerOffset - 50) / 100.0 * (box.maxY - box.minY)
                renderer.tracer(box.center.add(0.0, offset, 0.0), color.alpha(a), thickness, through)
            }
        }

        if (clear) clear()
    }

    data class Info(val box: AABB, val color: ColorRGB, val sides: Int) {
        constructor(box: AABB) : this(box, ColorRGB(255, 255, 255), EnumFacingMask.ALL)
        constructor(box: AABB, color: ColorRGB) : this(box, color, EnumFacingMask.ALL)
        constructor(pos: BlockPos) : this(AABB(pos), ColorRGB(255, 255, 255), EnumFacingMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB) : this(AABB(pos), color, EnumFacingMask.ALL)
        constructor(pos: BlockPos, color: ColorRGB, sides: Int) : this(AABB(pos), color, sides)
    }
}
