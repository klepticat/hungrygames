package org.klepticat.hungrygames.api;

import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.object.PlayerTextObjectContents;
import net.minecraft.text.object.TextObjectContents;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Stream;

public class FallenManager {
    private final Queue<ServerPlayerEntity> fallenQueue = new LinkedList<>();
    private final HashSet<DisplayEntity.TextDisplayEntity> textEntities = new HashSet<>();

    private final Screen screen = new Screen(
            new Vector3f(-0.5f, 0f, 0f),
            new Quaternionf(new AxisAngle4f((float) (Math.PI/2), 0, 1, 0)).mul(new Quaternionf(new AxisAngle4f(0.1f, 1, 0, 0))),
            0.1f
    );

    public boolean addFallen(ServerPlayerEntity player) {
        if (fallenQueue.contains(player)) return false;

        return fallenQueue.add(player);
    }

    public @Nullable ServerPlayerEntity getNextFallen() {
        return fallenQueue.peek();
    }

    public @Nullable ServerPlayerEntity popNextFallen() {
        return fallenQueue.poll();
    }

    public Stream<ServerPlayerEntity> getQueueStream() {
        return fallenQueue.stream();
    }

    public int getFallenCount() {
        return fallenQueue.size();
    }

    public void clearFallen() {
        fallenQueue.clear();
    }

    public void initScreen(ServerPlayerEntity player) {
        float offset = 0f;
        int count = 3;
        for (int i = 0; i < count; i++) {
            AffineTransformation textTransformation = new AffineTransformation(
                    screen.makeOffset(new Vector3f(0f, -offset/3, offset*2)),
                    screen.rotation(),
                    new Vector3f(screen.scale()),
                    null
            );

            AffineTransformation iconTransformation = new AffineTransformation(
                    screen.makeOffset(new Vector3f(-0.15f, (-offset/3), offset*2)),
                    screen.rotation(),
                    new Vector3f(screen.scale()).mul(7f),
                    null
            );

            DisplayEntity.TextDisplayEntity textDisplay = addText(
                    player,
                    Text.literal("THE FALLEN"),
                    textTransformation,
                    ColorHelper.getArgb(100, 255, 230),
                    (byte) (((float) (count - i) / (float) count) * 255)
            );

            DisplayEntity.TextDisplayEntity iconDisplay = addIcon(
                    player,
                    new PlayerTextObjectContents(ProfileComponent.ofStatic(player.getGameProfile()), true),
                    iconTransformation,
                    (byte) (((float) (count - i) / (float) count) * 255)
            );

            offset--;
        }


    }

    private @Nullable DisplayEntity.TextDisplayEntity addText(ServerPlayerEntity player, MutableText text, AffineTransformation transform, int color, byte opacity) {
        DisplayEntity.TextDisplayEntity textDisplay = EntityType.TEXT_DISPLAY.spawn(player.getEntityWorld(), player.getBlockPos(), SpawnReason.COMMAND);

        if(textDisplay == null) return null;

        textDisplay.addCommandTag("FallenText");
        textDisplay.setText(text.withColor(color));
        textDisplay.setTextOpacity(opacity);
        textDisplay.setTransformation(transform);
        textDisplay.setBackground(0);
        textDisplay.setYaw(0);

        if(textDisplay.startRiding(player, true, false)) {
            player.networkHandler.send(new EntityPassengersSetS2CPacket(player), null);
        } else {
            textDisplay.discard();
            return null;
        }

        return textDisplay;
    }

    private @Nullable DisplayEntity.TextDisplayEntity addIcon(ServerPlayerEntity player, TextObjectContents icon, AffineTransformation transform, byte opacity) {
        DisplayEntity.TextDisplayEntity textDisplay = EntityType.TEXT_DISPLAY.spawn(player.getEntityWorld(), player.getBlockPos(), SpawnReason.COMMAND);

        if(textDisplay == null) return null;

        textDisplay.addCommandTag("FallenIcon");
        textDisplay.setText(Text.object(icon));
        textDisplay.setTextOpacity(opacity);
        textDisplay.setTransformation(transform);
        textDisplay.setBackground(0);
        textDisplay.setYaw(0);

        if(textDisplay.startRiding(player, true, false)) {
            player.networkHandler.send(new EntityPassengersSetS2CPacket(player), null);
        } else {
            textDisplay.discard();
            return null;
        }

        return textDisplay;
    }

    public void updateText(ServerPlayerEntity player, Text text) {
        player.getPassengerList().forEach(passenger -> {
            if(passenger instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                if(textDisplay.getCommandTags().contains("FallenText")) {
                    textDisplay.setText(text);
                }
            }
        });
    }

    public void updateIcon(ServerPlayerEntity player, ServerPlayerEntity fallen) {
        player.getPassengerList().forEach(passenger -> {
            if(passenger instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                if(textDisplay.getCommandTags().contains("FallenIcon")) {
                    textDisplay.setText(Text.object(new PlayerTextObjectContents(ProfileComponent.ofStatic(fallen.getGameProfile()), true)));
                }
            }
        });
    }

    private void updateIcon(ServerPlayerEntity player, TextObjectContents icon) {
        player.getPassengerList().forEach(passenger -> {
            if(passenger instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                if(textDisplay.getCommandTags().contains("FallenIcon")) {
                    textDisplay.setText(Text.object(icon));
                }
            }
        });
    }

    public void killScreen(ServerPlayerEntity player) {
        player.getPassengerList().forEach(passenger -> {
            if(passenger instanceof DisplayEntity.TextDisplayEntity textDisplay) {
                if(textDisplay.getCommandTags().contains("FallenIcon") || textDisplay.getCommandTags().contains("FallenText")) {
                    textDisplay.discard();
                }
            }
        });
    }
}
