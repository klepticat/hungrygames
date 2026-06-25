package org.klepticat.hungrygames.api;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class HungryEvent {
    public HungryEvent(MinecraftServer server) {
        init(server);
    }

    public abstract void tick(MinecraftServer server);

    public abstract void init(MinecraftServer server);
}
