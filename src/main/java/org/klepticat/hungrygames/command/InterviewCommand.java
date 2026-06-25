package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;
import org.klepticat.hungrygames.Hungrygames;
import org.klepticat.hungrygames.api.LocationPosition;
import org.klepticat.hungrygames.data.ServerState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.*;

public class InterviewCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("interview")
                        .requires(requirePermissionLevel(2))
                        .then(literal("release").executes(InterviewCommand::releasePlayer))
                        .then((literal("queue").then(argument("player", EntityArgumentType.player()).executes(InterviewCommand::queuePlayer))))
                        .then((literal("kick").executes(InterviewCommand::kickPlayer)))
                        .then((literal("intro").executes(InterviewCommand::intro)))
        );
    }

    private static int intro(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(Identifier.of("hungrygames", "interview"))), SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 1L));
        });

        return 1;
    }

    private static int queuePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(Hungrygames.nextInterviewee != null) {
            context.getSource().sendFeedback(() -> Text.literal("There is already an interviewee queued"), true);
            return 0;
        } else {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");

            LocationPosition interviewWaiting = ServerState.getServerState(context.getSource().getServer()).getInterviewWaiting();

            player.teleport(player.getEntityWorld(), interviewWaiting.centerX(), interviewWaiting.y(), interviewWaiting.centerZ(), new HashSet<>(), interviewWaiting.yaw(), interviewWaiting.pitch(), true);

            context.getSource().sendFeedback(() -> Text.literal("Queued %s backstage.".formatted(player.getStringifiedName())), true);

            Hungrygames.nextInterviewee = player;

            return 1;
        }
    }

    private static int releasePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(Hungrygames.nextInterviewee == null) {
            context.getSource().sendFeedback(() -> Text.literal("There is no interviewee queued backstage."), true);
            return 0;
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Released %s from backstage queue.".formatted(Hungrygames.nextInterviewee.getStringifiedName())), true);

            Hungrygames.currentInterviewee = Hungrygames.nextInterviewee;
            Hungrygames.nextInterviewee = null;

            Vec3d damagePos = ServerState.getServerState(context.getSource().getServer()).getInterviewWaiting().pos().toCenterPos().add(-1, 0, -1);

            Hungrygames.currentInterviewee.damage(Hungrygames.currentInterviewee.getEntityWorld(), new DamageSource(context.getSource().getRegistryManager().getEntryOrThrow(DamageTypes.GENERIC), damagePos), 1);

            return 1;
        }
    }

    private static int kickPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if(Hungrygames.currentInterviewee == null) {
            context.getSource().sendFeedback(() -> Text.literal("There is no current interviewee to kick."), true);
            return 0;
        } else {
            ServerPlayerEntity player = Hungrygames.currentInterviewee;

            LocationPosition interviewSpawn = ServerState.getServerState(context.getSource().getServer()).getInterviewSpawn();

            player.teleport(player.getEntityWorld(), interviewSpawn.centerX(), interviewSpawn.y(), interviewSpawn.centerZ(), new HashSet<>(), interviewSpawn.yaw(), interviewSpawn.pitch(), true);

            context.getSource().sendFeedback(() -> Text.literal("Kicked %s from the stage.".formatted(player.getStringifiedName())), true);

            Hungrygames.currentInterviewee = null;
            return 1;
        }
    }
}
