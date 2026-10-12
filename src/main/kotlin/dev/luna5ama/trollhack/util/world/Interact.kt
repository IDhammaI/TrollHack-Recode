package dev.luna5ama.trollhack.util.world

import dev.fastmc.common.collection.FastObjectArrayList
import dev.fastmc.common.floorToInt
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.serverSideItem
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.collections.EnumSet
import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import dev.luna5ama.trollhack.util.inventory.slot.offhandSlot
import dev.luna5ama.trollhack.util.math.isInSight
import dev.luna5ama.trollhack.util.math.vector.Vec3f
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.math.vector.toVec3dCenter
import dev.luna5ama.trollhack.util.accessor.withPrediction
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Input
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

fun SafeClientEvent.checkPlaceRotation(placeInfo: PlaceInfo): Boolean {
    var eyeHeight = player.eyeHeight
    if (!player.isShiftKeyDown) eyeHeight -= 0.08f
    return AABB(placeInfo.pos).isInSight(
        PlayerPacketManager.position.add(
            0.0,
            eyeHeight.toDouble(),
            0.0
        ), rotation = PlayerPacketManager.rotation
    )
}

fun SafeClientEvent.getPlacement(
    targetPos: BlockPos,
    vararg options: PlacementSearchOption?
) = getPlacement(targetPos, 3, SIDES, *options)

fun SafeClientEvent.getPlacement(
    targetPos: BlockPos,
    sides: Array<Direction>,
    vararg options: PlacementSearchOption?
) = getPlacement(targetPos, 3, sides, *options)

fun SafeClientEvent.getPlacement(
    targetPos: BlockPos,
    maxDepth: Int,
    vararg options: PlacementSearchOption?
) = getPlacement(targetPos, maxDepth, SIDES, *options)

fun SafeClientEvent.getPlacement(
    targetPos: BlockPos,
    maxDepth: Int,
    sides: Array<Direction>,
    vararg options: PlacementSearchOption?
): PlaceInfo? {
    val queue = ObjectArrayFIFOQueue<SearchData>()
    val visited = LongOpenHashSet()

    for (side in sides) {
        queue.enqueue(SearchData(targetPos, side, targetPos.relative(side)))
    }

    loop@ while (!queue.isEmpty) {
        val data = queue.dequeue()
        for (option in options) {
            if (option == null) continue
            if (!option.run { check(data.pos, data.side, data.placedPos) }) continue@loop
        }

        if (!world.getBlockState(data.pos).canBeReplaced()) {
            return data.toPlaceInfo(player.eyePosition)
        }

        if (data.depth > maxDepth) continue
        if (!visited.add(data.pos.asLong())) continue

        for (side in sides) {
            val next = data.next(side)
            queue.enqueue(next)
        }
    }

    return null
}

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    vararg options: PlacementSearchOption?
) = getPlacementSequence(targetPos, 3, SIDES, *options)

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int,
    vararg options: PlacementSearchOption?
) = getPlacementSequence(targetPos, maxDepth, SIDES, *options)

fun SafeClientEvent.getPlacementSequence(
    targetPos: BlockPos,
    maxDepth: Int,
    sides: Array<Direction>,
    vararg options: PlacementSearchOption?
): List<PlaceInfo>? {
    val queue = ObjectArrayFIFOQueue<SearchData>()
    val visited = LongOpenHashSet()

    for (side in sides) {
        queue.enqueue(SearchData(targetPos.relative(side), side.opposite, targetPos))
    }

    loop@ while (!queue.isEmpty) {
        val data = queue.dequeue()
        if (world.isOutsideBuildHeight(data.placedPos)) continue@loop
        if (!world.worldBorder.isWithinBounds(data.placedPos)) continue@loop

        for (option in options) {
            if (option == null) continue
            if (!option.run { check(data.pos, data.side, data.placedPos) }) continue@loop
        }

        if (!world.getBlockState(data.pos).canBeReplaced()) {
            return data.toPlacementSequence(player.eyePosition)
        }

        if (data.depth >= maxDepth) continue
        if (!visited.add(data.pos.asLong())) continue

        for (side in sides) {
            val next = data.next(side)
            queue.enqueue(next)
        }
    }

    return null
}

private val SIDES = arrayOf(
    Direction.NORTH,
    Direction.SOUTH,
    Direction.EAST,
    Direction.WEST,
    Direction.UP,
    Direction.DOWN
)

