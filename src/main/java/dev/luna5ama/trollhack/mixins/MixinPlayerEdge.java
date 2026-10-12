package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.module.modules.movement.SafeWalk;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MixinPlayerEdge {

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void trollhack$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.shouldSafewalk((Player) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
