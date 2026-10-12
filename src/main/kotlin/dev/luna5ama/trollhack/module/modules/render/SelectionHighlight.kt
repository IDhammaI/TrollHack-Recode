package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.atTrue
import dev.luna5ama.trollhack.util.world.getSelectedBox
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

internal object SelectionHighlight : Module(
    name = "Selection Highlight",
    description = "Highlights object you are looking at",
    category = Category.RENDER
) {
    private val expand by setting("Expand", 0.002f, 0.0f..1.0f, 0.001f)
    private val entity by setting("Entity", true)
    private val depth by setting("Depth", false)
    private val filled = setting("Filled", true)
    private val outline = setting("Outline", true)
    private val color by setting("Color", ColorRGB(255, 255, 255))
    private val aFilled by setting("Filled Alpha", 63, 0..255, 1, filled.atTrue())
    private val aOutline by setting("Outline Alpha", 200, 0..255, 1, outline.atTrue())
    private val width by setting("Width", 2.0f, 0.25f..5.0f, 0.25f)

    init {
        safeListener<Render3DEvent> { event ->
            val hitObject = mc.hitResult ?: return@safeListener

            val box = when (hitObject.type) {
                HitResult.Type.ENTITY -> {
                    if (!entity) return@safeListener
                    val target = (hitObject as EntityHitResult).entity
                    target.boundingBox.inflate(expand.toDouble())
                }
                HitResult.Type.BLOCK -> {
                    val pos = (hitObject as BlockHitResult).blockPos
                    world.getSelectedBox(pos)?.inflate(expand.toDouble()) ?: return@safeListener
                }
                else -> return@safeListener
            }

            if (filled.value) {
                event.renderer.boxFill(box, color.alpha(aFilled), !depth)
            }
            if (outline.value) {
                event.renderer.boxOutline(box, color.alpha(aOutline), width, !depth)
            }
        }
    }
}
