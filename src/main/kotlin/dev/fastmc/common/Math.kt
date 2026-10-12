@file:Suppress("NOTHING_TO_INLINE")

package dev.fastmc.common

import kotlin.math.floor
import kotlin.math.sqrt

const val PI_FLOAT = 3.1415927f
const val DEGREE_TO_RADIAN = PI_FLOAT / 180.0f
const val RADIAN_TO_DEGREE = 180.0f / PI_FLOAT

inline val Float.sq: Float get() = this * this
inline val Double.sq: Double get() = this * this
inline val Int.sq: Int get() = this * this

inline val Float.cubic: Float get() = this * this * this
inline val Double.cubic: Double get() = this * this * this

inline val Float.quart: Float get() = (this * this) * (this * this)
inline val Double.quart: Double get() = (this * this) * (this * this)

inline val Float.quint: Float get() = (this * this) * (this * this) * this
inline val Double.quint: Double get() = (this * this) * (this * this) * this

inline fun Float.toRadians() = this * DEGREE_TO_RADIAN
inline fun Double.toRadians() = Math.toRadians(this)

inline fun Float.toDegree() = this * RADIAN_TO_DEGREE
inline fun Double.toDegree() = Math.toDegrees(this)

inline fun Double.floorToInt(): Int = floor(this).toInt()
inline fun Float.floorToInt(): Int = floor(this).toInt()

inline fun Double.ceilToInt(): Int = kotlin.math.ceil(this).toInt()
inline fun Float.ceilToInt(): Int = kotlin.math.ceil(this).toInt()

inline fun distanceSq(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    val dx = x2 - x1
    val dy = y2 - y1
    val dz = z2 - z1
    return dx * dx + dy * dy + dz * dz
}

inline fun distance(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
}

inline fun distanceSq(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    val dx = x2 - x1
    val dy = y2 - y1
    return dx * dx + dy * dy
}

inline fun distance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
    return sqrt(distanceSq(x1, y1, x2, y2))
}

inline fun distanceSq(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val dz = z2 - z1
    return dx * dx + dy * dy + dz * dz
}

inline fun distance(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float): Float {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2))
}

inline fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return dx * dx + dy * dy
}

inline fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    return sqrt(distanceSq(x1, y1, x2, y2))
}

inline fun distanceSq(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Int {
    val dx = x2 - x1
    val dy = y2 - y1
    val dz = z2 - z1
    return dx * dx + dy * dy + dz * dz
}

inline fun distance(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
    return sqrt(distanceSq(x1, y1, z1, x2, y2, z2).toDouble())
}

inline fun distanceSq(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    val dx = x2 - x1
    val dy = y2 - y1
    return dx * dx + dy * dy
}

inline fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    return sqrt(distanceSq(x1, y1, x2, y2).toDouble())
}

inline fun length(x: Double, y: Double): Double = sqrt(x * x + y * y)
inline fun length(x: Double, y: Double, z: Double): Double = sqrt(x * x + y * y + z * z)
inline fun lengthSq(x: Double, y: Double): Double = x * x + y * y
inline fun lengthSq(x: Double, y: Double, z: Double): Double = x * x + y * y + z * z

inline fun length(x: Float, y: Float): Float = sqrt(x * x + y * y)
inline fun length(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)
inline fun lengthSq(x: Float, y: Float): Float = x * x + y * y
inline fun lengthSq(x: Float, y: Float, z: Float): Float = x * x + y * y + z * z
