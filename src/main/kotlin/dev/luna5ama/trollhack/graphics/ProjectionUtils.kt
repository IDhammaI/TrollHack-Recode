package dev.luna5ama.trollhack.graphics

import dev.luna5ama.trollhack.util.Wrapper
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector4f

/**
 * World position -> screen position, for drawing 2D overlays (damage numbers, nametags) over
 * things in the world.
 *
 * The matrices are captured once per frame by the level render hook, right where the world MVP
 * is still what was actually used to draw the frame. All projection happens camera-relative to
 * keep float precision at far coordinates, matching how Renderer3D submits geometry.
 */
object ProjectionUtils {
    private val mc get() = Wrapper.minecraft

    private val mvp = Matrix4f()
    var camPos: Vec3 = Vec3.ZERO; private set

    /** Called from the render hook each frame, before Render3DEvent/Render2DEvent fire. */
    fun update(cameraPos: Vec3, modelView: Matrix4fc, projection: Matrix4fc) {
        camPos = cameraPos
        mvp.set(projection).mul(modelView)
    }

    fun distToCamera(pos: Vec3): Double = pos.distanceTo(camPos)

    /** Screen position in gui-scaled units (the space Render2DEvent draws in). */
    fun toScaledScreenPos(pos: Vec3): Vec3 {
        return toScreenPos(
            pos,
            mc.window.guiScaledWidth.toFloat(),
            mc.window.guiScaledHeight.toFloat()
        )
    }

    /**
     * @return screen position; z is 0.0 when the point lands on screen, -1.0 when it is
     * off-screen or behind the camera (matching the original's contract)
     */
    fun toScreenPos(pos: Vec3, width: Float, height: Float): Vec3 {
        val v = Vector4f(
            (pos.x - camPos.x).toFloat(),
            (pos.y - camPos.y).toFloat(),
            (pos.z - camPos.z).toFloat(),
            1.0f
        )
        mvp.transform(v)

        if (v.w <= 0.0f) {
            // Behind the camera: shove it far off screen so length checks all fail
            return Vec3(-1.0e6, -1.0e6, -1.0)
        }

        val invW = 1.0f / v.w
        val ndcX = v.x * invW
        val ndcY = v.y * invW

        val screenX = (ndcX * 0.5f + 0.5f) * width
        val screenY = (0.5f - ndcY * 0.5f) * height
        val visible = screenX in 0.0f..width && screenY in 0.0f..height

        return Vec3(screenX.toDouble(), screenY.toDouble(), if (visible) 0.0 else -1.0)
    }
}
