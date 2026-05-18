package com.velocity.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class ProjectileMath {

    public static Vec3d lastIntercept = null;

    /**
     * Calculates the required pitch and yaw to hit a target.
     * Overrides normal aimbot logic by predicting target velocity and modeling gravity.
     * 
     * @return float[2] {yaw, pitch} or null if impossible to hit
     */
    public static float[] getBowAimbotAngles(MinecraftClient client, LivingEntity target) {
        if (client.player == null) return null;

        net.minecraft.item.ItemStack mainStack = client.player.getMainHandStack();
        boolean isCrossbow = mainStack.getItem() instanceof net.minecraft.item.CrossbowItem;
        float velocity = 0f;

        if (isCrossbow) {
            // Crossbow max velocity is theoretically 3.15
            if (net.minecraft.component.DataComponentTypes.CHARGED_PROJECTILES != null && mainStack.contains(net.minecraft.component.DataComponentTypes.CHARGED_PROJECTILES)) {
                // If it's loaded with at least one projectile
                if (!mainStack.get(net.minecraft.component.DataComponentTypes.CHARGED_PROJECTILES).isEmpty()) {
                    velocity = 3.15f;
                }
            } else {
                lastIntercept = null;
                return null;
            }
        } else {
            // Bow Physics
            int useTicks = client.player.getItemUseTime();
            if (useTicks <= 0) {
                lastIntercept = null;
                return null;
            }
            float charge = (float) useTicks / 20.0f;
            if (charge < 1.0f) {
                // User requested bow ONLY tracks if fully charged!
                lastIntercept = null;
                return null;
            }
            velocity = (charge * charge + charge * 2.0f) / 3.0f * 3.0f;
        }

        if (velocity < 0.1f) {
            lastIntercept = null;
            return null; // Too weak to fire
        }

        Vec3d startPos = client.player.getCameraPosVec(1.0f);
        Vec3d targetPos = target.getEntityPos().add(0, target.getHeight() / 2.0, 0); // Aim for chest
        Vec3d targetVel = target.getVelocity();

        // 2. Iterate Flight Ticks to Find Mathematically Exact Velocity Match
        int bestTick = -1;
        double bestV_XZ = 0;
        double bestV_Y = 0;
        double minVelocityDiff = Double.MAX_VALUE;
        Vec3d bestIntercept = null;

        for (int ticks = 1; ticks <= 100; ticks++) {
            // Predict target future position (Linear vector)
            // Note: For perfect jump prediction you can simulate Y-gravity here, but linear is highly accurate.
            Vec3d predict = targetPos.add(targetVel.multiply(ticks));
            
            double dx = predict.x - startPos.x;
            double dy = predict.y - startPos.y;
            double dz = predict.z - startPos.z;
            double distXZ = Math.sqrt(dx * dx + dz * dz);

            // XZ Distance = V_initial * 100 * (1 - 0.99^t)
            double dragMult = 100.0 * (1.0 - Math.pow(0.99, ticks));
            if (dragMult == 0) continue;

            // Gravity Drop = 5 * (t - dragMult)
            double gravityDrop = 5.0 * (ticks - dragMult);

            // Required Launch Vectors to hit exactly at 'ticks'
            double vXZ = distXZ / dragMult;
            double vY = (dy + gravityDrop) / dragMult;

            double requiredVelocitySq = vXZ * vXZ + vY * vY;
            double requiredVelocity = Math.sqrt(requiredVelocitySq);

            // Find the tick where the required velocity perfectly matches our weapon's physical max velocity
            double diff = Math.abs(requiredVelocity - velocity);
            if (diff < minVelocityDiff) {
                minVelocityDiff = diff;
                bestTick = ticks;
                bestV_XZ = vXZ;
                bestV_Y = vY;
                bestIntercept = predict;
            }
        }

        // If the closest match requires wildly impossible velocities, target is out of range
        if (bestIntercept == null || minVelocityDiff > 0.5) {
            lastIntercept = null;
            return null; 
        }

        lastIntercept = bestIntercept;

        double pitchRad = Math.atan2(bestV_Y, bestV_XZ);
        double dx = bestIntercept.x - startPos.x;
        double dz = bestIntercept.z - startPos.z;
        double yawRad = Math.atan2(dz, dx) - Math.PI / 2.0;

        return new float[] { (float) Math.toDegrees(yawRad), (float) -Math.toDegrees(pitchRad) }; // Minecraft Pitch is inverted (negative looks up)
    }
}
