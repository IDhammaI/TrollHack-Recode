package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OptionInstance.class)
public interface AccessorOptionInstance<T> {

    /** Bypasses validateValue so gamma can exceed vanilla bounds. */
    @Accessor("value")
    void trollhack$forceSetValue(T value);
}
