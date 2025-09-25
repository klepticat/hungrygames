package org.klepticat.hungrygames.api;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Screen {
    private final Vector3f position;
    private final Quaternionf rotation;
    private final float scale;

    public Screen(Vector3f position, Quaternionf rotation, float scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    public Vector3f makeOffset(Vector3f offset) {
        return offset.mul(this.scale).rotate(rotation).add(this.position);
    }

    public float getScale() {
        return scale;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Vector3f getPosition() {
        return position;
    }
}
