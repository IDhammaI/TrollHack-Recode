package dev.luna5ama.trollhack.module.modules.player

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ProcessKeyBindEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.setting.settings.impl.collection.CollectionSetting
import dev.luna5ama.trollhack.util.BOOLEAN_SUPPLIER_FALSE
import dev.luna5ama.trollhack.util.KeyboardUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.component.AttackRange
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Degradations from the 1.12 version:
 *
 * - 1.12 hooked `Entity.rayTrace` through `MixinEntity`, which replaced *every* ray trace the client
 *   did. Creating new mixins is out of scope here, so the port re-picks on [ProcessKeyBindEvent.Pre]
 *   (`Minecraft.handleKeybinds` head, which vanilla runs right after `Minecraft.pick(1.0F)` inside
 *   `Minecraft.tick`) and overwrites `Minecraft.hitResult`. Consequences:
 *   - Interaction (attack, continue attack, use, pick block) sees the ghosted target, matching 1.12.
 *   - The block outline / crosshair highlight does **not**: `Minecraft.renderFrame` re-runs
 *     `Minecraft.pick` every frame, so the selection box still draws on the ignored block.
 *   - Nothing is overridden while a GUI screen is open, because vanilla skips `handleKeybinds` then.
 *     1.12 kept ghosting there, but no interaction happens with a screen open anyway.
 * - `Block.canCollideCheck(state, stopOnLiquid)` no longer exists. The equivalent on 26.2 is simply
 *   clipping against the outline shape (`ClipContext.Block.OUTLINE`), which returns no hit for shapeless
 *   blocks; fluids are excluded exactly like vanilla's `Entity.pick(range, partialTicks, false)`.
 * - The default block list keeps the same three blocks, remapped to their 26.2 registry names
 *   (`minecraft:portal_frame` -> `minecraft:end_portal_frame`, `minecraft:portal` -> `minecraft:nether_portal`).
 * - 26.2 adds an attack-range pass (spears, `DataComponents.ATTACK_RANGE`) that runs its own un-ghosted
 *   block clip before the normal pick. It has no 1.12 counterpart, so it is kept as-is *unless* it landed
 *   on a block this module ghosts; in that case its result is dropped and the ghosted pick is used, at
 *   the cost of the weapon's extended reach for that one ray. Without this the module would silently do
 *   nothing while such a weapon is held.
 */
