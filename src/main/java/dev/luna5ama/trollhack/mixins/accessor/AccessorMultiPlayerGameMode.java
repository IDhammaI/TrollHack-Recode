package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface AccessorMultiPlayerGameMode {

    /** Flushes the selected hotbar slot to the server if it changed. */
    @Invoker("ensureHasSentCarriedItem")
    void trollhack$syncCurrentPlayItem();

    @Accessor("destroyDelay")
    int trollhack$getDestroyDelay();

    @Accessor("destroyDelay")
    void trollhack$setDestroyDelay(int delay);
}
