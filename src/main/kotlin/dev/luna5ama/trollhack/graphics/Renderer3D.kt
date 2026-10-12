package dev.luna5ama.trollhack.graphics

import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector3f
import java.util.*
import kotlin.math.sqrt

/**
 * Immediate mode world renderer for ESP boxes, tracers and lines
 * built on the Minecraft 26.2 GPU pipeline API.
 *
 * Valid for one frame: modules draw into it during [dev.luna5ama.trollhack.event.events.render.Render3DEvent],
 * then [flush] submits everything in four batches (quads/lines, each with a
 * see-through and a depth tested variant).
 *
 * All coordinates are absolute world positions; they are made camera relative
 * internally to keep float precision at far coordinates.
 */
class Renderer3D {
    private val stagedBuffer = StagedVertexBuffer({ "Troll Hack 3D" }, 65536)

    private var quadsThrough: StagedVertexBuffer.Draw? = null
    private var quadsDepth: StagedVertexBuffer.Draw? = null
    private var linesThrough: StagedVertexBuffer.Draw? = null
    private var linesDepth: StagedVertexBuffer.Draw? = null

    var camX = 0.0; private set
    var camY = 0.0; private set
    var camZ = 0.0; private set
    private var viewMatrix: Matrix4fc = Matrix4f()

    /** Starts a new frame; called by the level render hook before the event fires. */
    fun begin(cameraPos: Vec3, viewMatrixIn: Matrix4fc) {
        camX = cameraPos.x
        camY = cameraPos.y
        camZ = cameraPos.z
        viewMatrix = viewMatrixIn

        quadsThrough = stagedBuffer.appendDraw(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS)
        quadsDepth = stagedBuffer.appendDraw(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS)
        linesThrough =
            stagedBuffer.appendDraw(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, PrimitiveTopology.LINES)
        linesDepth =
            stagedBuffer.appendDraw(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, PrimitiveTopology.LINES)
    }

    fun line(from: Vec3, to: Vec3, color: ColorRGB, width: Float, throughWalls: Boolean = true) {
        line(from.x, from.y, from.z, to.x, to.y, to.z, color, color, width, throughWalls)
    }

    fun line(
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color1: ColorRGB, color2: ColorRGB, width: Float, throughWalls: Boolean = true
    ) {
        val draw = (if (throughWalls) linesThrough else linesDepth) ?: return
        val buffer = stagedBuffer.getVertexBuilder(draw)

        val fx1 = (x1 - camX).toFloat()
        val fy1 = (y1 - camY).toFloat()
        val fz1 = (z1 - camZ).toFloat()
        val fx2 = (x2 - camX).toFloat()
        val fy2 = (y2 - camY).toFloat()
        val fz2 = (z2 - camZ).toFloat()

        val dx = fx2 - fx1
        val dy = fy2 - fy1
        val dz = fz2 - fz1
        val length = sqrt(dx * dx + dy * dy + dz * dz)
        if (length < 1.0e-5f) return

        val nx = dx / length
        val ny = dy / length
        val nz = dz / length

        buffer.addVertex(fx1, fy1, fz1).setColor(color1.toArgb()).setNormal(nx, ny, nz).setLineWidth(width)
        buffer.addVertex(fx2, fy2, fz2).setColor(color2.toArgb()).setNormal(nx, ny, nz).setLineWidth(width)
    }

    fun boxOutline(box: AABB, color: ColorRGB, width: Float, throughWalls: Boolean = true) {
        val x1 = box.minX
        val y1 = box.minY
        val z1 = box.minZ
        val x2 = box.maxX
        val y2 = box.maxY
        val z2 = box.maxZ

        // Bottom loop
        line(x1, y1, z1, x2, y1, z1, color, color, width, throughWalls)
        line(x2, y1, z1, x2, y1, z2, color, color, width, throughWalls)
        line(x2, y1, z2, x1, y1, z2, color, color, width, throughWalls)
        line(x1, y1, z2, x1, y1, z1, color, color, width, throughWalls)
        // Top loop
        line(x1, y2, z1, x2, y2, z1, color, color, width, throughWalls)
        line(x2, y2, z1, x2, y2, z2, color, color, width, throughWalls)
        line(x2, y2, z2, x1, y2, z2, color, color, width, throughWalls)
        line(x1, y2, z2, x1, y2, z1, color, color, width, throughWalls)
        // Verticals
        line(x1, y1, z1, x1, y2, z1, color, color, width, throughWalls)
        line(x2, y1, z1, x2, y2, z1, color, color, width, throughWalls)
        line(x2, y1, z2, x2, y2, z2, color, color, width, throughWalls)
        line(x1, y1, z2, x1, y2, z2, color, color, width, throughWalls)
    }

    fun boxFill(box: AABB, color: ColorRGB, throughWalls: Boolean = true) {
        boxFillGradient(box, color, color, throughWalls)
    }

    /** Filled box with different bottom/top colors for a vertical fade. */
    fun boxFillGradient(box: AABB, bottom: ColorRGB, top: ColorRGB, throughWalls: Boolean = true) {
        val draw = (if (throughWalls) quadsThrough else quadsDepth) ?: return
        val buffer = stagedBuffer.getVertexBuilder(draw)

        val x1 = (box.minX - camX).toFloat()
        val y1 = (box.minY - camY).toFloat()
        val z1 = (box.minZ - camZ).toFloat()
        val x2 = (box.maxX - camX).toFloat()
        val y2 = (box.maxY - camY).toFloat()
        val z2 = (box.maxZ - camZ).toFloat()
        val cB = bottom.toArgb()
        val cT = top.toArgb()

        // Bottom, top
        quad(buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, cB, cB, cB, cB)
        quad(buffer, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, cT, cT, cT, cT)
        // North, south
        quad(buffer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, cB, cT, cT, cB)
        quad(buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, cB, cB, cT, cT)
        // West, east
        quad(buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, cB, cB, cT, cT)
        quad(buffer, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, cB, cT, cT, cB)
    }

