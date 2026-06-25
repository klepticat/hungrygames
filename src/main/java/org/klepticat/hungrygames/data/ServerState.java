package org.klepticat.hungrygames.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import org.klepticat.hungrygames.GameStartCutscene;
import org.klepticat.hungrygames.api.LocationPosition;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ServerState extends PersistentState {
    private HashMap<UUID, PlayerData> players;

    private HashMap<String, BlockPos> podiums;

    private LocationPosition arenaCenter;
    private LocationPosition interviewWaiting;
    private LocationPosition interviewSpawn;
    private LocationPosition trainingSpawn;

    private GameState currentGameState;

    public static ServerState create() {
        ServerState state = new ServerState();
        state.players = new HashMap<>();
        state.podiums = new HashMap<>();
        state.arenaCenter = new LocationPosition(new BlockPos(0, 0, 0), 0, 0);
        state.interviewWaiting = new LocationPosition(new BlockPos(0, 0, 0), 0, 0);
        state.interviewSpawn = new LocationPosition(new BlockPos(0, 0, 0), 0, 0);
        state.trainingSpawn = new LocationPosition(new BlockPos(0, 0, 0), 0, 0);
        state.currentGameState = GameState.NONE;

        return state;
    }

    public static ServerState createFromCodec(
            Map<UUID, PlayerData> players,
            Map<String, BlockPos> podiums,
            LocationPosition arenaCenter,
            LocationPosition interviewWaiting,
            LocationPosition interviewSpawn,
            LocationPosition trainingSpawn,
            GameState gameState
    ) {
        ServerState state = new ServerState();

        state.players = new HashMap<>(players);

        state.podiums = new HashMap<>(podiums);

        state.arenaCenter = arenaCenter;
        state.interviewWaiting = interviewWaiting;
        state.interviewSpawn = interviewSpawn;
        state.trainingSpawn = trainingSpawn;

        state.currentGameState = gameState;

        return state;
    }

    public static final Codec<ServerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Uuids.CODEC, PlayerData.CODEC).optionalFieldOf("players", Map.of()).forGetter(ServerState::getPlayers),
            Codec.unboundedMap(Codec.STRING, BlockPos.CODEC).optionalFieldOf("podiums", Map.of()).forGetter(ServerState::getPodiums),
            LocationPosition.CODEC.optionalFieldOf("arena_center", new LocationPosition(new BlockPos(0, 0, 0), 0, 0)).forGetter(ServerState::getArenaCenter),
            LocationPosition.CODEC.optionalFieldOf("interview_wait", new LocationPosition(new BlockPos(0, 0, 0), 0, 0)).forGetter(ServerState::getInterviewWaiting),
            LocationPosition.CODEC.optionalFieldOf("interview_spawn", new LocationPosition(new BlockPos(0, 0, 0), 0, 0)).forGetter(ServerState::getInterviewSpawn),
            LocationPosition.CODEC.optionalFieldOf("training_spawn", new LocationPosition(new BlockPos(0, 0, 0), 0, 0)).forGetter(ServerState::getTrainingSpawn),
            GameState.CODEC.optionalFieldOf("game_state", GameState.NONE).forGetter(ServerState::getGameState)
    ).apply(instance, ServerState::createFromCodec));

    public static final PersistentStateType<ServerState> TYPE = new PersistentStateType<>(
            "hungry_server_state",
            context -> ServerState.create(),
            context -> CODEC,
            null
    );

    public static ServerState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        ServerState state = persistentStateManager.getOrCreate(TYPE);

        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        ServerState state = getServerState(player.getEntityWorld().getServer());

        PlayerData playerData = state.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData((byte) 0, (byte) -1));

        return playerData;
    }

    public static PlayerData getPlayerState(UUID player, MinecraftServer server) {
        ServerState state = getServerState(server);

        PlayerData playerData = state.players.computeIfAbsent(player, uuid -> new PlayerData((byte) 0, (byte) -1));

        return playerData;
    }
    
    public HashMap<UUID, PlayerData> getPlayers() {
        return players;
    }

    public LocationPosition getArenaCenter() {
        return arenaCenter;
    }

    public LocationPosition getInterviewSpawn() {
        return interviewSpawn;
    }

    public LocationPosition getInterviewWaiting() {
        return interviewWaiting;
    }

    public LocationPosition getTrainingSpawn() {
        return trainingSpawn;
    }

    public HashMap<String, BlockPos> getPodiums() {
        return podiums;
    }

    public BlockPos getPodium(byte i) {
        return podiums.get(String.valueOf(i));
    }

    public GameState getGameState() {
        return currentGameState;
    }

    public void setArenaCenter(LocationPosition arenaCenter) {
        this.arenaCenter = arenaCenter;
    }

    public void setInterviewSpawn(LocationPosition interviewSpawn) {
        this.interviewSpawn = interviewSpawn;
    }

    public void setInterviewWaiting(LocationPosition interviewWaiting) {
        this.interviewWaiting = interviewWaiting;
    }

    public void setTrainingSpawn(LocationPosition trainingSpawn) {
        this.trainingSpawn = trainingSpawn;
    }

    public void setPodium(int i, BlockPos podium) {
        this.podiums.put(String.valueOf(i), podium);
    }

    public void clearPodiums() {
        this.podiums = new HashMap<>();
    }

    public void setGameState(GameState gameState) {
        this.currentGameState = gameState;
    }

    //NONE -> Teleports everyone to the training center and sets their gamemode to adventure.
    //PRESENTATION -> Currently same as above, but blocks off all training minigames.
    //INTERVIEWS -> Teleports everyone to the stage audience in adventure and teleports sour backstage in creative.
    //GAME_WAITING -> Teleports everyone to a waiting lobby while final game setup is being done.
    //GAME_ACTIVE -> Teleports all players to their respective podium elevator rooms and begins the game countdown. When only one player remains alive, the game is automatically moved to the GAME_OVER state.

    public enum GameState implements StringIdentifiable {
        NONE(
                "none",
                (server) -> {
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        player.removeCommandTag("archery");
                    });
                },
                server -> {
                    ServerState serverState = getServerState(server);

                    LocationPosition trainingCenter = serverState.getTrainingSpawn();

                    GameRules gameRules = server.getGameRules();

                    gameRules.get(GameRules.PVP).set(false, server);
                    gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server);

                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        byte playerDistrict = ServerState.getPlayerState(player).getDistrict();

                        if(playerDistrict != 0) {
                            player.teleport(server.getWorld(World.OVERWORLD), trainingCenter.x(), trainingCenter.y(), trainingCenter.z(), new HashSet<>(), trainingCenter.yaw(), trainingCenter.pitch(), true);

                            player.changeGameMode(GameMode.ADVENTURE);

                            player.getInventory().clear();
                            player.clearStatusEffects();

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
                        }
                    });
                }),
        PRESENTATION(
                "presentation",
                (server) -> {

                },
                server -> {
                    ServerState serverState = getServerState(server);

                    LocationPosition trainingCenter = serverState.getTrainingSpawn();

                    GameRules gameRules = server.getGameRules();

                    gameRules.get(GameRules.PVP).set(false, server);
                    gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server);

                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        byte playerDistrict = ServerState.getPlayerState(player).getDistrict();

                        if(playerDistrict != 0) {
                            player.teleport(server.getWorld(World.OVERWORLD), trainingCenter.x(), trainingCenter.y(), trainingCenter.z(), new HashSet<>(), trainingCenter.yaw(), trainingCenter.pitch(), true);

                            player.changeGameMode(GameMode.ADVENTURE);

                            player.getInventory().clear();
                            player.clearStatusEffects();

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
                        }
                    });
                }),
        INTERVIEWS(
                "interviews",
                (server) -> {

                },
                server -> {
                    ServerState serverState = getServerState(server);

                    LocationPosition interviewSpawn = serverState.getInterviewSpawn();
                    LocationPosition interviewBackstage = serverState.getInterviewWaiting();

                    GameRules gameRules = server.getGameRules();

                    gameRules.get(GameRules.PVP).set(false, server);
                    gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server);

                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        byte playerDistrict = ServerState.getPlayerState(player).getDistrict();

                        if(playerDistrict != 0) {
                            player.teleport(server.getWorld(World.OVERWORLD), interviewSpawn.x(), interviewSpawn.y(), interviewSpawn.z(), new HashSet<>(), interviewSpawn.yaw(), interviewSpawn.pitch(), true);

                            player.changeGameMode(GameMode.ADVENTURE);

                            player.getInventory().clear();
                            player.clearStatusEffects();

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));
                        } else {
                            player.teleport(server.getWorld(World.OVERWORLD), interviewBackstage.x(), interviewBackstage.y(), interviewBackstage.z(), new HashSet<>(), interviewBackstage.yaw(), interviewBackstage.pitch(), true);

                            player.changeGameMode(GameMode.CREATIVE);

                            player.clearStatusEffects();
                        }
                    });
                }),
        GAME_WAITING(
                "game_waiting",
                (server) -> {

                },
                server -> {
                    ServerState serverState = getServerState(server);

                    GameRules gameRules = server.getGameRules();

                    gameRules.get(GameRules.PVP).set(false, server);
                    gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server);

                    ServerWorld world = server.getWorld(World.OVERWORLD);

                    LocationPosition trainingCenter = serverState.getTrainingSpawn();
                    LocationPosition arenaCenter = serverState.getArenaCenter();

                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        byte playerDistrict = ServerState.getPlayerState(player).getDistrict();

                        if(playerDistrict != 0) {
                            player.changeGameMode(GameMode.ADVENTURE);

                            player.getInventory().clear();
                            player.clearStatusEffects();

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 255, true, false));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, -1, 255, true, false));

                            player.teleport(world, trainingCenter.x(), trainingCenter.y(), trainingCenter.z(), new HashSet<>(), trainingCenter.yaw(), trainingCenter.pitch(), true);
                        } else {
                            player.teleport(world, arenaCenter.x(), arenaCenter.y(), arenaCenter.z(), new HashSet<>(), arenaCenter.yaw(), arenaCenter.pitch(), true);
                            player.changeGameMode(GameMode.CREATIVE);

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 0, true, false));
                        }
                    });
                }),
        GAME_ACTIVE(
                "game_active",
                (server) -> {
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        server.getScoreboard().clearTeam(player.getNameForScoreboard());
                        player.removeCommandTag("alive");
                    });
                },
                server -> {
                    ServerState serverState = getServerState(server);

                    GameRules gameRules = server.getGameRules();

                    gameRules.get(GameRules.PVP).set(true, server);
                    gameRules.get(GameRules.SHOW_DEATH_MESSAGES).set(false, server);

                    LocationPosition arenaCenter = serverState.getArenaCenter();

                    ServerWorld world = server.getWorld(World.OVERWORLD);

                    world.setTimeOfDay(9500);

                    Team tributes = server.getScoreboard().getTeam("tributes");

                    if(tributes == null) {
                        tributes = server.getScoreboard().addTeam("tributes");
                    }

                    tributes.setFriendlyFireAllowed(true);
                    tributes.setCollisionRule(AbstractTeam.CollisionRule.PUSH_OWN_TEAM);
                    tributes.setShowFriendlyInvisibles(false);

                    Team gameMaster = server.getScoreboard().getTeam("game_master");

                    if(gameMaster == null) {
                        gameMaster = server.getScoreboard().addTeam("game_master");
                    }

                    gameMaster.setFriendlyFireAllowed(true);
                    gameMaster.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
                    gameMaster.setShowFriendlyInvisibles(true);

                    Set<ServerPlayerEntity> assignedPlayers = new HashSet<>();
                    Set<ServerPlayerEntity> unassignedPlayers = new HashSet<>();
                    ArrayList<Byte> unusedPodiums = new ArrayList<>();
                    for (int i = 0; i < 16; i++) {
                        unusedPodiums.add((byte) i);
                    }

                    Team finalTributes = tributes;
                    Team finalGameMaster = gameMaster;
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        byte playerDistrict = ServerState.getPlayerState(player).getDistrict();

                        if(playerDistrict != 0) {
                            player.addCommandTag("alive");
                            player.changeGameMode(GameMode.ADVENTURE);

                            server.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), finalTributes);

                            byte podium = ServerState.getPlayerState(player).getPodium();

                            player.getInventory().clear();
                            player.clearStatusEffects();
                            player.setExperienceLevel(0);
                            player.setExperiencePoints(0);

                            player.sendMessage(Text.literal("[Peacekeeper] You've got ").append(Text.literal("sixty seconds ").formatted(Formatting.BOLD)).append(Text.literal("to get in that elevator, or you'll be shot for disrupting The Culling.")));

                            if(podium == -1) unassignedPlayers.add(player);
                            else assignedPlayers.add(player);
                        } else {
                            player.teleport(world, arenaCenter.x(), arenaCenter.y(), arenaCenter.z(), new HashSet<>(), arenaCenter.yaw(), arenaCenter.pitch(), true);
                            player.changeGameMode(GameMode.CREATIVE);

                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 0, true, false));

                            server.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), finalGameMaster);
                        }
                    });

