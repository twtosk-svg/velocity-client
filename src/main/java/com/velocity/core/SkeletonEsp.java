package com.velocity.core;

import com.velocity.config.EspSettings;
import imgui.ImDrawList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class SkeletonEsp {

    private static final int[][] CONNECTIONS = {
        {0, 1}, // bodyTop → headTop
        {0, 2}, // bodyTop → bodyBottom
        {0, 3}, // bodyTop → rightShoulder
        {0, 4}, // bodyTop → leftShoulder
        {3, 5}, // rightShoulder → rightHand
        {4, 6}, // leftShoulder → leftHand
        {2, 7}, // bodyBottom → rightHip
        {2, 8}, // bodyBottom → leftHip
        {7, 9}, // rightHip → rightFoot
        {8, 10} // leftHip → leftFoot
    };

    public static void renderSkeleton(ImDrawList drawList, LivingEntity entity, float tickDelta) {
        if (!EspSettings.skeletonEspEnabled) return;
        
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null && entity.distanceTo(client.player) > 64.0f) {
            return;
        }

        BoneDataCache.BoneSnapshot snap = BoneDataCache.cache.get(entity.getId());
        if (snap == null) return;

        // Model space coordinates: Y+ is down.
        // Arms and Legs are 12 units long. Head is 8 units tall. Body is 12 units long.

        Vec3d headTop       = BoneTransform.toWorld(snap.head(),     new Vector3f(0, -6, 0), snap);
        Vec3d bodyTop       = BoneTransform.toWorld(snap.body(),     new Vector3f(0, 0, 0),  snap);
        Vec3d bodyBottom    = BoneTransform.toWorld(snap.body(),     new Vector3f(0, 12, 0), snap);
        
        Vec3d rightShoulder = BoneTransform.toWorld(snap.rightArm(), new Vector3f(0, 0, 0),  snap);
        Vec3d rightHand     = BoneTransform.toWorld(snap.rightArm(), new Vector3f(0, 11, 0), snap);
        
        Vec3d leftShoulder  = BoneTransform.toWorld(snap.leftArm(),  new Vector3f(0, 0, 0),  snap);
        Vec3d leftHand      = BoneTransform.toWorld(snap.leftArm(),  new Vector3f(0, 11, 0), snap);
        
        Vec3d rightHip      = BoneTransform.toWorld(snap.rightLeg(), new Vector3f(0, 0, 0),  snap);
        Vec3d rightFoot     = BoneTransform.toWorld(snap.rightLeg(), new Vector3f(0, 12, 0), snap);
        
        Vec3d leftHip       = BoneTransform.toWorld(snap.leftLeg(),  new Vector3f(0, 0, 0),  snap);
        Vec3d leftFoot      = BoneTransform.toWorld(snap.leftLeg(),  new Vector3f(0, 12, 0), snap);

        Vec3d[] points = {
            bodyTop,        // 0
            headTop,        // 1
            bodyBottom,     // 2
            rightShoulder,  // 3
            leftShoulder,   // 4
            rightHand,      // 5
            leftHand,       // 6
            rightHip,       // 7
            leftHip,        // 8
            rightFoot,      // 9
            leftFoot        // 10
        };

        float[] c = EspSettings.skeletonColor;
        if (EspSettings.friendsSystemEnabled && EspSettings.friendEspOverride && entity instanceof net.minecraft.entity.player.PlayerEntity) {
            if (com.velocity.config.FriendManager.isFriend(entity.getName().getString())) {
                c = EspSettings.friendColor;
            }
        }
        int color = imgui.ImGui.colorConvertFloat4ToU32(c[0], c[1], c[2], c[3]);
        float thickness = EspSettings.skeletonThickness;
        
        int outlineMode = EspSettings.skeletonOutlineMode;
        if (outlineMode > 0) {
            int outlineColor = outlineMode == 1 ? 0xFF000000 : 0xFFFFFFFF;
            float outlineThickness = thickness + 1.5f;
            for (int[] conn : CONNECTIONS) {
                drawLine(drawList, points[conn[0]], points[conn[1]], outlineColor, outlineThickness);
            }
        }

        for (int[] conn : CONNECTIONS) {
            drawLine(drawList, points[conn[0]], points[conn[1]], color, thickness);
        }
    }

    private static void drawLine(ImDrawList drawList, Vec3d p1, Vec3d p2, int color, float thickness) {
        Vector3f p1Screen = new Vector3f();
        Vector3f p2Screen = new Vector3f();

        if (ProjectionMath.projectCached((float)p1.x, (float)p1.y, (float)p1.z, p1Screen) && 
            ProjectionMath.projectCached((float)p2.x, (float)p2.y, (float)p2.z, p2Screen)) {
            
            if (!Float.isFinite(p1Screen.x) || !Float.isFinite(p1Screen.y) || !Float.isFinite(p2Screen.x) || !Float.isFinite(p2Screen.y)) return;
            if (Math.abs(p1Screen.x) > 15000f || Math.abs(p1Screen.y) > 15000f || Math.abs(p2Screen.x) > 15000f || Math.abs(p2Screen.y) > 15000f) return;
            
            drawList.addLine(p1Screen.x, p1Screen.y, p2Screen.x, p2Screen.y, color, thickness);
        }
    }
}
