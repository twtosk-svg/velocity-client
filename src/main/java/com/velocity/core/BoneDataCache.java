package com.velocity.core;

import org.joml.Vector3f;
import java.util.concurrent.ConcurrentHashMap;

public class BoneDataCache {

    public record Bone(Vector3f pivot, Vector3f rotation) {}

    public record BoneSnapshot(
        // Raw model-space pivots and rotations (already animated by setAngles)
        // Pivots are in model units (divide by 16 for meters), rotations in radians
        Bone head,
        Bone body,
        Bone rightArm,
        Bone leftArm,
        Bone rightLeg,
        Bone leftLeg,

        // Entity world transform (to combine with pivots)
        double worldX,
        double worldY,
        double worldZ,
        float  bodyYaw,      // degrees, already lerped
        float  entityHeight, // for Y baseline calibration

        // Extra state flags (read from entity, not model)
        boolean isSneaking,
        boolean isSwimming,
        boolean isGliding    // elytra
    ) {}

    // Key: entity ID → latest snapshot
    // Written by render thread, read by ImGui thread
    public static final ConcurrentHashMap<Integer, BoneSnapshot> cache 
        = new ConcurrentHashMap<>();

    // Call this on disconnect / world change
    public static void clear() {
        cache.clear();
    }
}
