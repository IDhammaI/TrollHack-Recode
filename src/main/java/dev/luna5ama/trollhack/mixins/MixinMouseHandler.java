package dev.luna5ama.trollhack.mixins;

import net.minecraft.client.input.MouseButtonInfo;
import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void trollhack$onButton(long handle, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (action == 1) {
            Hooks.onMouseInput(buttonInfo.button(), true);
        } else if (action == 0) {
            Hooks.onMouseInput(buttonInfo.button(), false);
        }
    }
}

