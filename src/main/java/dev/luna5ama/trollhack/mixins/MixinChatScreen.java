package dev.luna5ama.trollhack.mixins;

import dev.luna5ama.trollhack.core.Hooks;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts submitted chat lines for client commands.
 *
 * <p>The injection point sits after {@code ChatComponent.addRecentChat} so commands
 * stay in the player's chat history (arrow-up recall) but never reach the server.</p>
 */
@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Inject(
        method = "handleChatInput(Ljava/lang/String;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent;addRecentChat(Ljava/lang/String;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void trollhack$handleCommand(String message, boolean addToHistory, CallbackInfo ci) {
        if (Hooks.onChatInput(message)) {
            ci.cancel();
        }
    }
}
