package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

/**
 * Posts send/receive packet events; cancelling an event swallows the packet.
 */
@Mixin(Connection.class)
public abstract class MixinConnection {

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void trollhack$onReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Iterator<Packet<? super ClientGamePacketListener>> it = bundle.subPackets().iterator(); it.hasNext(); ) {
                if (Hooks.onPacketReceive(it.next())) {
                    it.remove();
                }
            }
        } else {
            if (Hooks.onPacketReceive(packet)) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("TAIL")
    )
    private void trollhack$onPostReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        Hooks.onPacketPostReceive(packet);
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollhack$onSend(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        if (Hooks.onPacketSend(packet)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
        at = @At("TAIL")
    )
    private void trollhack$onPostSend(Packet<?> packet, @Nullable ChannelFutureListener listener, CallbackInfo ci) {
        Hooks.onPacketPostSend(packet);
    }
}
