package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.object.PlayerTextObjectContents;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.klepticat.hungrygames.Hungrygames;
import org.klepticat.hungrygames.api.FallenManager;
import org.klepticat.hungrygames.api.Screen;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FallenCommand {
    public FallenCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("fallen")
                        .then(literal("start").executes(FallenCommand::startFallen))
                        .then(literal("dump").executes(FallenCommand::dumpFallen))
                        .then(literal("clear").executes(FallenCommand::clearFallen))
                        .then(literal("next").executes(FallenCommand::nextFallen))
        );
    }

    private static int startFallen(CommandContext<ServerCommandSource> context) {
        int successCount = 0;

        for(ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            Hungrygames.fallenManager.initScreen(player);
            successCount++;
        }

        int finalSuccessCount = successCount;
        context.getSource().sendFeedback(() -> Text.literal("Started The Fallen cutscene for %s players".formatted(finalSuccessCount)), true);

        return finalSuccessCount;
    }

    private static int nextFallen(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity fallen = Hungrygames.fallenManager.popNextFallen();
        if(fallen == null) {
            for(ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
                Hungrygames.fallenManager.killScreen(player);
            }

            return 1;
        }

        int count = 0;

        for(ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            Hungrygames.fallenManager.updateText(player, fallen.getDisplayName());
            Hungrygames.fallenManager.updateIcon(player, fallen);
            count++;
        }

        return count;
    }

    private static int dumpFallen(CommandContext<ServerCommandSource> context) {
        Hungrygames.fallenManager.getQueueStream().forEachOrdered(player -> {
            ProfileComponent profileComponent = ProfileComponent.ofStatic(player.getGameProfile());

            context.getSource().sendFeedback(() -> Text.object(new PlayerTextObjectContents(profileComponent, true)), false);
        });

        return 1;
    }

    private static int clearFallen(CommandContext<ServerCommandSource> context) {
        int clearCount = Hungrygames.fallenManager.getFallenCount();

        Hungrygames.fallenManager.clearFallen();

        context.getSource().sendFeedback(() -> Text.literal("Cleared %s Fallen from the queue".formatted(clearCount)), false);

        return clearCount;
    }
}