internal object GhostHand : Module(
    name = "Ghost Hand",
    description = "Ignores interaction with certain blocks",
    category = Category.PLAYER
) {
    private const val MAX_ATTEMPT = 50
    private const val PARTIAL_TICKS = 1.0f

    private val defaultVisibleList =
        linkedSetOf("minecraft:bedrock", "minecraft:end_portal_frame", "minecraft:nether_portal")

    private val ignoreListed by setting("Ignore Listed", true)
    val blockList = setting(CollectionSetting("Block List", defaultVisibleList, BOOLEAN_SUPPLIER_FALSE))

    init {
        safeListener<ProcessKeyBindEvent.Pre> {
            if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT)
                || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT)
            ) return@safeListener
            if (KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)
                || KeyboardUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)
            ) return@safeListener
            if (KeyboardUtils.isKeyDown(-GLFW.GLFW_MOUSE_BUTTON_RIGHT - 1)) return@safeListener

            val cameraEntity = mc.cameraEntity ?: return@safeListener
            val result = raycastHitResult(cameraEntity, PARTIAL_TICKS)

            mc.hitResult = result
            mc.crosshairPickEntity = (result as? EntityHitResult)?.entity
        }
    }

    /** Mirror of `LocalPlayer.raycastHitResult`, with the block pass filtered by [blockList]. */
    private fun SafeClientEvent.raycastHitResult(cameraEntity: Entity, partialTicks: Float): HitResult {
        val blockInteractionRange = player.blockInteractionRange()
        val itemAttackRange: AttackRange? = player.activeItem.get(DataComponents.ATTACK_RANGE)

        if (itemAttackRange != null) {
            var hitResult = itemAttackRange.getClosesetHit(cameraEntity, partialTicks, EntitySelector.CAN_BE_PICKED)
            if (hitResult is BlockHitResult) {
                hitResult = filterHitResult(hitResult, cameraEntity.getEyePosition(partialTicks), blockInteractionRange)
            }

            val blockHit = hitResult as? BlockHitResult
            val hitGhostedBlock = blockHit != null
                && blockHit.type == HitResult.Type.BLOCK
                && isGhosted(world.getBlockState(blockHit.blockPos))

            if (hitResult.type != HitResult.Type.MISS && !hitGhostedBlock) return hitResult
        }

        return pickGhost(cameraEntity, blockInteractionRange, player.entityInteractionRange(), partialTicks)
    }

    /** Mirror of `LocalPlayer.pick`, but the block ray trace goes through [clipGhost]. */
    private fun SafeClientEvent.pickGhost(
        cameraEntity: Entity,
        blockInteractionRange: Double,
        entityInteractionRange: Double,
        partialTicks: Float
    ): HitResult {
        var maxDistance = max(blockInteractionRange, entityInteractionRange)
        var maxDistanceSq = maxDistance * maxDistance

        val from = cameraEntity.getEyePosition(partialTicks)
        val direction = cameraEntity.getViewVector(partialTicks)

        val blockHitResult = clipGhost(
            from,
            from.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance)
        )
        val blockDistanceSq = blockHitResult.location.distanceToSqr(from)

        if (blockHitResult.type != HitResult.Type.MISS) {
            maxDistanceSq = blockDistanceSq
            maxDistance = sqrt(maxDistanceSq)
        }

        val to = from.add(direction.x * maxDistance, direction.y * maxDistance, direction.z * maxDistance)
        val box = cameraEntity.boundingBox.expandTowards(direction.scale(maxDistance)).inflate(1.0, 1.0, 1.0)
        val entityHitResult = ProjectileUtil.getEntityHitResult(
            cameraEntity,
            from,
            to,
            box,
            EntitySelector.CAN_BE_PICKED,
            maxDistanceSq
        )

        return if (entityHitResult != null && entityHitResult.location.distanceToSqr(from) < blockDistanceSq) {
            filterHitResult(entityHitResult, from, entityInteractionRange)
        } else {
            filterHitResult(blockHitResult, from, blockInteractionRange)
        }
    }

    /** Mirror of `LocalPlayer.filterHitResult`. */
    private fun filterHitResult(hitResult: HitResult, from: Vec3, maxRange: Double): HitResult {
        val location = hitResult.location
        if (location.closerThan(from, maxRange)) return hitResult

        return BlockHitResult.miss(
            location,
            Direction.getApproximateNearest(location.x - from.x, location.y - from.y, location.z - from.z),
            BlockPos.containing(location)
        )
    }

    /**
     * Voxel traversal copied from `BlockGetter.traverseBlocks`, capped at [MAX_ATTEMPT] steps like the
     * 1.12 `rayTrace` helper, with [clipGhosted] deciding whether a block is solid to the ray.
     */
    private fun SafeClientEvent.clipGhost(from: Vec3, to: Vec3): BlockHitResult {
        if (from == to) return miss(from, to)

        val toX = Mth.lerp(-1.0E-7, to.x, from.x)
        val toY = Mth.lerp(-1.0E-7, to.y, from.y)
        val toZ = Mth.lerp(-1.0E-7, to.z, from.z)
        val fromX = Mth.lerp(-1.0E-7, from.x, to.x)
        val fromY = Mth.lerp(-1.0E-7, from.y, to.y)
        val fromZ = Mth.lerp(-1.0E-7, from.z, to.z)

        var currentBlockX = Mth.floor(fromX)
        var currentBlockY = Mth.floor(fromY)
        var currentBlockZ = Mth.floor(fromZ)

        val pos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
        clipGhosted(from, to, pos)?.let { return it }

        val dx = toX - fromX
        val dy = toY - fromY
        val dz = toZ - fromZ

        val signX = Mth.sign(dx)
        val signY = Mth.sign(dy)
        val signZ = Mth.sign(dz)

        val tDeltaX = if (signX == 0) Double.MAX_VALUE else signX / dx
        val tDeltaY = if (signY == 0) Double.MAX_VALUE else signY / dy
        val tDeltaZ = if (signZ == 0) Double.MAX_VALUE else signZ / dz

        var tX = tDeltaX * (if (signX > 0) 1.0 - Mth.frac(fromX) else Mth.frac(fromX))
        var tY = tDeltaY * (if (signY > 0) 1.0 - Mth.frac(fromY) else Mth.frac(fromY))
        var tZ = tDeltaZ * (if (signZ > 0) 1.0 - Mth.frac(fromZ) else Mth.frac(fromZ))

        var attempt = MAX_ATTEMPT

        while ((tX <= 1.0 || tY <= 1.0 || tZ <= 1.0) && attempt-- > 0) {
            if (tX < tY) {
                if (tX < tZ) {
                    currentBlockX += signX
                    tX += tDeltaX
                } else {
                    currentBlockZ += signZ
                    tZ += tDeltaZ
                }
            } else if (tY < tZ) {
                currentBlockY += signY
                tY += tDeltaY
            } else {
                currentBlockZ += signZ
                tZ += tDeltaZ
            }

            clipGhosted(from, to, pos.set(currentBlockX, currentBlockY, currentBlockZ))?.let { return it }
        }

        return miss(from, to)
    }

    /** True when a ray should pass straight through a block in this state. */
    private fun isGhosted(blockState: BlockState): Boolean {
        if (blockState.isAir) return true

        val name = BuiltInRegistries.BLOCK.getKey(blockState.block).toString()
        return ignoreListed == blockList.contains(name)
    }

    /** Null means "the ray passes straight through this block". */
    private fun SafeClientEvent.clipGhosted(from: Vec3, to: Vec3, pos: BlockPos): BlockHitResult? {
        val blockState = world.getBlockState(pos)
        if (isGhosted(blockState)) return null

        val shape = blockState.getShape(world, pos)
        val result = world.clipWithInteractionOverride(from, to, pos, shape, blockState) ?: return null

        return result.withPosition(pos.immutable())
    }

    private fun miss(from: Vec3, to: Vec3): BlockHitResult {
        val delta = from.subtract(to)
        return BlockHitResult.miss(
            to,
            Direction.getApproximateNearest(delta.x, delta.y, delta.z),
            BlockPos.containing(to)
        )
    }
}
