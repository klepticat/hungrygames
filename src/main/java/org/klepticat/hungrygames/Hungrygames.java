package org.klepticat.hungrygames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.klepticat.hungrygames.api.FallenManager;
import org.klepticat.hungrygames.command.FallenCommand;

public class Hungrygames implements ModInitializer {
    public static FallenManager fallenManager = new FallenManager();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            FallenCommand.register(commandDispatcher);
        });
    }
}
