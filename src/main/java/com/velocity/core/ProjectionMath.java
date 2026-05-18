package com.velocity.core;

import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class ProjectionMath {

    // ── Per-frame cached camera context ──────────────────────────────────────
    // Computed once via prepareFrame(), reused by all projectCached() calls.
    // Eliminates 4× trig calls + 1× tan + 1× division per worldToScreen call.
    private static float cSinY, cCosY, cSinP, cCosP;
    private static float cTanHalfFov, cAspect;
    private static double cCamX, cCamY, cCamZ;
    private static float cHalfW, cHalfH;

    /**
     * Call once per frame before any projectCached() calls.
     * Pre-computes all trig values that are constant for the entire frame.
     */
    public static void prepareFrame(
            float pitch, float yaw, float fov,
            int width, int height,
            double camX, double camY, double camZ) {
        float yawRad   = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-pitch);
        cSinY = (float) Math.sin(yawRad);
        cCosY = (float) Math.cos(yawRad);
        cSinP = (float) Math.sin(pitchRad);
        cCosP = (float) Math.cos(pitchRad);
        cTanHalfFov = (float) Math.tan(Math.toRadians(fov * 0.5));
        cAspect = (float) width / (float) height;
        cCamX = camX;
        cCamY = camY;
        cCamZ = camZ;
        cHalfW = width * 0.5f;
        cHalfH = height * 0.5f;
    }

    /**
     * Projects using the cached camera context from prepareFrame().
     * Zero trig calls — all sin/cos/tan are reused from the cache.
     */
    public static boolean projectCached(
            double posX, double posY, double posZ,
            Vector3f outScreen) {

        double x = posX - cCamX;
        double y = posY - cCamY;
        double z = posZ - cCamZ;

        float rx = (float) (x * cCosY - z * cSinY);
        float ry = (float) y;
        float rz = (float) (x * cSinY + z * cCosY);

        float vx =  rx;
        float vy =  ry * cCosP - rz * cSinP;
        float vz =  ry * cSinP + rz * cCosP;

        if (vz <= 0.0f) return false;

        float ndcX = -vx / (vz * cTanHalfFov * cAspect);
        float ndcY =  vy / (vz * cTanHalfFov);

        outScreen.set(
                (ndcX + 1.0f) * cHalfW,
                (1.0f - ndcY) * cHalfH,
                vz);
        return true;
    }

    /** Returns the cached tanHalfFov for FOV circle computations. */
    public static float getCachedTanHalfFov() { return cTanHalfFov; }

    // ── Legacy methods (for callers that don't use frame caching) ────────────

    public static boolean worldToScreen(
            Vec3d pos,
            Vec3d cameraPos,
            float pitch,
            float yaw,
            float fov,
            int width,
            int height,
            Vector3f outScreen) {
        return worldToScreen(
                pos.x, pos.y, pos.z,
                cameraPos.x, cameraPos.y, cameraPos.z,
                pitch, yaw, fov, width, height, outScreen);
    }

    public static boolean worldToScreen(
            double posX, double posY, double posZ,
            double camX, double camY, double camZ,
            float pitch,
            float yaw,
            float fov,
            int width,
            int height,
            Vector3f outScreen) {

        double x = posX - camX;
        double y = posY - camY;
        double z = posZ - camZ;

        float yawRad   = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(-pitch);

        float sinY = (float) Math.sin(yawRad);
        float cosY = (float) Math.cos(yawRad);
        float sinP = (float) Math.sin(pitchRad);
        float cosP = (float) Math.cos(pitchRad);

        float rx = (float) (x * cosY - z * sinY);
        float ry = (float) y;
        float rz = (float) (x * sinY + z * cosY);

        float vx =  rx;
        float vy =  ry * cosP - rz * sinP;
        float vz =  ry * sinP + rz * cosP;

        if (vz <= 0.0f) return false;

        float aspectRatio = (float) width / (float) height;
        float tanHalfFov  = (float) Math.tan(Math.toRadians(fov * 0.5));

        float ndcX = -vx / (vz * tanHalfFov * aspectRatio);
        float ndcY =  vy / (vz * tanHalfFov);

        float winX = (ndcX + 1.0f) * 0.5f * width;
        float winY = (1.0f - ndcY) * 0.5f * height;

        outScreen.set(winX, winY, vz);
        return true;
    }
}
