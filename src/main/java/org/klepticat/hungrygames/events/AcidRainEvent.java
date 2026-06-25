package org.klepticat.hungrygames.events;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.EntityGameEventHandler;
import org.klepticat.hungrygames.api.HungryEvent;
import org.klepticat.hungrygames.data.ServerState;

import java.util.HashSet;
import java.util.Set;

public class AcidRainEvent extends HungryEvent {
    Set<ServerPlayerEntity> trackedPlayers;
    Set<ServerPlayerEntity> untrackedPlayers;

    public AcidRainEvent(MinecraftServer server) {
        super(server);
    }

    @Override
    public void tick(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.getRegistryManager().getOrThrow(RegistryKeys.BIOME);

        LocationPredicate skyPredicate = LocationPredicate.Builder.create()
                .canSeeSky(true)
                .build();

        LocationPredicate biomePredicate = LocationPredicate.Builder.create()
                .biome(RegistryEntryList.of(biomeRegistry.getOrThrow(BiomeKeys.JUNGLE), biomeRegistry.getOrThrow(BiomeKeys.BAMBOO_JUNGLE)))
                .build();

        Set<ServerPlayerEntity> playersToRemove = new HashSet<>();
        trackedPlayers.forEach(player -> {
            if(!biomePredicate.test(player.getEntityWorld(), player.getX(), player.getY(), player.getZ())) {
                playersToRemove.add(player);
            } else if(skyPredicate.test(player.getEntityWorld(), player.getX(), player.getY(), player.getZ()) && !player.hasStatusEffect(StatusEffects.POISON)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 1));
            }
        });

        playersToRemove.forEach(player -> {
            trackedPlayers.remove(player);
            untrackedPlayers.add(player);

            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0));
        });

        Set<ServerPlayerEntity> playersToAdd = new HashSet<>();
        untrackedPlayers.forEach(player -> {
            if(biomePredicate.test(player.getEntityWorld(), player.getX(), player.getY(), player.getZ())) {
                playersToAdd.add(player);
            }
        });

        playersToAdd.forEach(player -> {
            untrackedPlayers.remove(player);
            trackedPlayers.add(player);

            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0));
        });
    }

    @Override
    public void init(MinecraftServer server) {
        trackedPlayers = new HashSet<>();
        untrackedPlayers = new HashSet<>();

        server.getPlayerManager().getPlayerList().forEach(player -> {
            if(ServerState.getPlayerState(player).getDistrict() == 0) return;

            RegistryEntry<Biome> biome = player.getEntityWorld().getBiome(player.getBlockPos());
            if(biome.matchesKey(BiomeKeys.JUNGLE) || biome.matchesKey(BiomeKeys.BAMBOO_JUNGLE)) {
                trackedPlayers.add(player);
                player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STOPPED, 0));
            } else {
                untrackedPlayers.add(player);
            }
        });
    }
}
