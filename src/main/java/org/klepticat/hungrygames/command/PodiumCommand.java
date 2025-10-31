package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.klepticat.hungrygames.data.PlayerData;
import org.klepticat.hungrygames.data.ServerState;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.*;

public class PodiumCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("podium")
                        .requires(requirePermissionLevel(2))
                        .then(
                                argument("player", GameProfileArgumentType.gameProfile())
                                        .then(literal("set").then(argument("podium", IntegerArgumentType.integer(-1, 15)).executes(PodiumCommand::setPlayerPodium)))
                                        .then(literal("get").executes(PodiumCommand::getPlayerPodium))
                        )

        );
    }

    private static int setPlayerPodium(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<PlayerConfigEntry> players = GameProfileArgumentType.getProfileArgument(context, "player");

        players.forEach(playerConfigEntry -> {


            byte podium = (byte) IntegerArgumentType.getInteger(context, "podium");

            PlayerData playerData = ServerState.getPlayerState(playerConfigEntry.id(), context.getSource().getServer());
            byte podiumPrev = playerData.getPodium();

            playerData.setPodium(podium);

            context.getSource().sendFeedback(() -> Text.literal("Updated %s's podium from Podium %s to Podium %s".formatted(playerConfigEntry.name(), podiumPrev, podium)), true);
        });

        return 1;
    }

    private static int getPlayerPodium(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<PlayerConfigEntry> players = GameProfileArgumentType.getProfileArgument(context, "player");

        players.forEach(playerConfigEntry -> {
            PlayerData playerData = ServerState.getPlayerState(playerConfigEntry.id(), context.getSource().getServer());
            byte podium = playerData.getPodium();

            context.getSource().sendFeedback(() -> Text.literal("%s is assigned to Podium %s".formatted(playerConfigEntry.name(), podium)), false);
        });

        return 1;
    }
}
