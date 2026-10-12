package dev.luna5ama.trollhack.mixins;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the end of world rendering, where the world projection matrix and depth
 * buffer are still active (GameRenderer switches to the HUD projection and
 * clears depth right after this method returns).
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Inject(method = "render", at = @At("RETURN"))
    private void trollhack$onWorldRendered(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
                                           boolean renderOutline, CameraRenderState cameraState,
                                           Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor,
                                           boolean shouldRenderSky, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Hooks.onRender3D(cameraState.pos, modelViewMatrix, cameraState.projectionMatrix, partialTick);
    }
}
