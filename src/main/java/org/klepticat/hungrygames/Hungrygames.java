package org.klepticat.hungrygames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.joml.Vector3f;
import org.klepticat.hungrygames.command.FallenCommand;

public class Hungrygames implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            FallenCommand.register(commandDispatcher);
        });
    }
}
