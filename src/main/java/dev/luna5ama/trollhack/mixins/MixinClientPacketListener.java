package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void trollhack$onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        Hooks.onConnect();
    }

    /**
     * Routes chat the player typed through MessageManager's queue. Cancelling here rather than at
     * the packet keeps signing correct: the message is signed inside sendChat, so it has to be
     * final before this method runs.
     */
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void trollhack$onSendChat(String content, CallbackInfo ci) {
        if (Hooks.onSendChat(content)) {
            ci.cancel();
        }
    }
}
