package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets NoFall and friends flip fields on outgoing movement packets. */
@Mixin(ServerboundMovePlayerPacket.class)
public interface AccessorServerboundMovePlayerPacket {

    @Accessor("onGround")
    @Mutable
    void trollhack$setOnGround(boolean onGround);

    @Accessor("x")
    @Mutable
    void trollhack$setX(double x);

    @Accessor("y")
    @Mutable
    void trollhack$setY(double y);

    @Accessor("z")
    @Mutable
    void trollhack$setZ(double z);

    @Accessor("yRot")
    @Mutable
    void trollhack$setYRot(float yRot);

    @Accessor("xRot")
    @Mutable
    void trollhack$setXRot(float xRot);
}
