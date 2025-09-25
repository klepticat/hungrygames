package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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
                        .then(literal("test").then(argument("entity", EntityArgumentType.entity()).executes(FallenCommand::testFallen)))
        );
    }

    private static int startFallen(CommandContext<ServerCommandSource> context) {
        int successCount = 0;

        Screen screen = new Screen(
                new Vector3f(-0.5f, 0f, 0f),
                new Quaternionf(new AxisAngle4f((float) (Math.PI/2), 0, 1, 0)).mul(new Quaternionf(new AxisAngle4f(0.1f, 1, 0, 0))),
                0.1f
        );

        for(ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            DisplayEntity.TextDisplayEntity textDisplay = EntityType.TEXT_DISPLAY.spawn(player.getEntityWorld(), BlockPos.ofFloored(player.getPos()), SpawnReason.COMMAND);

            if(textDisplay != null) {
                textDisplay.setText(Text.literal("THE FALLEN"));

                textDisplay.setYaw(0);

                Vector3f screenPosition = new Vector3f(-0.5f, 0f, 0f);
                Quaternionf screenRotation = new Quaternionf(new AxisAngle4f((float) (Math.PI/2), 0, 1, 0)).mul(new Quaternionf(new AxisAngle4f(0.1f, 1, 0, 0)));
                float screenScale = 0.1f;



                AffineTransformation transformation = new AffineTransformation(
                        screen.makeOffset(new Vector3f(0f, 1f, 0f)),
                        screen.rotation,
                        new Vector3f(screenScale),
                        null
                );

                textDisplay.setTransformation(transformation);

                if(textDisplay.startRiding(player, true, false)) {
                    player.networkHandler.send(new EntityPassengersSetS2CPacket(player), null);
                    successCount++;
                }
            }
        }

        int finalSuccessCount = successCount;
        context.getSource().sendFeedback(() -> Text.literal("Started The Fallen cutscene for %s players".formatted(finalSuccessCount)), true);

        return finalSuccessCount;
    }

    private static int testFallen(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        try {
            Entity entity = EntityArgumentType.getEntity(context, "entity");
            ServerPlayerEntity player = context.getSource().getPlayer();

            boolean b = entity.startRiding(player, true, true);

            context.getSource().sendFeedback(() -> Text.literal(String.valueOf(b)), true);
            player.networkHandler.send(new EntityPassengersSetS2CPacket(player), null);
            return b ? 1 : 0;
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
