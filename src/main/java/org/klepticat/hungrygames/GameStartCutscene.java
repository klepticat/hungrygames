package org.klepticat.hungrygames;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import org.jetbrains.annotations.Nullable;
import org.klepticat.hungrygames.data.ServerState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GameStartCutscene {
    @Nullable
    public static GameStartCutscene instance;

    private final MinecraftServer server;
    private final ServerWorld world;
    private long ticksToPodium = 1200;
    private boolean podiumRising = false;
    private boolean podiumRisen = false;
    private final long startingTicks = 1200;
    private long countdownTicks = startingTicks;

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

                DisplayEntity.BlockDisplayEntity platformBlock = EntityType.BLOCK_DISPLAY.create(instance.world, SpawnReason.MOB_SUMMONED);

                platformBlock.setPosition(Vec3d.of(platform));

                platformBlock.setYaw(0);
                platformBlock.setPitch(0);

                instance.world.spawnNewEntityAndPassengers(platformBlock);

                instance.blocks.put(podiumPos, platformBlock);

                platformBlock.setBlockState(Blocks.SMOOTH_STONE.getDefaultState());

                instance.world.setBlockState(piston, Blocks.AIR.getDefaultState());

                instance.world.setBlockState(glass1, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
                instance.world.setBlockState(glass2, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());
                instance.world.setBlockState(glass3, Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState());

                instance.world.setBlockState(platform, Blocks.AIR.getDefaultState());
            });

            instance.podiumRising = true;
        }

        if(instance.podiumRising) {
            instance.blocks.forEach((podiumPos, podiumBlock) -> {
                podiumBlock.setPos(podiumBlock.getX(), podiumBlock.getY() + 0.045, podiumBlock.getZ());

                PlayerEntity player = instance.world.getClosestPlayer(podiumBlock, 3);

                if(player != null)  {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 3, 0, true, false));
                }

                if(podiumBlock.getY() - 23.1 > podiumPos.getY()) {
                    instance.podiumRisen = true;
                    instance.podiumRising = false;

                    instance.world.setBlockState(podiumPos.add(0, 23, 0), Blocks.SMOOTH_STONE.getDefaultState());

                    if(player != null) {
                        player.clearStatusEffects();
                        player.heal(50);
                        player.getHungerManager().setSaturationLevel(0);
                        player.getHungerManager().setFoodLevel(20);

                        ((ServerPlayerEntity) player).networkHandler.sendPacket(new TitleS2CPacket(Text.literal("60").formatted(Formatting.BOLD, Formatting.GOLD)));
                        ((ServerPlayerEntity) player).networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(Identifier.of("hungrygames", "countdown.tick"))), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 1L));

                        player.updatePosition(podiumPos.toCenterPos().x, podiumPos.getY() + 24, podiumPos.toCenterPos().z);
                    }

                    podiumBlock.getPassengerList().forEach(Entity::discard);
                    podiumBlock.discard();
                }
            });
        }

        if(instance.podiumRisen && --instance.countdownTicks > 0L) {
            instance.server.getPlayerManager().getPlayerList().forEach(player -> {
                if(instance.countdownTicks % 20 == 0) {
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("%s".formatted(instance.countdownTicks / 20)).formatted(Formatting.BOLD, Formatting.GOLD)));
                    player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(Identifier.of("hungrygames", "countdown.tick"))), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 1L));
                }

                if(player.getBlockY() <= 65 && instance.startingTicks - instance.countdownTicks > 20 && player.getCommandTags().contains("alive")) {
                    player.getEntityWorld().createExplosion(
                            null,
                            null,
                            new AdvancedExplosionBehavior(false, false, Optional.of(5.0f), Optional.empty()),
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            5,
                            false,
                            World.ExplosionSourceType.TRIGGER
                    );

                    player.getInventory().clear();
                    player.setExperiencePoints(0);
                    player.setExperienceLevel(0);
                    player.removeCommandTag("alive");
                    player.kill(player.getEntityWorld());
                }
            });
        } else if(instance.podiumRisen && instance.countdownTicks == 0L) {
            instance.server.getPlayerManager().getPlayerList().forEach(player -> {
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("BEGIN").formatted(Formatting.BOLD, Formatting.GOLD)));
                player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(Identifier.of("hungrygames", "countdown.end"))), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 1L));

                if(player.getCommandTags().contains("alive")) {
                    player.changeGameMode(GameMode.SURVIVAL);
                    player.clearStatusEffects();
                    player.getInventory().clear();
                }
            });
        }
    }
}
