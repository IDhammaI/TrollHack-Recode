package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface AccessorClientInput {

    @Accessor("moveVector")
    void trollhack$setMoveVector(Vec2 moveVector);
}
