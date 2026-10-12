package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientCommonPacketListenerImpl.class)
public interface AccessorClientCommonPacketListener {

    @Accessor("serverBrand")
    String trollhack$getServerBrand();
}
