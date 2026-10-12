package dev.luna5ama.trollhack.util.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.world.getCollisionBox
import dev.luna5ama.trollhack.util.world.isFullBox
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

object HoleUtils {
    private val holeOffset1 = arrayOf(
        BlockPos(0, 0, 0),
    )

    private val holeOffsetCheck1 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 1, 0)
    )

    private val surroundOffset1 = arrayOf(
        BlockPos(0, -1, 0), // Down
        BlockPos(0, 0, -1), // North
        BlockPos(1, 0, 0),  // East
        BlockPos(0, 0, 1),  // South
        BlockPos(-1, 0, 0)  // West
    )

    private val holeOffset2X = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(1, 0, 0),
    )

    private val holeOffsetCheck2X = arrayOf(
        *holeOffset2X,
        BlockPos(0, 1, 0),
        BlockPos(1, 1, 0),
    )

    private val holeOffset2Z = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
    )

    private val holeOffsetCheck2Z = arrayOf(
        *holeOffset2Z,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
    )

    private val surroundOffset2X = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(1, -1, 0),
        BlockPos(-1, 0, 0),
        BlockPos(0, 0, -1),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, -1),
        BlockPos(1, 0, 1),
        BlockPos(2, 0, 0)
    )

    private val surroundOffset2Z = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(0, 0, -1),
        BlockPos(-1, 0, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(1, 0, 1),
        BlockPos(0, 0, 2)
    )

    private val holeOffset4 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, 0),
        BlockPos(1, 0, 1)
    )

    private val holeOffsetCheck4 = arrayOf(
        *holeOffset4,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1)
    )

    private val surroundOffset4 = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(1, -1, 0),
        BlockPos(1, -1, 1),
        BlockPos(-1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(0, 0, -1),
        BlockPos(1, 0, -1),
        BlockPos(0, 0, 2),
        BlockPos(1, 0, 2),
        BlockPos(2, 0, 0),
        BlockPos(2, 0, 1)
    )

    private val mutableBlockPos = ThreadLocal.withInitial {
        BlockPos.MutableBlockPos()
    }

    fun SafeClientEvent.checkHoleM(pos: BlockPos): HoleInfo {
        if (world.isOutsideBuildHeight(pos) || !world.checkAir(pos)) return HoleInfo.empty(pos.immutable())

        val mutablePos = mutableBlockPos.get().set(pos)

        return checkHole1(pos, mutablePos)
            ?: checkHole2(pos, mutablePos)
            ?: checkHole4(pos, mutablePos)
            ?: HoleInfo.empty(pos.immutable())
    }

    private fun SafeClientEvent.checkHole1(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.obsidianHole && !CombatSetting.bedrockHole) return null

        if (!world.checkAirMultiple(holeOffsetCheck1, pos, mutablePos)) return null

        val type = checkSurroundPos(pos, mutablePos, surroundOffset1, HoleType.BEDROCK, HoleType.OBBY)

        if (type == HoleType.NONE) return null
        if (!CombatSetting.bedrockHole && type == HoleType.BEDROCK) return null
        if (!CombatSetting.obsidianHole && type == HoleType.OBBY) return null

        val holePosArray = holeOffset1.offset(pos)

        var trapped = false
        var fullyTrapped = true

        for (holePos in holePosArray) {
            if (world.checkAir(mutablePos.set(holePos.x, holePos.y + 2, holePos.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }

        return HoleInfo(
            pos.immutable(),
            pos.toVec3d(
                0.5,
                0.0, 0.5
            ),
            AABB(pos),
            holePosArray,
            surroundOffset1.offset(pos),
            type,
            trapped,
            fullyTrapped
        )
    }

    private fun SafeClientEvent.checkHole2(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.twoBlocksHole) return null

        var x = true

        if (!world.checkAir(mutablePos.set(pos.x + 1, pos.y, pos.z))) {
            if (!world.checkAir(mutablePos.set(pos.x, pos.y, pos.z + 1))) return null
            else x = false
        }

        val checkArray = if (x) holeOffsetCheck2X else holeOffsetCheck2Z
        if (!world.checkAirMultiple(checkArray, pos, mutablePos)) return null

        val surroundOffset = if (x) surroundOffset2X else surroundOffset2Z
        val holeOffset = if (x) holeOffset2X else holeOffset2Z
        val centerX = if (x) 1.0 else 0.5
        val centerZ = if (x) 0.5 else 1.0

        val type = checkSurroundPos(pos, mutablePos, surroundOffset, HoleType.TWO, HoleType.TWO)

        if (type == HoleType.NONE) return null


        val holePosArray = holeOffset.offset(pos)

        var trapped = false
        var fullyTrapped = true

        for (holePos in holePosArray) {
            if (world.checkAir(mutablePos.set(holePos.x, holePos.y + 2, holePos.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }

        return HoleInfo(
            pos.immutable(),
            pos.toVec3d(
                centerX,
                0.0, centerZ
            ),
            if (x) {
                AABB(
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    pos.x + 2.0, pos.y + 1.0, pos.z + 1.0
                )
            } else {
                AABB(
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    pos.x + 1.0, pos.y + 1.0, pos.z + 2.0
                )
            },
            holePosArray,
            surroundOffset.offset(pos),
            type,
            trapped,
            fullyTrapped
        )
    }

    private fun SafeClientEvent.checkHole4(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!CombatSetting.fourBlocksHole) return null
        if (!world.checkAirMultiple(holeOffsetCheck4, pos, mutablePos)) return null

        val type = checkSurroundPos(pos, mutablePos, surroundOffset4, HoleType.FOUR, HoleType.FOUR)
        if (type == HoleType.NONE) return null

        val holePosArray = holeOffset4.offset(pos)

        var trapped = false
        var fullyTrapped = true

        for (holePos in holePosArray) {
            if (world.checkAir(mutablePos.set(holePos.x, holePos.y + 2, holePos.z))) {
                fullyTrapped = false
            } else {
                trapped = true
            }
        }

        return HoleInfo(
            pos.immutable(),
            pos.toVec3d(
                1.0,
                0.0, 1.0
            ),
            AABB(
                pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                pos.x + 2.0, pos.y + 1.0, pos.z + 2.0
            ),
            holePosArray,
            surroundOffset4.offset(pos),
            type,
            trapped,
            fullyTrapped
        )
    }

    private fun Level.checkAirMultiple(
        array: Array<BlockPos>,
        pos: BlockPos,
        mutablePos: BlockPos.MutableBlockPos
    ): Boolean {
        return array.all {
            checkAir(mutablePos.set(pos.x + it.x, pos.y + it.y, pos.z + it.z))
        }
    }

    private fun Level.checkAir(pos: BlockPos): Boolean {
        val blockState = getBlockState(pos)
        if (blockState.block == Blocks.COBWEB) return false
        if (CombatSetting.ignoreReplaceableFilling && blockState.canBeReplaced()) return true

        val collisionBox = getCollisionBox(pos)
        val lowCollisionBox = collisionBox == null || collisionBox.maxY <= 0.5

        if (CombatSetting.ignoreNonFullBoxFilling && !isFullBox(pos) && lowCollisionBox) return true
        if (CombatSetting.ignoreNonCollidingFilling && lowCollisionBox) return true

        return blockState.isAir
    }

    private fun Array<BlockPos>.offset(pos: BlockPos): ObjectSet<BlockPos> {
        val result = ObjectOpenHashSet<BlockPos>(size)
        for (blockPos in this) {
            result.add(pos.offset(blockPos))
        }
        return result
    }

    private fun SafeClientEvent.checkSurroundPos(
        pos: BlockPos,
        mutablePos: BlockPos.MutableBlockPos,
        surroundOffset: Array<BlockPos>,
        expectType: HoleType,
        obbyType: HoleType
    ): HoleType {
        var type = expectType

        for (offset in surroundOffset) {
            val blockState =
                world.getBlockState(mutablePos.set(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z))
            when {
                blockState.block == Blocks.BEDROCK -> continue
                !blockState.isAir && CrystalUtils.isResistant(blockState) -> type = obbyType
                else -> return HoleType.NONE
            }
        }

        return type
    }
}