fun interface PlacementSearchOption {
    fun SafeClientEvent.check(from: BlockPos, side: Direction, to: BlockPos): Boolean

    companion object {
        @JvmStatic
        fun range(range: Float) = range(range.toDouble())

        @JvmStatic
        fun range(range: Double): PlacementSearchOption {
            val rangeSq = range * range
            return PlacementSearchOption { from, side, _ ->
                val sideVec = side.unitVec3i
                val hitX = from.x + sideVec.x * 0.5
                val hitY = from.y + sideVec.y * 0.5
                val hitZ = from.z + sideVec.z * 0.5
                player.distanceSqTo(hitX, hitY, hitZ) <= rangeSq
            }
        }

        @JvmField
        val VISIBLE_SIDE = PlacementSearchOption { from, side, _ ->
            isSideVisible(
                player.x,
                player.y + player.eyeHeight,
                player.z,
                from,
                side
            )
        }

        @JvmField
        val ENTITY_COLLISION = PlacementSearchOption { _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }

        @JvmField
        val ENTITY_COLLISION_IGNORE_SELF = PlacementSearchOption { _, _, to ->
            EntityManager.checkNoEntityCollision(to)
        }
    }
}

private data class SearchData(
    val prev: SearchData?,
    val pos: BlockPos,
    val side: Direction,
    val placedPos: BlockPos,
    val depth: Int
) {
    constructor(pos: BlockPos, side: Direction, placedPos: BlockPos) :
        this(null, pos, side, placedPos, 1)

    fun next(side: Direction): SearchData {
        val newPos = pos.relative(side.opposite)
        val newPlacedPos = pos
        return SearchData(this, newPos, side, newPlacedPos, depth + 1)
    }

    fun toPlaceInfo(src: Vec3): PlaceInfo {
        val hitVecOffset = getHitVecOffset(side)
        val hitVec = getHitVec(pos, side)

        return PlaceInfo(pos, side, src.distanceTo(hitVec), hitVecOffset, hitVec, placedPos)
    }

    fun toPlacementSequence(src: Vec3): List<PlaceInfo> {
        val sequence = FastObjectArrayList<PlaceInfo>()
        var data: SearchData? = this

        while (data != null) {
            sequence.add(data.toPlaceInfo(src))
            data = data.prev
        }

        return sequence
    }
}

fun SafeClientEvent.getClosestSide(pos: BlockPos): Direction {
    val dx = player.x - pos.x
    val dy = player.y - pos.y
    val dz = player.z - pos.z

    return Direction.entries.maxBy {
        val vec = it.unitVec3i
        dx * vec.x + dy * vec.y + dz * vec.z
    }
}


fun SafeClientEvent.getMiningSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .filter { !world.getBlockState(pos.relative(it)).isCollisionShapeFullBlock(world, pos.relative(it)) }
        .minByOrNull { eyePos.distanceSqTo(getHitVec(pos, it)) }
}

fun SafeClientEvent.getClosestVisibleSide(pos: BlockPos): Direction? {
    val eyePos = player.eyePosition

    return getVisibleSides(pos)
        .minByOrNull { eyePos.distanceSqTo(getHitVec(pos, it)) }
}

/**
 * Get the "visible" sides related to player's eye position
 */
fun SafeClientEvent.getVisibleSides(pos: BlockPos, assumeAirAsFullBox: Boolean = false): Set<Direction> {
    val visibleSides = EnumSet<Direction>()

    val eyePos = player.eyePosition
    val blockCenter = pos.toVec3dCenter()
    val blockState = world.getBlockState(pos)
    val isFullBox = assumeAirAsFullBox && blockState.block == Blocks.AIR
        || blockState.isCollisionShapeFullBlock(world, pos)

    return visibleSides
        .checkAxis(eyePos.x - blockCenter.x, Direction.WEST, Direction.EAST, !isFullBox)
        .checkAxis(eyePos.y - blockCenter.y, Direction.DOWN, Direction.UP, true)
        .checkAxis(eyePos.z - blockCenter.z, Direction.NORTH, Direction.SOUTH, !isFullBox)
}

private fun java.util.EnumSet<Direction>.checkAxis(
    diff: Double,
    negativeSide: Direction,
    positiveSide: Direction,
    bothIfInRange: Boolean
) =
    this.apply {
        when {
            diff < -0.5 -> {
                add(negativeSide)
            }
            diff > 0.5 -> {
                add(positiveSide)
            }
            else -> {
                if (bothIfInRange) {
                    add(negativeSide)
                    add(positiveSide)
                }
            }
        }
    }

