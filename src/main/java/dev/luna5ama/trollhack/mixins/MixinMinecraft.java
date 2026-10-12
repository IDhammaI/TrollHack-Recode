package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Inject(method = "getTickTargetMillis", at = @At("RETURN"), cancellable = true)
    private void trollhack$modifyTickTarget(float defaultTickTargetMillis, CallbackInfoReturnable<Float> cir) {
        float override = Hooks.overrideTickLength(cir.getReturnValueF());
        if (override != cir.getReturnValueF()) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void trollhack$runTickStart(boolean advanceGameTime, CallbackInfo ci) {
        Hooks.onRunTickStart();
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void trollhack$runTickEnd(boolean advanceGameTime, CallbackInfo ci) {
        Hooks.onRunTickEnd();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void trollhack$tickPre(CallbackInfo ci) {
        Hooks.onTickPre();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void trollhack$tickPost(CallbackInfo ci) {
        Hooks.onTickPost();
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void trollhack$keyBindsPre(CallbackInfo ci) {
        Hooks.onProcessKeyBindsPre();
    }

    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void trollhack$keyBindsPost(CallbackInfo ci) {
        Hooks.onProcessKeyBindsPost();
    }

    @Inject(method = "setLevel", at = @At("TAIL"))
    private void trollhack$worldLoad(ClientLevel level, CallbackInfo ci) {
        Hooks.onWorldLoad();
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"))
    private void trollhack$worldUnload(Component message, CallbackInfo ci) {
        Hooks.onWorldUnload();
    }
}
