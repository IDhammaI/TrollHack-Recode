package dev.luna5ama.trollhack.util.math.vector

import dev.luna5ama.trollhack.util.math.MathUtils
import net.minecraft.world.phys.Vec3

fun Vec3.lerp(to: Vec3, delta: Float): Vec3 {
    val x = MathUtils.lerp(this.x, to.x, delta.toDouble())
    val y = MathUtils.lerp(this.y, to.y, delta.toDouble())
    val z = MathUtils.lerp(this.z, to.z, delta.toDouble())

    return Vec3(x, y, z)
}

fun Vec3.lerp(to: Vec3, delta: Double): Vec3 {
    val x = MathUtils.lerp(this.x, to.x, delta)
    val y = MathUtils.lerp(this.y, to.y, delta)
    val z = MathUtils.lerp(this.z, to.z, delta)

    return Vec3(x, y, z)
}
