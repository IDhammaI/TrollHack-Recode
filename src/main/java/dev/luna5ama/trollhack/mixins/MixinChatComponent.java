package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Single funnel for everything the chat displays, so modules can hide or rewrite incoming
 * messages. The 1.12 original edited the chat packet in place, which 26.2 does not allow:
 * both chat packets are records and player chat carries a signature over its content.
 */
@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Unique
    private static final ThreadLocal<Boolean> trollhack$redispatching = ThreadLocal.withInitial(() -> false);

    @Invoker("addMessage")
    abstract void trollhack$addMessage(
        Component contents,
        @Nullable MessageSignature signature,
        GuiMessageSource source,
        @Nullable GuiMessageTag tag
    );

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void trollhack$onAddMessage(
        Component contents,
        @Nullable MessageSignature signature,
        GuiMessageSource source,
        @Nullable GuiMessageTag tag,
        CallbackInfo ci
    ) {
        if (trollhack$redispatching.get()) return;

        Component result = Hooks.onChatMessage(contents);

        if (result == null) {
            ci.cancel();
        } else if (result != contents) {
            // Re-enter with the rewritten text; the guard keeps the hook from seeing it again
            ci.cancel();
            trollhack$redispatching.set(true);
            try {
                trollhack$addMessage(result, signature, source, tag);
            } finally {
                trollhack$redispatching.set(false);
            }
        }
    }

    /** ExtraChatHistory: raise vanilla's hard cap of 100 retained lines. */
    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
    private int trollhack$displayHistoryLimit(int original) {
        return Hooks.chatHistoryLimit(original);
    }

    /** ExtraChatHistory: raise vanilla's hard cap of 100 retained messages. */
    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int trollhack$queueHistoryLimit(int original) {
        return Hooks.chatHistoryLimit(original);
    }
}
