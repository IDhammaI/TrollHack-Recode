package dev.luna5ama.trollhack.mixins.accessor;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface AccessorLivingEntity {

    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> trollhack$getDataHealthId() {
        throw new AssertionError();
    }
}
