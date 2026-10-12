@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package dev.luna5ama.trollhack.util.math.vector

import dev.fastmc.common.distance
import dev.fastmc.common.distanceSq
import net.minecraft.core.Vec3i
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3

fun Vec3i.distanceToCenter(x: Double, y: Double, z: Double): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}

fun Vec3i.distanceSqToCenter(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, x, y, z)
}


fun Vec3i.distanceToCenter(vec3d: Vec3): Double {
    return distance(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3i.distanceSqToCenter(vec3d: Vec3): Double {
    return distanceSq(this.x + 0.5, this.y + 0.5, this.z + 0.5, vec3d.x, vec3d.y, vec3d.z)
}


fun Vec3i.distanceTo(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

fun Vec3i.distanceSqTo(x: Int, y: Int, z: Int): Int {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}


fun Vec3i.distanceTo(vec3i: Vec3i): Double {
    return distance(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}

fun Vec3i.distanceSqTo(vec3i: Vec3i): Int {
    return distanceSq(this.x, this.y, this.z, vec3i.x, vec3i.y, vec3i.z)
}


fun Vec3.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

fun Vec3.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}


fun Vec3.distanceTo(vec3d: Vec3): Double {
    return distanceTo(vec3d.x, vec3d.y, vec3d.z)
}

fun Vec3.distanceSqTo(vec3d: Vec3): Double {
    return distanceSqTo(vec3d.x, vec3d.y, vec3d.z)
}


fun Vec3.distanceTo(entity: Entity): Double {
    return distanceTo(entity.x, entity.y, entity.z)
}

fun Vec3.distanceSqTo(entity: Entity): Double {
    return distanceSqTo(entity.x, entity.y, entity.z)
}


fun Vec3.distanceToCenter(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}

fun Vec3.distanceSqToCenter(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}


fun Vec3.distanceToCenter(vec3i: Vec3i): Double {
    return distanceToCenter(vec3i.x, vec3i.y, vec3i.z)
}

fun Vec3.distanceSqToCenter(vec3i: Vec3i): Double {
    return distanceSqToCenter(vec3i.x, vec3i.y, vec3i.z)
}


fun Entity.distanceTo(x: Double, y: Double, z: Double): Double {
    return distance(this.x, this.y, this.z, x, y, z)
}

fun Entity.distanceSqTo(x: Double, y: Double, z: Double): Double {
    return distanceSq(this.x, this.y, this.z, x, y, z)
}


fun Entity.distanceTo(vec3d: Vec3): Double {
    return distanceTo(vec3d.x, vec3d.y, vec3d.z)
}

fun Entity.distanceSqTo(vec3d: Vec3): Double {
    return distanceSqTo(vec3d.x, vec3d.y, vec3d.z)
}


fun Entity.distanceTo(entity: Entity): Double {
    return distanceTo(entity.x, entity.y, entity.z)
}

fun Entity.distanceSqTo(entity: Entity): Double {
    return distanceSqTo(entity.x, entity.y, entity.z)
}


fun Entity.distanceToCenter(x: Int, y: Int, z: Int): Double {
    return distance(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}

fun Entity.distanceSqToCenter(x: Int, y: Int, z: Int): Double {
    return distanceSq(this.x, this.y, this.z, x + 0.5, y + 0.5, z + 0.5)
}


fun Entity.distanceToCenter(vec3i: Vec3i): Double {
    return distanceToCenter(vec3i.x, vec3i.y, vec3i.z)
}

fun Entity.distanceSqToCenter(vec3i: Vec3i): Double {
    return distanceSqToCenter(vec3i.x, vec3i.y, vec3i.z)
}

fun Entity.hDistanceTo(x: Double, y: Double): Double {
    return distance(this.x, this.z, x, y)
}

fun Entity.hDistanceSqTo(x: Double, y: Double): Double {
    return distanceSq(this.x, this.z, x, y)
}

fun Entity.hDistanceToCenter(x: Int, y: Int): Double {
    return distance(this.x, this.z, x + 0.5, y + 0.5)
}

fun Entity.hDistanceSqToCenter(x: Int, y: Int): Double {
    return distanceSq(this.x, this.z, x + 0.5, y + 0.5)
}

fun Entity.hDistanceToCenter(chunkPos: ChunkPos): Double {
    return hDistanceToCenter(chunkPos.x * 16 + 8, chunkPos.z * 16 + 8)
}

fun Entity.hDistanceSqToCenter(chunkPos: ChunkPos): Double {
    return hDistanceSqToCenter(chunkPos.x * 16 + 8, chunkPos.z * 16 + 8)
}
