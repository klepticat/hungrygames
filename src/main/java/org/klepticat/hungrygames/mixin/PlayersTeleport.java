package org.klepticat.hungrygames.mixin;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import org.klepticat.hungrygames.api.LocationPosition;
import org.klepticat.hungrygames.data.ServerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerWorld.class)
public class PlayersTeleport {
    @Inject(at = @At("TAIL"), method = "onPlayerConnected")
    public void teleportPlayerOnConnect(ServerPlayerEntity player, CallbackInfo callbackInfo) {
        ServerWorld _this = (ServerWorld) (Object) this;

        ServerState serverState = ServerState.getServerState(_this.getServer());
        ServerState.GameState gameState = serverState.getGameState();

        boolean trainingCenterActive = gameState == ServerState.GameState.NONE || gameState == ServerState.GameState.PRESENTATION;
        boolean isTribute = ServerState.getPlayerState(player).getDistrict() != 0;

        if(trainingCenterActive && isTribute) {
            teleportPlayerCenter(player, _this,serverState.getTrainingSpawn());

            player.getInventory().clear();
            player.clearStatusEffects();

            player.changeGameMode(GameMode.ADVENTURE);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
        } else if(gameState == ServerState.GameState.INTERVIEWS) {
            if(!isTribute) {
                teleportPlayer(player, _this, serverState.getInterviewWaiting());

                player.changeGameMode(GameMode.CREATIVE);

            } else {
                teleportPlayer(player, _this, serverState.getInterviewSpawn());

                player.changeGameMode(GameMode.ADVENTURE);

            }
            player.getInventory().clear();
            player.clearStatusEffects();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
        }
    }

    @Inject(at = @At("TAIL"), method = "onPlayerRespawned")
    public void teleportPlayerOnRespawn(ServerPlayerEntity player, CallbackInfo callbackInfo) {
        ServerWorld _this = (ServerWorld) (Object) this;

        ServerState serverState = ServerState.getServerState(_this.getServer());
        ServerState.GameState gameState = serverState.getGameState();

        boolean trainingCenterActive = gameState == ServerState.GameState.NONE || gameState == ServerState.GameState.PRESENTATION;
        boolean isTribute = ServerState.getPlayerState(player).getDistrict() != 0;

        if(trainingCenterActive && isTribute) {
            teleportPlayerCenter(player, _this,serverState.getTrainingSpawn());

            player.getInventory().clear();
            player.clearStatusEffects();

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
        } else if(gameState == ServerState.GameState.GAME_ACTIVE) {
            teleportPlayer(player, _this,serverState.getArenaCenter());
            _this.getServer().execute(() -> _this.getServer().execute(() -> player.changeGameMode(GameMode.SPECTATOR)));
            player.removeCommandTag("alive");
        } else if(gameState == ServerState.GameState.INTERVIEWS) {
            if(!isTribute) {
                teleportPlayer(player, _this, serverState.getInterviewWaiting());

                player.changeGameMode(GameMode.CREATIVE);

            } else {
                teleportPlayer(player, _this, serverState.getInterviewSpawn());

                player.changeGameMode(GameMode.ADVENTURE);

            }
            player.getInventory().clear();
            player.clearStatusEffects();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
        }
    }

    @Unique
    private static void teleportPlayer(ServerPlayerEntity player, ServerWorld world, LocationPosition location) {
        player.teleport(world, location.x(), location.y(), location.z(), Set.of(), location.yaw(), location.pitch(), true);
    }

    @Unique
    private static void teleportPlayerCenter(ServerPlayerEntity player, ServerWorld world, LocationPosition location) {
        player.teleport(world, location.centerX(), location.y(), location.centerZ(), Set.of(), location.yaw(), location.pitch(), true);
    }
}
