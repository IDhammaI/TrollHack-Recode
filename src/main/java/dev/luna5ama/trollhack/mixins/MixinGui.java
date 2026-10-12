package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jspecify.annotations.Nullable;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Shadow
    private @Nullable Screen screen;

    /** Posts GuiEvent.Displayed/Closed and lets listeners swap the screen being opened. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen trollhack$onSetScreen(@Nullable Screen newScreen) {
        Screen prev = this.screen;
        if (prev != null && prev != newScreen) {
            Hooks.onScreenClosed(prev);
        }
        return Hooks.onScreenDisplayed(newScreen);
    }

    /** Injects the TrollHack HUD into the 26.2 extract-then-render GUI pipeline. */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void trollhack$extractHud(DeltaTracker deltaTracker, boolean renderLevel, boolean isGameLoaded, CallbackInfo ci) {
        Hooks.onRender2D(this.guiRenderState, deltaTracker.getGameTimeDeltaPartialTick(true));
    }
}
