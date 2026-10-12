package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.module.modules.render.AntiFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void trollhack$antiFog(
        Camera camera,
        int renderDistanceInChunks,
        DeltaTracker deltaTracker,
        float darkenWorldAmount,
        ClientLevel level,
        CallbackInfoReturnable<FogData> cir
    ) {
        if (AntiFog.isModuleEnabled()) {
            FogData fog = cir.getReturnValue();
            fog.environmentalStart = Float.MAX_VALUE;
            fog.environmentalEnd = Float.MAX_VALUE;
            fog.renderDistanceStart = Float.MAX_VALUE;
            fog.renderDistanceEnd = Float.MAX_VALUE;
        }
    }
}
