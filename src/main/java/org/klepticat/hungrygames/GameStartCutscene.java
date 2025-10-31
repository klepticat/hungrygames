package org.klepticat.hungrygames;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.klepticat.hungrygames.data.ServerState;

import java.util.HashMap;
import java.util.Map;

public class GameStartCutscene {
    @Nullable
    public static GameStartCutscene instance;

    private final MinecraftServer server;
    private final ServerWorld world;
    private long ticksToPodium = 400;
    private boolean podiumRising = false;
    private boolean podiumRisen = false;
    private final long countdownTicks = 1200;

    private Map<BlockPos, DisplayEntity.BlockDisplayEntity> blocks = new HashMap<>();

    public GameStartCutscene(MinecraftServer server) {
        this.server = server;
        this.world = server.getWorld(World.OVERWORLD);
    }

    public static void tick() {
        if(instance == null) return;

        if(--instance.ticksToPodium == 0L) {
            ServerState.getServerState(instance.server).getPodiums().values().forEach(podiumPos -> {
                BlockPos piston = podiumPos.add(2, 4, 0);
                BlockPos glass1 = podiumPos.add(1, 0, 1);
                BlockPos glass2 = podiumPos.add(1, 1, 1);
                BlockPos glass3 = podiumPos.add(1, 2, 1);
                BlockPos platform = podiumPos.add(0, -1, 0);

                ShulkerEntity platformShulker = EntityType.SHULKER.spawn(instance.world, platform, SpawnReason.MOB_SUMMONED);
                DisplayEntity.BlockDisplayEntity platformBlock = EntityType.BLOCK_DISPLAY.spawn(instance.world, platform, SpawnReason.MOB_SUMMONED);

                instance.blocks.put(podiumPos, platformBlock);

                platformShulker.startRiding(platformBlock);

                platformShulker.setAiDisabled(true);
                platformBlock.setBlockState(Blocks.SMOOTH_STONE.getDefaultState());

                instance.world.setBlockState(piston, Blocks.AIR.getDefaultState());

                instance.world.setBlockState(glass1, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
                instance.world.setBlockState(glass2, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
                instance.world.setBlockState(glass3, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
            });

            instance.podiumRising = true;
        }

        if(instance.podiumRising) {
            instance.blocks.forEach((podiumPos, podiumBlock) -> {
                podiumBlock.setPos(podiumBlock.getX(), podiumBlock.getY() + 0.2, podiumBlock.getZ());

                if(podiumBlock.getY() - 24 > podiumPos.getY()) {
                    instance.podiumRisen = true;
                    instance.podiumRising = false;

                    instance.world.setBlockState(podiumPos.add(0, 24, 0), Blocks.SMOOTH_STONE.getDefaultState());

                    podiumBlock.getPassengerList().forEach(Entity::discard);
                    podiumBlock.discard();
                }
            });
        }
    }
}
