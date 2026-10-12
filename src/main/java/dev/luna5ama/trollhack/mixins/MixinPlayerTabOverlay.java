package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * ExtraTab: 26.2 caps the tab list inside getPlayerInfos with a stream limit
 * rather than the sublist the 1.12 version replaced.
 */
@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @ModifyConstant(method = "getPlayerInfos", constant = @Constant(longValue = 80L))
    private long trollhack$tabSize(long original) {
        return Hooks.tabListLimit(original);
    }
}