//                    assignedPlayers.forEach(player -> {
//                        byte podium = ServerState.getPlayerState(player).getPodium();
//
//                        BlockPos podiumPos = serverState.getPodium(podium);
//
//                        if(podiumPos != null && unusedPodiums.contains(podium)) {
//                            //player.teleport(world, podiumPos.getX() + 4, podiumPos.getY() - 1, podiumPos.getZ() + 3, Set.of(), 110, 0, true);
//                        } else {
//                            unassignedPlayers.add(player);
//                        }
//
//                        unusedPodiums.remove(podium);
//                    });

//                    unassignedPlayers.forEach(player -> {
//                        byte podium = unusedPodiums.get(world.random.nextInt(unusedPodiums.stream().filter(Objects::nonNull).collect(Collectors.toSet()).size()));
//
//                        BlockPos podiumPos = serverState.getPodium(podium);
//
//                        if(podiumPos != null && unusedPodiums.contains(podium)) {
//                            //player.teleport(world, podiumPos.getX() + 4, podiumPos.getY() - 1, podiumPos.getZ() + 3, Set.of(), 110, 0, true);
//                        }
//
//                        unusedPodiums.remove(podium);
//                    });

                    serverState.getPodiums().values().forEach(podiumPos -> {
                        BlockPos piston = podiumPos.add(2, 4, 0);
                        BlockPos glass1 = podiumPos.add(1, 0, 1);
                        BlockPos glass2 = podiumPos.add(1, 1, 1);
                        BlockPos glass3 = podiumPos.add(1, 2, 1);
                        BlockPos platform = podiumPos.add(0, -1, 0);
                        BlockPos podium = podiumPos.add(0, 23, 0);

                        world.setBlockState(piston, Blocks.REDSTONE_TORCH.getDefaultState());

                        world.setBlockState(glass1, Blocks.AIR.getDefaultState());
                        world.setBlockState(glass2, Blocks.AIR.getDefaultState());
                        world.setBlockState(glass3, Blocks.AIR.getDefaultState());

                        world.setBlockState(platform, Blocks.SMOOTH_STONE.getDefaultState());
                        world.setBlockState(podium, Blocks.AIR.getDefaultState());
                    });

                    GameStartCutscene.instance = new GameStartCutscene(server);
                }),
        GAME_VICTORY(
                "victory",
                (server) -> {

                },
                (server) -> {
                    server.getPlayerManager().getPlayerList().forEach(player -> {
                        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("VICTORY!").formatted(Formatting.BOLD, Formatting.GOLD)));
                        player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(Identifier.of("hungrygames", "victory"))), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 0.7f, 1.0f, 1L));
                    });
                });

        private final String id;
        private final Consumer<MinecraftServer> exitState;
        private final Consumer<MinecraftServer> beginState;

        private GameState(final String id, final Consumer<MinecraftServer> exit, Consumer<MinecraftServer> begin) {
            this.id = id;
            this.exitState = exit;
            this.beginState = begin;
        }

        public static final Codec<GameState> CODEC = StringIdentifiable.createCodec(GameState::values);

        @Override
        public String asString() {
            return this.id;
        }

        public GameState transition(MinecraftServer server, GameState nextState) {
            this.exitState.accept(server);
            nextState.beginState.accept(server);
            return nextState;
        }
    }
}
