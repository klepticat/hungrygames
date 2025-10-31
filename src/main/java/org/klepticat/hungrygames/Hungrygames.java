package org.klepticat.hungrygames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.klepticat.hungrygames.api.FallenManager;
import org.klepticat.hungrygames.command.*;

public class Hungrygames implements ModInitializer {
    public static FallenManager fallenManager = new FallenManager();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            FallenCommand.register(commandDispatcher);
            DistrictCommand.register(commandDispatcher);
            LocationCommand.register(commandDispatcher);
            GameStateCommand.register(commandDispatcher);
            PodiumCommand.register(commandDispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameStartCutscene.tick();
        });
    }
}
