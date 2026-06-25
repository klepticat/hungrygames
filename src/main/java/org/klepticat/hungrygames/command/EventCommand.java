package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.klepticat.hungrygames.Hungrygames;
import org.klepticat.hungrygames.api.HungryEvent;
import org.klepticat.hungrygames.events.AcidRainEvent;
import org.klepticat.hungrygames.events.EruptionEvent;
import org.klepticat.hungrygames.events.SandstormEvent;

import static net.minecraft.server.command.CommandManager.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class EventCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("event")
                        .requires(requirePermissionLevel(2))
                        .then(literal("eruption")
                                .then(literal("start").executes(EventCommand::startEruption))
                                .then(literal("end").executes(EventCommand::clearEruption))
                        )
                        .then(literal("acid_rain")
                                .then(literal("start").executes(EventCommand::startRain))
                                .then(literal("end").executes(EventCommand::clearRain))
                        )
                        .then(literal("sandstorm")
                                .then(literal("start").executes(EventCommand::startSandstorm))
                                .then(literal("end").executes(EventCommand::clearSandstorm))
                        )
        );
    }

    private static int clearSandstorm(CommandContext<ServerCommandSource> context) {
        boolean bl = clearEvent(SandstormEvent.class, context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(bl ? "Cleared sandstorm." : "No active sandstorm."), true);

        return bl ? 1 : 0;
    }

    private static int startSandstorm(CommandContext<ServerCommandSource> context) {
        boolean bl = startEvent(SandstormEvent.class, context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(bl ? "Started sandstorm." : "Sandstorm already started."), true);

        return bl ? 1 : 0;
    }

    private static int clearEruption(CommandContext<ServerCommandSource> context) {
        Optional<? extends HungryEvent> eventOptional = Hungrygames.events.stream().filter(event1 -> event1.getClass() == EruptionEvent.class).findFirst();
        if(eventOptional.isPresent()) {
            EruptionEvent event = (EruptionEvent) eventOptional.get();

            event.magmaBlocks.forEach(Entity::discard);

            boolean bl = clearEvent(EruptionEvent.class, context.getSource().getServer());

            context.getSource().sendFeedback(() -> Text.literal(bl ? "Cleared eruption." : "No active eruption."), true);

            return bl ? 1 : 0;
        }
        return 0;
    }

    private static int startEruption(CommandContext<ServerCommandSource> context) {
        boolean bl = startEvent(EruptionEvent.class, context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(bl ? "Started eruption." : "Eruption already started."), true);

        return bl ? 1 : 0;
    }

    private static int clearRain(CommandContext<ServerCommandSource> context) {
        boolean bl = clearEvent(AcidRainEvent.class, context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(bl ? "Cleared acid rain." : "No active acid rain."), true);

        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0)));

        return bl ? 1 : 0;
    }

    private static int startRain(CommandContext<ServerCommandSource> context) {
        boolean bl = startEvent(AcidRainEvent.class, context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.literal(bl ? "Started acid rain." : "Acid rain already started."), true);

        return bl ? 1 : 0;
    }

    private static boolean startEvent(Class<? extends HungryEvent> newEvent, MinecraftServer server) {
        if(Hungrygames.events.stream().anyMatch(event -> event.getClass() == newEvent)) return false;
        else {
            try {
                Hungrygames.events.add(newEvent.getDeclaredConstructor(MinecraftServer.class).newInstance(server));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    private static boolean clearEvent(Class<? extends HungryEvent> endEvent, MinecraftServer server) {
        return Hungrygames.events.removeIf(event -> event.getClass() == endEvent);
    }
}
