package org.klepticat.hungrygames.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.AdvancedExplosionBehavior;

import java.util.Collection;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.*;

public class ExplodeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("explode")
                        .requires(requirePermissionLevel(2))
                        .then(argument("players", EntityArgumentType.players()).executes(ExplodeCommand::explodePlayers))

        );
    }

    private static int explodePlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "players");

        players.forEach(player -> {
            player.getEntityWorld().createExplosion(
                    null,
                    null,
                    new AdvancedExplosionBehavior(false, false, Optional.of(5.0f), Optional.empty()),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    5,
                    false,
                    World.ExplosionSourceType.TRIGGER
            );

            player.getInventory().clear();
            player.setExperiencePoints(0);
            player.setExperienceLevel(0);
            player.removeCommandTag("alive");
            player.kill(player.getEntityWorld());
        });

        return 1;
    }
}
