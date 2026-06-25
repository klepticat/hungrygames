package org.klepticat.hungrygames.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.klepticat.hungrygames.Hungrygames;
import org.klepticat.hungrygames.data.ServerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class PlayersDeathAndDamage {
    @Inject(
            method = "onDeath",
            at = @At("HEAD")
    )
    public void addPlayerToFallen(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity _this = (ServerPlayerEntity) (Object) this;

        Hungrygames.fallenManager.addFallen(_this);

        _this.getEntityWorld().getPlayers(player -> ServerState.getPlayerState(player).getDistrict() == 0).forEach(player -> {
            player.sendMessage(Text.literal("%s has died".formatted(_this.getStringifiedName())).formatted(Formatting.BOLD, Formatting.RED));
        });

        _this.getPassengerList().forEach(Entity::discard);
    }

    @Inject(
            method = "damage",
            at = @At("HEAD")
    )
    public void logPlayerDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity _this = (ServerPlayerEntity) (Object) this;

        _this.getEntityWorld().getPlayers(player -> ServerState.getPlayerState(player).getDistrict() == 0).forEach(player -> {
            player.sendMessage(Text.literal("%s took %s damage from %s".formatted(_this.getStringifiedName(), amount, source.getName())).formatted(Formatting.BOLD, Formatting.RED));
        });
    }
}
