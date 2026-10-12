package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires WorldEvent.ClientBlockUpdate; 26.2 has no IWorldEventListener, so this
 * replaces the old WorldManager's notifyBlockUpdate hook.
 */
@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @Inject(method = "sendBlockUpdated", at = @At("HEAD"))
    private void trollhack$onBlockUpdated(
        BlockPos pos,
        BlockState old,
        BlockState current,
        int updateFlags,
        CallbackInfo ci
    ) {
        Hooks.onClientBlockUpdate(pos, old, current);
    }
}
