package org.klepticat.hungrygames.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class PlayersRideable {
    @Redirect(
            method = "startRiding(Lnet/minecraft/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isSaveable()Z")
    )
    boolean alwaysSaveable(EntityType instance) {
        return instance == EntityType.PLAYER || instance.isSaveable();
    }
}
