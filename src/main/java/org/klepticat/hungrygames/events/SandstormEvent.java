package org.klepticat.hungrygames.events;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import org.klepticat.hungrygames.api.HungryEvent;
import org.klepticat.hungrygames.data.ServerState;

import java.util.HashSet;
import java.util.Set;

public class SandstormEvent extends HungryEvent {
    Set<ServerPlayerEntity> trackedPlayers;
    Set<ServerPlayerEntity> untrackedPlayers;

    public SandstormEvent(MinecraftServer server) {
        super(server);
    }

    @Override
    public void tick(MinecraftServer server) {
        Registry<Biome> biomeRegistry = server.getRegistryManager().getOrThrow(RegistryKeys.BIOME);

        LocationPredicate biomePredicate = LocationPredicate.Builder.create()
                .biome(RegistryEntryList.of(biomeRegistry.getOrThrow(BiomeKeys.DESERT)))
                .build();

        Set<ServerPlayerEntity> playersToRemove = new HashSet<>();
        trackedPlayers.forEach(player -> {
            if(!biomePredicate.test(player.getEntityWorld(), player.getX(), player.getY(), player.getZ())) {
                playersToRemove.add(player);
            }

            player.addVelocity(0.005f, 0.0f, 0.002f);
            player.velocityModified = true;
        });

        playersToRemove.forEach(player -> {
            trackedPlayers.remove(player);
            untrackedPlayers.add(player);
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
        });
    }

    @Override
    public void init(MinecraftServer server) {
        trackedPlayers = new HashSet<>();
        untrackedPlayers = new HashSet<>();

        server.getPlayerManager().getPlayerList().forEach(player -> {
            if(ServerState.getPlayerState(player).getDistrict() == 0) return;

            RegistryEntry<Biome> biome = player.getEntityWorld().getBiome(player.getBlockPos());
            if(biome.matchesKey(BiomeKeys.DESERT)) {
                trackedPlayers.add(player);
            } else {
                untrackedPlayers.add(player);
            }
        });
    }
}
