package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MixinPlayer {

    /** PlayerTravelEvent: cancelling freezes vanilla travel physics for the tick. */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void trollhack$travel(Vec3 input, CallbackInfo ci) {
        if ((Object) this instanceof LocalPlayer && Hooks.onPlayerTravel()) {
            ci.cancel();
        }
    }
}
