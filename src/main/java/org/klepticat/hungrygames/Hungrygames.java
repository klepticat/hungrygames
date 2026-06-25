package org.klepticat.hungrygames;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.klepticat.hungrygames.api.FallenManager;
import org.klepticat.hungrygames.api.HungryEvent;
import org.klepticat.hungrygames.command.*;

import java.util.HashSet;
import java.util.Set;

public class Hungrygames implements ModInitializer {
    public static FallenManager fallenManager = new FallenManager();

    public static Set<HungryEvent> events = new HashSet<>();

    public static ServerPlayerEntity nextInterviewee;
    public static ServerPlayerEntity currentInterviewee;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            FallenCommand.register(commandDispatcher);
            DistrictCommand.register(commandDispatcher);
            LocationCommand.register(commandDispatcher);
            GameStateCommand.register(commandDispatcher);
            PodiumCommand.register(commandDispatcher);
            EventCommand.register(commandDispatcher);
            ExplodeCommand.register(commandDispatcher);
            InterviewCommand.register(commandDispatcher);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GameStartCutscene.tick();
            events.forEach(event -> {
                event.tick(server);
            });

            if(nextInterviewee != null) {
                nextInterviewee.setVelocity(0, 0, 0);
                nextInterviewee.velocityDirty = true;
            }
        });
    }
}
