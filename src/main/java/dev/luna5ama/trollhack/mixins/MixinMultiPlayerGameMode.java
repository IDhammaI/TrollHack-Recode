package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Posts the block InteractEvents; 26.2 replacement for the 1.12 MixinPlayerControllerMP.
 * TrollAura (manual override), PacketMine (click to mine) and AutoTool listen to these.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {

    /** 1.12 clickBlock: the first swing at a block. */
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void trollhack$onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.onBlockLeftClick(pos, direction)) {
            cir.setReturnValue(false);
        }
    }

    /** 1.12 onPlayerDamageBlock: held-down mining ticks. */
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void trollhack$onContinueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Hooks.onBlockDamage(pos, direction)) {
            cir.setReturnValue(false);
        }
    }

    /** 1.12 processRightClickBlock. */
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void trollhack$onUseItemOn(
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult blockHit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (Hooks.onBlockRightClick(blockHit.getBlockPos(), blockHit.getDirection(), hand)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
