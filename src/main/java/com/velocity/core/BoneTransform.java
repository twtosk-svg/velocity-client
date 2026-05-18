package com.velocity.core;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class BoneTransform {

    public static Vec3d toWorld(BoneDataCache.Bone bone, Vector3f localOffset, BoneDataCache.BoneSnapshot snap) {
        // Step 1: Apply bone local rotation to the local offset
        // In Minecraft ModelPart.render, matrices are multiplied: Z, then Y, then X.
        // Because matrices apply right-to-left to vectors, the actual order applied to the vertex is X, then Y, then Z.
        Vector3f pos = new Vector3f(localOffset);
        
        // Pitch (X)
        if (bone.rotation().x != 0.0f) {
            float cos = MathHelper.cos(bone.rotation().x);
            float sin = MathHelper.sin(bone.rotation().x);
            float py = pos.y * cos - pos.z * sin;
            float pz = pos.y * sin + pos.z * cos;
            pos.y = py;
            pos.z = pz;
        }
        
        // Yaw (Y)
        if (bone.rotation().y != 0.0f) {
            float cos = MathHelper.cos(bone.rotation().y);
            float sin = MathHelper.sin(bone.rotation().y);
            float px = pos.x * cos + pos.z * sin;
            float pz = -pos.x * sin + pos.z * cos;
            pos.x = px;
            pos.z = pz;
        }
        
        // Roll (Z)
        if (bone.rotation().z != 0.0f) {
            float cos = MathHelper.cos(bone.rotation().z);
            float sin = MathHelper.sin(bone.rotation().z);
            float px = pos.x * cos - pos.y * sin;
            float py = pos.x * sin + pos.y * cos;
            pos.x = px;
            pos.y = py;
        }

        // Step 2: Add pivot translation
        pos.add(bone.pivot());

        // Step 3: Convert from model units to meters
        Vector3f transformed = new Vector3f(pos.x / 16f, pos.y / 16f, pos.z / 16f);

        // Step 4: Apply Minecraft's global LivingEntityRenderer transformations
        // These happen in the reverse order of the Java matrix commands!
        
        // 1. Translate (Minecraft hardcodes -1.501f for Biped models to place feet on the ground)
        transformed.add(0.0f, -1.501f, 0.0f);
        
        // 2. Scale (Minecraft flips X and Y to convert from model-space to world-space)
        transformed.mul(-1.0f, -1.0f, 1.0f);
        
        // 3. Rotate by Body Yaw (Minecraft offsets yaw by 180 degrees)
        float yawRad = (float) Math.toRadians(180.0f - snap.bodyYaw());
        // Standard Y-axis rotation
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float tx = transformed.x * cosYaw + transformed.z * sinYaw;
        float tz = -transformed.x * sinYaw + transformed.z * cosYaw;
        transformed.x = tx;
        transformed.z = tz;

        // Step 5: Add entity world origin
        // (Swimming offsets are handled by pitch/yaw, sneaking by Bone pivot adjustments!)
        return new Vec3d(
            snap.worldX() + transformed.x,
            snap.worldY() + transformed.y,
            snap.worldZ() + transformed.z
        );
    }
    
    public static Vec3d midpoint(Vec3d a, Vec3d b) {
        return new Vec3d(
            (a.x + b.x) * 0.5,
            (a.y + b.y) * 0.5,
            (a.z + b.z) * 0.5
        );
    }

    public static Vec3d lerp(Vec3d a, Vec3d b, double t) {
        return new Vec3d(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
    }
}
