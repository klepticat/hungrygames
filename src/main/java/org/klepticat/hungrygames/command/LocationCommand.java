package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.klepticat.hungrygames.api.LocationPosition;
import org.klepticat.hungrygames.data.ServerState;

import static net.minecraft.server.command.CommandManager.*;

public class LocationCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("location")
                        .requires(requirePermissionLevel(2))
                        .then(
                                literal("get")
                                        .then(literal("training").executes(LocationCommand::getTraining))
                                        .then(literal("arena").executes(LocationCommand::getArena))
                                        .then(literal("interview_wait").executes(LocationCommand::getInterviewWait))
                                        .then(literal("interview_spawn").executes(LocationCommand::getInterviewSpawn))
                                        .then(literal("podium")
                                                .then(argument("number", IntegerArgumentType.integer(0, 15)).executes(LocationCommand::getPodium))
                                        )
                        )
                        .then(
                                literal("set")
                                        .then(literal("training").executes(LocationCommand::setTraining))
                                        .then(literal("arena").executes(LocationCommand::setArena))
                                        .then(literal("interview_wait").executes(LocationCommand::setInterviewWait))
                                        .then(literal("interview_spawn").executes(LocationCommand::setInterviewSpawn))
                                        .then(literal("podium")
                                                .then(argument("number", IntegerArgumentType.integer(0, 15)).executes(LocationCommand::setPodium))
                                        )
                        )
        );
    }

    private static int setTraining(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        Vec3d position = context.getSource().getPosition();
        Vec2f rotation = context.getSource().getRotation();

        serverState.setTrainingSpawn(new LocationPosition(BlockPos.ofFloored(position), rotation.x, rotation.y));

        return 1;
    }

    public static int getTraining(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.of("%s".formatted(serverState.getTrainingSpawn())), false);

        return 1;
    }

    private static int setArena(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        Vec3d position = context.getSource().getPosition();
        Vec2f rotation = context.getSource().getRotation();

        serverState.setArenaCenter(new LocationPosition(BlockPos.ofFloored(position), rotation.x, rotation.y));

        return 1;
    }

    public static int getArena(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.of("%s".formatted(serverState.getArenaCenter())), false);

        return 1;
    }

    private static int setInterviewWait(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        Vec3d position = context.getSource().getPosition();
        Vec2f rotation = context.getSource().getRotation();

        serverState.setInterviewWaiting(new LocationPosition(BlockPos.ofFloored(position), rotation.x, rotation.y));

        return 1;
    }

    public static int getInterviewWait(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.of("%s".formatted(serverState.getInterviewWaiting())), false);

        return 1;
    }

    private static int setInterviewSpawn(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        Vec3d position = context.getSource().getPosition();
        Vec2f rotation = context.getSource().getRotation();

        serverState.setInterviewSpawn(new LocationPosition(BlockPos.ofFloored(position), rotation.x, rotation.y));

        return 1;
    }

    public static int getInterviewSpawn(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        context.getSource().sendFeedback(() -> Text.of("%s".formatted(serverState.getInterviewSpawn())), false);

        return 1;
    }

    private static int setPodium(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());
        byte podiumIndex = (byte) IntegerArgumentType.getInteger(context, "number");

        Vec3d position = context.getSource().getPosition();

        serverState.setPodium(podiumIndex, BlockPos.ofFloored(position));

        return 1;
    }

    public static int getPodium(CommandContext<ServerCommandSource> context) {
        ServerState serverState = ServerState.getServerState(context.getSource().getServer());

        BlockPos podium = serverState.getPodium((byte) IntegerArgumentType.getInteger(context, "number"));

        context.getSource().sendFeedback(() -> Text.of("%s %s %s".formatted(podium.getX(), podium.getY(), podium.getZ())), false);

        return 1;
    }
}
