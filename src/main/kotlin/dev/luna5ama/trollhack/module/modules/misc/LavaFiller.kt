package dev.luna5ama.trollhack.module.modules.misc

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.safeConcurrentListener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.collections.removeBy
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.inventory.findBestTool
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.math.VectorUtils
import dev.luna5ama.trollhack.util.math.vector.distanceSqToCenter
import dev.luna5ama.trollhack.util.math.vector.distanceToCenter
import dev.luna5ama.trollhack.util.threads.runSynchronized
import dev.luna5ama.trollhack.util.world.PlaceInfo
import dev.luna5ama.trollhack.util.world.PlacementSearchOption
import dev.luna5ama.trollhack.util.world.getBlock
import dev.luna5ama.trollhack.util.world.getHitVec
import dev.luna5ama.trollhack.util.world.getHitVecOffset
import dev.luna5ama.trollhack.util.world.getClosestSide
import dev.luna5ama.trollhack.util.world.getPlacement
import dev.luna5ama.trollhack.util.world.placeBlock
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LiquidBlock

internal object LavaFiller : Module(
    name = "Lava Filler",
    description = "Fills up lava with netherack",
    category = Category.MISC,
    modulePriority = 100
) {
    private val placeDelay by setting("Place Delay", 200, 0..1000, 10)
    private val mineDelay by setting("Mine Delay", 100, 0..1000, 10)
    private val cleanDelay by setting("Clean Delay", 3000, 0..10000, 100)
    private val placeTimeout by setting("Place Timeout", 1000, 0..10000, 100)
    private val mineTimeout by setting("Mine Timeout", 1000, 0..10000, 100)
    private val range by setting("Range", 4.0f, 1.0f..8.0f, 0.1f)

    private val placeTimer = TickTimer()
    private val mineTimer = TickTimer()
    private val placeTimeoutMap = Long2LongOpenHashMap().synchronized()
    private val mineTimeoutMap = Long2LongOpenHashMap().synchronized()

    init {
        placeTimeoutMap.defaultReturnValue(0L)
        mineTimeoutMap.defaultReturnValue(0L)

        onDisable {
            placeTimeoutMap.clear()
            mineTimeoutMap.clear()
        }

        safeListener<WorldEvent.ClientBlockUpdate> {
            if (it.newState.block == Blocks.AIR) {
                placeTimeoutMap.remove(it.pos.asLong())
            } else if (it.newState.block == Blocks.NETHERRACK) {
                val time = placeTimeoutMap.remove(it.pos.asLong())
                if (time != 0L) {
                    mineTimeoutMap.put(it.pos.asLong(), System.currentTimeMillis() + cleanDelay)
                }
            }
        }

        safeConcurrentListener<RunGameLoopEvent.Tick> {
            place()
            mine()
        }
    }

    private fun SafeClientEvent.mine() {
        if (!mineTimer.tickAndReset(mineDelay)) return
        val toolSlot = findBestTool(Blocks.NETHERRACK.defaultBlockState()) ?: return

        val currentTime = System.currentTimeMillis()
        mineTimeoutMap.runSynchronized {
            values.removeBy {
                it < currentTime
            }
        }

        val pos = VectorUtils.getBlockPosInSphere(player, range)
            .filterNot { mineTimeoutMap.containsKey(it.asLong()) }
            .filter { world.getBlock(it) == Blocks.NETHERRACK }
            .minByOrNull { player.distanceSqToCenter(it) } ?: return

        mineTimeoutMap.put(pos.asLong(), currentTime + mineTimeout)

        val side = getClosestSide(pos)

        ghostSwitch(toolSlot) {
            connection.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    pos,
                    side
                )
            )
            connection.send(
                ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pos,
                    side
                )
            )
        }
    }

    private fun SafeClientEvent.place() {
        if (!placeTimer.tickAndReset(placeDelay)) return

        val fillSlot = player.allSlotsPrioritized.firstBlock(Blocks.NETHERRACK) ?: return
        val currentTime = System.currentTimeMillis()
        placeTimeoutMap.runSynchronized {
            values.removeBy {
                it < currentTime
            }
        }

        val pos = VectorUtils.getBlockPosInSphere(player, range)
            .filterNot {
                placeTimeoutMap.containsKey(it.asLong())
            }
            .filter {
                val blockState = world.getBlockState(it)
                blockState.block == Blocks.LAVA && blockState.getValue(LiquidBlock.LEVEL) == 0
            }
            .maxWithOrNull(
                compareBy<BlockPos> {
                    getPlacement(
                        it,
                        1,
                        PlacementSearchOption.range(range.toDouble()),
                        PlacementSearchOption.ENTITY_COLLISION
                    ) != null
                }.thenBy {
                    player.distanceSqToCenter(it)
                }
            ) ?: return

        val directPlace = getPlacement(
            pos,
            1,
            PlacementSearchOption.range(range.toDouble()),
            PlacementSearchOption.ENTITY_COLLISION
        )

        if (directPlace != null) {
            placeBlock(directPlace, fillSlot)
            return
        }

        val posUp = pos.above()

        val placeInfo1 = PlaceInfo(
            pos,
            Direction.UP,
            player.distanceToCenter(pos),
            getHitVecOffset(Direction.UP),
            getHitVec(pos, Direction.UP),
            posUp
        )

        val placeInfo2 = PlaceInfo(
            posUp,
            Direction.DOWN,
            player.distanceToCenter(posUp),
            getHitVecOffset(Direction.DOWN),
            getHitVec(posUp, Direction.DOWN),
            pos
        )

        placeBlock(placeInfo1, fillSlot)
        placeBlock(placeInfo2, fillSlot)

        placeTimer.reset(placeDelay.toLong())

        placeTimeoutMap.put(pos.asLong(), currentTime + placeTimeout)
    }
}
