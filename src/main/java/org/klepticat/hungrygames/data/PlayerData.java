package org.klepticat.hungrygames.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PlayerData {
    private byte district;
    private byte podium;

    public PlayerData(byte district, byte podium) {
        this.district = district;
        this.podium = podium;
    }

    public byte getDistrict() {
        return district;
    }

    public byte setDistrict(byte district) {
        this.district = district;
        return this.district;
    }

    public byte getPodium() {
        return podium;
    }

    public byte setPodium(byte podium) {
        this.podium = podium;
        return this.podium;
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BYTE.optionalFieldOf("district", (byte) 0).forGetter(PlayerData::getDistrict),
            Codec.BYTE.optionalFieldOf("podium", (byte) -1).forGetter(PlayerData::getPodium)
    ).apply(instance, PlayerData::new));
}
