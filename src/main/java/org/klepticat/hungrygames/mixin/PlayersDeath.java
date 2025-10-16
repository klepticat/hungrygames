package org.klepticat.hungrygames.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.klepticat.hungrygames.Hungrygames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class PlayersDeath {
    @Inject(
            method = "onDeath",
            at = @At("HEAD")
    )
    public void addPlayerToFallen(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity _this = (ServerPlayerEntity) (Object) this;

        Hungrygames.fallenManager.addFallen(_this);

        _this.getPassengerList().forEach(Entity::discard);
    }
}
