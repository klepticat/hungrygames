package org.klepticat.hungrygames.events;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import org.klepticat.hungrygames.api.HungryEvent;
import org.klepticat.hungrygames.data.ServerState;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class EruptionEvent extends HungryEvent {
    BlockPos arenaCenter;
    ServerWorld world;
    Registry<Biome> biomeRegistry;
    LocationPredicate biomePredicate;

    int lastMagmaSpawned;

    public HashSet<DisplayEntity.BlockDisplayEntity> magmaBlocks = new HashSet<>();

    public EruptionEvent(MinecraftServer server) {
        super(server);
    }

    @Override
    public void tick(MinecraftServer server) {
        if(arenaCenter == null) arenaCenter = ServerState.getServerState(server).getArenaCenter().getBlockPos();
        if(world == null) world = server.getWorld(World.OVERWORLD);
        if(biomeRegistry == null) biomeRegistry = server.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
        if(biomePredicate == null) biomePredicate = LocationPredicate.Builder.create()
                .biome(RegistryEntryList.of(biomeRegistry.getOrThrow(BiomeKeys.BADLANDS), biomeRegistry.getOrThrow(BiomeKeys.WOODED_BADLANDS)))
                .build();

        int randomX = world.random.nextBetween(-350, 350);
        int randomZ = world.random.nextBetween(-350, 350);

        if(biomePredicate.test(world, arenaCenter.getX() + randomX, arenaCenter.getY(), arenaCenter.getZ() + randomZ) || lastMagmaSpawned < 0) {
            lastMagmaSpawned = 240;

            DisplayEntity.BlockDisplayEntity newMagma = EntityType.BLOCK_DISPLAY.create(world, SpawnReason.COMMAND);

            newMagma.setBlockState(Blocks.MAGMA_BLOCK.getDefaultState());
            newMagma.setPitch(world.random.nextBetween(0, 180));
            newMagma.setYaw(world.random.nextBetween(0, 180));
            newMagma.setPos(arenaCenter.getX() + randomX, 200, arenaCenter.getZ() + randomZ);
            newMagma.addCommandTag("magma");

            world.spawnEntity(newMagma);

            magmaBlocks.add(newMagma);
        }

        Set<DisplayEntity.BlockDisplayEntity> toRemove = new HashSet<>();
        magmaBlocks.forEach(magmaBlock -> {
            magmaBlock.setPos(magmaBlock.getX(), magmaBlock.getY() - 1, magmaBlock.getZ());

            if(magmaBlock.getBlockStateAtPos() != Blocks.AIR.getDefaultState()) {
                world.createExplosion(
                        null,
                        null,
                        new AdvancedExplosionBehavior(false, true, Optional.of(4.0f), Optional.empty()),
                        magmaBlock.getX(),
                        magmaBlock.getY(),
                        magmaBlock.getZ(),
                        5,
                        true,
                        World.ExplosionSourceType.TRIGGER
                );

                toRemove.add(magmaBlock);
                magmaBlock.discard();
            }
        });

        toRemove.forEach(magmaBlock -> magmaBlocks.remove(magmaBlock));

        if(lastMagmaSpawned > 0) lastMagmaSpawned--;
    }

    @Override
    public void init(MinecraftServer server) {

    }
}
