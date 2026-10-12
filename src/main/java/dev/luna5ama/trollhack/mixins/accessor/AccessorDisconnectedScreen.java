package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisconnectedScreen.class)
public interface AccessorDisconnectedScreen {

    @Accessor("parent")
    Screen trollhack$getParent();

    @Accessor("details")
    DisconnectionDetails trollhack$getDetails();
}
