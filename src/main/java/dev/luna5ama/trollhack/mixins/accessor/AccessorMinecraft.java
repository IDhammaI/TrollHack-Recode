package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** FastPlace zeroes the right click delay; AutoClicker triggers vanilla attack/use. */
@Mixin(Minecraft.class)
public interface AccessorMinecraft {

    @Accessor("rightClickDelay")
    int trollhack$getRightClickDelay();

    @Accessor("rightClickDelay")
    void trollhack$setRightClickDelay(int delay);

    @Invoker("startAttack")
    boolean trollhack$startAttack();

    @Invoker("startUseItem")
    void trollhack$startUseItem();

    @Invoker("continueAttack")
    void trollhack$continueAttack(boolean down);
}