fun getHitVec(pos: BlockPos, facing: Direction): Vec3 {
    val vec = facing.unitVec3i
    return Vec3(vec.x * 0.5 + 0.5 + pos.x, vec.y * 0.5 + 0.5 + pos.y, vec.z * 0.5 + 0.5 + pos.z)
}

fun getHitVecOffset(facing: Direction): Vec3f {
    val vec = facing.unitVec3i
    return Vec3f(vec.x * 0.5f + 0.5f, vec.y * 0.5f + 0.5f, vec.z * 0.5f + 0.5f)
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    hand: InteractionHand = InteractionHand.MAIN_HAND
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val sneak = !player.isShiftKeyDown && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) sendSneakPacket(true)

    world.withPrediction { sequence ->
        connection.send(placeInfo.toPlacePacket(hand, sequence))
    }
    player.swing(hand)

    if (sneak) sendSneakPacket(false)

    playPlaceSound(placeInfo, InteractionHand.MAIN_HAND)
}

/**
 * Placing block without desync
 */
fun SafeClientEvent.placeBlock(
    placeInfo: PlaceInfo,
    slot: Slot
) {
    if (!world.isPlaceable(placeInfo.placedPos)) return

    val hand = if (slot == player.offhandSlot) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND

    val sneak = !player.isShiftKeyDown && blockBlacklist.contains(world.getBlock(placeInfo.pos))
    if (sneak) sendSneakPacket(true)

    if (hand == InteractionHand.OFF_HAND) {
        world.withPrediction { sequence ->
            connection.send(placeInfo.toPlacePacket(hand, sequence))
        }
    } else {
        ghostSwitch(slot) {
            world.withPrediction { sequence ->
                connection.send(placeInfo.toPlacePacket(hand, sequence))
            }
        }
    }

    player.swing(hand)

    if (sneak) sendSneakPacket(false)

    playPlaceSound(placeInfo, hand)
}

private fun SafeClientEvent.sendSneakPacket(sneak: Boolean) {
    val keyPresses = player.input.keyPresses
    connection.send(
        ServerboundPlayerInputPacket(
            Input(
                keyPresses.forward(),
                keyPresses.backward(),
                keyPresses.left(),
                keyPresses.right(),
                keyPresses.jump(),
                sneak,
                keyPresses.sprint()
            )
        )
    )
}

private fun SafeClientEvent.playPlaceSound(placeInfo: PlaceInfo, hand: InteractionHand) {
    val itemStack = player.serverSideItem
    val block = (itemStack.item as? BlockItem)?.block ?: return
    val blockState = block.getStateForPlacement(
        BlockPlaceContext(
            player,
            hand,
            itemStack,
            placeInfo.toBlockHitResult()
        )
    ) ?: return
    val soundType = blockState.soundType
    onMainThreadSafe {
        world.playSound(
            player,
            placeInfo.pos,
            soundType.placeSound,
            SoundSource.BLOCKS,
            (soundType.volume + 1.0f) / 2.0f,
            soundType.pitch * 0.8f
        )
    }
}

fun SafeClientEvent.isSideVisible(
    eyeX: Double,
    eyeY: Double,
    eyeZ: Double,
    blockPos: BlockPos,
    side: Direction,
    assumeAirAsFullBox: Boolean = true
): Boolean {
    fun isFullBox(): Boolean {
        val blockState = world.getBlockState(blockPos)
        return assumeAirAsFullBox && blockState.block == Blocks.AIR
            || blockState.isCollisionShapeFullBlock(world, blockPos)
    }

    return when (side) {
        Direction.DOWN -> {
            eyeY <= blockPos.y
        }
        Direction.UP -> {
            eyeY >= blockPos.y + 1
        }
        Direction.NORTH -> {
            val i = eyeZ.floorToInt()
            i < blockPos.z || i == blockPos.z && isFullBox()
        }
        Direction.SOUTH -> {
            val i = eyeZ.floorToInt()
            i > blockPos.z + 1 || i == blockPos.z + 1 && isFullBox()
        }
        Direction.WEST -> {
            val i = eyeX.floorToInt()
            i < blockPos.x || i == blockPos.x && isFullBox()
        }
        Direction.EAST -> {
            val i = eyeX.floorToInt()
            i > blockPos.x + 1 || i == blockPos.x + 1 && isFullBox()
        }
    }
}

fun PlaceInfo.toBlockHitResult() =
    BlockHitResult(this.hitVec, this.direction, this.pos, false)

fun PlaceInfo.toPlacePacket(hand: InteractionHand, sequence: Int) =
    ServerboundUseItemOnPacket(hand, this.toBlockHitResult(), sequence)
