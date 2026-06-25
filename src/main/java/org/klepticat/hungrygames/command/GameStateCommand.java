package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.klepticat.hungrygames.data.ServerState;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.*;

public class GameStateCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("gamestate")
                        .requires(requirePermissionLevel(2))
                        .then(literal("get").executes(GameStateCommand::getCurrentState))
                        .then(
                                literal("set").then(argument("state", StringArgumentType.word()).suggests(new GameStateProvider()).executes(GameStateCommand::setNewState))
                        )
        );
    }

    private static class GameStateProvider implements SuggestionProvider<ServerCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
            for(ServerState.GameState gameState : ServerState.GameState.values()) {
               builder.suggest(gameState.asString());
            }

            return builder.buildFuture();
        }
    }

    public static int getCurrentState(CommandContext<ServerCommandSource> context) {
        ServerState.GameState state = ServerState.getServerState(context.getSource().getServer()).getGameState();

        context.getSource().sendFeedback(() -> Text.of("Current gamestate is %s".formatted(state.asString())), false);

        return state.ordinal();
    }

    public static int setNewState(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        ServerState.GameState oldGameState = serverState.getGameState();
        String newGameStateString = StringArgumentType.getString(context, "state");

        ServerState.GameState newGameState = switch (newGameStateString) {
            case "none" -> ServerState.GameState.NONE;
            case "presentation" -> ServerState.GameState.PRESENTATION;
            case "interviews" -> ServerState.GameState.INTERVIEWS;
            case "game_waiting" -> ServerState.GameState.GAME_WAITING;
            case "game_active" -> ServerState.GameState.GAME_ACTIVE;
            case "victory" -> ServerState.GameState.GAME_VICTORY;
            default -> null;
        };

        if(newGameState == null) {
            context.getSource().sendFeedback(() -> Text.of("Not a valid game state: %s".formatted(newGameStateString)), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> Text.of("Transitioning gamestates from %s to %s".formatted(oldGameState.asString(), newGameState.asString())), true);

        try {
            serverState.setGameState(oldGameState.transition(context.getSource().getServer(), newGameState));
        } catch (Exception e) {
            context.getSource().sendFeedback(() -> Text.of(e.toString()), true);
        }


        return 1;
    }
}