    /**
     * Filled box drawing only the faces set in [sides], a bitmask of
     * [dev.luna5ama.trollhack.graphics.mask.EnumFacingMask] constants.
     */
    fun boxFillSided(box: AABB, color: ColorRGB, sides: Int, throughWalls: Boolean = true) {
        if (sides == EnumFacingMask.ALL) {
            boxFillGradient(box, color, color, throughWalls)
            return
        }
        if (sides == 0) return

        val draw = (if (throughWalls) quadsThrough else quadsDepth) ?: return
        val buffer = stagedBuffer.getVertexBuilder(draw)

        val x1 = (box.minX - camX).toFloat()
        val y1 = (box.minY - camY).toFloat()
        val z1 = (box.minZ - camZ).toFloat()
        val x2 = (box.maxX - camX).toFloat()
        val y2 = (box.maxY - camY).toFloat()
        val z2 = (box.maxZ - camZ).toFloat()
        val c = color.toArgb()

        if (sides and EnumFacingMask.DOWN != 0) {
            quad(buffer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, c, c, c, c)
        }
        if (sides and EnumFacingMask.UP != 0) {
            quad(buffer, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, c, c, c, c)
        }
        if (sides and EnumFacingMask.NORTH != 0) {
            quad(buffer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, c, c, c, c)
        }
        if (sides and EnumFacingMask.SOUTH != 0) {
            quad(buffer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, c, c, c, c)
        }
        if (sides and EnumFacingMask.WEST != 0) {
            quad(buffer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, c, c, c, c)
        }
        if (sides and EnumFacingMask.EAST != 0) {
            quad(buffer, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, c, c, c, c)
        }
    }

    /** Line from just in front of the camera to the target, the classic tracer. */
    fun tracer(target: Vec3, color: ColorRGB, width: Float, throughWalls: Boolean = true) {
        // Forward = inverse view rotation applied to -Z; rotation-only, so transpose == inverse
        val forward = Matrix4f(viewMatrix).transpose3x3().transformDirection(0.0f, 0.0f, -1.0f, Vector3f())
        val start = Vec3(camX + forward.x, camY + forward.y, camZ + forward.z)
        line(start, target, color, width, throughWalls)
    }

    private fun quad(
        buffer: VertexConsumer,
        x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float, x4: Float, y4: Float, z4: Float,
        c1: Int, c2: Int, c3: Int, c4: Int
    ) {
        buffer.addVertex(x1, y1, z1).setColor(c1)
        buffer.addVertex(x2, y2, z2).setColor(c2)
        buffer.addVertex(x3, y3, z3).setColor(c3)
        buffer.addVertex(x4, y4, z4).setColor(c4)
    }

    /** Uploads and draws all batches; called by the hook after the event. */
    fun flush() {
        stagedBuffer.upload()

        val mainTarget = Minecraft.getInstance().gameRenderer.mainRenderTarget()
        val colorView = mainTarget.colorTextureView ?: return
        val transform = RenderSystem.getDynamicUniforms().writeTransform(Matrix4f(viewMatrix))

        RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                { "Troll Hack 3D" },
                colorView, Optional.empty(),
                mainTarget.depthTextureView, OptionalDouble.empty()
            ).use { pass ->
                drawBatch(pass, QUADS_DEPTH, quadsDepth, transform)
                drawBatch(pass, LINES_DEPTH, linesDepth, transform)
                drawBatch(pass, QUADS_THROUGH_WALLS, quadsThrough, transform)
                drawBatch(pass, LINES_THROUGH_WALLS, linesThrough, transform)
            }

        stagedBuffer.endFrame()
        quadsThrough = null
        quadsDepth = null
        linesThrough = null
        linesDepth = null
    }

    private fun drawBatch(
        pass: RenderPass,
        pipeline: RenderPipeline,
        draw: StagedVertexBuffer.Draw?,
        transform: com.mojang.blaze3d.buffers.GpuBufferSlice
    ) {
        if (draw == null) return
        val info = stagedBuffer.getExecuteInfo(draw) ?: return

        pass.setPipeline(pipeline)
        RenderSystem.bindDefaultUniforms(pass)
        pass.setUniform("DynamicTransforms", transform)
        pass.setVertexBuffer(0, info.vertexBuffer().slice())
        pass.setIndexBuffer(info.indexBuffer(), info.indexType())
        pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0)
    }

    companion object {
        /**
         * World space pipelines built on vanilla snippets.
         *
         * 26.2 uses reversed-Z: the depth tested variants compare with
         * GREATER_THAN_OR_EQUAL (matching vanilla debug pipelines), the
         * see-through variants use ALWAYS_PASS. Depth writes stay off so
         * ESP never occludes the world.
         */
        val QUADS_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("trollhack/pipeline/quads_through_walls")
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build()

        val QUADS_DEPTH: RenderPipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation("trollhack/pipeline/quads_depth")
            .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build()

        val LINES_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("trollhack/pipeline/lines_through_walls")
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build()

        val LINES_DEPTH: RenderPipeline = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("trollhack/pipeline/lines_depth")
            .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build()
    }
}
