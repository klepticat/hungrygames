package org.klepticat.hungrygames.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

public record LocationPosition(BlockPos pos, float pitch, float yaw) {
    public static Codec<LocationPosition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("position").forGetter(LocationPosition::pos),
                    Codec.FLOAT.fieldOf("pitch").forGetter(LocationPosition::pitch),
                    Codec.FLOAT.fieldOf("yaw").forGetter(LocationPosition::yaw)
            ).apply(instance, LocationPosition::new)
    );

    public BlockPos getBlockPos() {
        return pos;
    }

    public int x() {
        return pos.getX();
    }

    public int y() {
        return pos.getY();
    }

    public int z() {
        return pos.getZ();
    }

    public double centerX() {
        return pos.toCenterPos().x;
    }

    public double centerY() {
        return pos.toCenterPos().y;
    }

    public double centerZ() {
        return pos.toCenterPos().z;
    }

    public String toString() {
        return "XYZ[%s %s %s] Rot[%s %s]".formatted(x(), y() ,z(), pitch, yaw);
    }
}
