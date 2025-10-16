package org.klepticat.hungrygames.api;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public record Screen(Vector3f position, Quaternionf rotation, float scale) {

    public Vector3f makeOffset(Vector3f offset) {
        return offset.mul(this.scale).rotate(rotation).add(this.position);
    }
}
