package org.klepticat.hungrygames.data;

import com.mojang.datafixers.types.Type;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class ServerState extends PersistentState {
    private final HashMap<UUID, PlayerData> players = new HashMap<>();

    public static ServerState createServerState() {
        return new ServerState();
    }

    public static ServerState createServerStateFromNBT(NbtCompound compound, RegistryWrapper.WrapperLookup registryLookup) {
        ServerState state = new ServerState();

        NbtCompound players = compound.getCompoundOrEmpty("players");
        players.getKeys().forEach(player -> {
            PlayerData playerData = new PlayerData();


        });
    }

    private static Type<ServerState> type = new Type<>(

    );

    public static ServerState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        ServerState state = persistentStateManager.getOrCreate()
    }
}
