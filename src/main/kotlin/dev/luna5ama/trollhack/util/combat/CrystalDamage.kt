package dev.luna5ama.trollhack.util.combat

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

class CrystalDamage(
    val crystalPos: Vec3,
    val blockPos: BlockPos,
    val selfDamage: Float,
    val targetDamage: Float,
    val eyeDistance: Double,
    val feetDistance: Double
) {
    val damageBalance = targetDamage - selfDamage
}
