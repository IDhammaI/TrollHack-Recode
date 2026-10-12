package dev.luna5ama.trollhack.mixins;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.luna5ama.trollhack.module.modules.misc.UnfocusedFps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class MixinFramerateLimitTracker {

    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void trollhack$unfocusedLimit(CallbackInfoReturnable<Integer> cir) {
        int limit = UnfocusedFps.getUnfocusedLimit();
        if (limit > 0 && limit < cir.getReturnValue()) {
            cir.setReturnValue(limit);
        }
    }
}
