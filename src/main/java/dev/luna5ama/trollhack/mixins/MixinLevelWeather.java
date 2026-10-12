package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.module.modules.misc.AntiWeather;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevelWeather {

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void trollhack$noRain(float a, CallbackInfoReturnable<Float> cir) {
        if (AntiWeather.isModuleEnabled()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void trollhack$noThunder(float a, CallbackInfoReturnable<Float> cir) {
        if (AntiWeather.isModuleEnabled()) {
            cir.setReturnValue(0.0F);
        }
    }
}
