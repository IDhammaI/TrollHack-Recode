package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface AccessorClientLevel {

    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler trollhack$getBlockStatePredictionHandler();
}
