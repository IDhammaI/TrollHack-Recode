package dev.luna5ama.trollhack.mixins;

import net.minecraft.client.input.KeyEvent;
import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void trollhack$onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        // action: 0 = release, 1 = press, 2 = repeat
        if (action == 1) {
            Hooks.onKeyboardInput(event.key(), true);
        } else if (action == 0) {
            Hooks.onKeyboardInput(event.key(), false);
        }
    }
}

