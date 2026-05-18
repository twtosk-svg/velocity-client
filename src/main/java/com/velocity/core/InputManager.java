package com.velocity.core;

public class InputManager {

    /**
     * Polls the OS for keypresses and returns the first active Win32 Virtual Key code.
     * Returns -1 if none.
     */
    public static int pollForKeyPress() {
        for (int k = 65; k <= 90; k++) {
            if ((Win32Setup.INSTANCE.GetAsyncKeyState(k) & 0x8000) != 0) return k;
        }
        for (int k = 48; k <= 57; k++) {
            if ((Win32Setup.INSTANCE.GetAsyncKeyState(k) & 0x8000) != 0) return k;
        }
        for (int i = 0; i < 12; i++) {
            if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x70 + i) & 0x8000) != 0) return 0x70 + i;
        }
        
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x01) & 0x8000) != 0) return 0x01; // LBUTTON
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x02) & 0x8000) != 0) return 0x02; // RBUTTON
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x04) & 0x8000) != 0) return 0x04; // MBUTTON
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x05) & 0x8000) != 0) return 0x05; // XBUTTON1
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x06) & 0x8000) != 0) return 0x06; // XBUTTON2

        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x09) & 0x8000) != 0) return 0x09; // Tab
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x10) & 0x8000) != 0) return 0x10; // Shift
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x11) & 0x8000) != 0) return 0x11; // Ctrl
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x12) & 0x8000) != 0) return 0x12; // Alt
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x14) & 0x8000) != 0) return 0x14; // CapsLock
        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x20) & 0x8000) != 0) return 0x20; // Space

        if ((Win32Setup.INSTANCE.GetAsyncKeyState(0x1B) & 0x8000) != 0)
            return org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
        return -1;
    }

    /** Returns a human-readable string for the bound Virtual Key code. */
    public static String getKeyName(int vkKey) {
        if (vkKey >= 65 && vkKey <= 90) return String.valueOf((char) vkKey);
        if (vkKey >= 48 && vkKey <= 57) return String.valueOf((char) vkKey);
        if (vkKey >= 0x70 && vkKey <= 0x7B) return "F" + (vkKey - 0x70 + 1);
        switch (vkKey) {
            case 0x01: return "M1";
            case 0x02: return "M2";
            case 0x04: return "M3";
            case 0x05: return "M4";
            case 0x06: return "M5";
            case 0x09: return "Tab";
            case 0x10: return "Shift";
            case 0x11: return "Ctrl";
            case 0x12: return "Alt";
            case 0x14: return "CapsLock";
            case 0x20: return "Space";
            case 256: return "Esc";
            default: return "Key(" + vkKey + ")";
        }
    }

    private static boolean wasMiddleClick = false;

    public static void checkMiddleClickFriend() {
        if (!com.velocity.config.AimAssistSettings.middleClickFriendEnabled) return;
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) return;

        boolean isMiddleClick = (Win32Setup.INSTANCE.GetAsyncKeyState(0x04) & 0x8000) != 0;
        if (isMiddleClick && !wasMiddleClick) {
            wasMiddleClick = true;
            
            // Custom 200-block raycast
            double maxDistance = 200.0;
            net.minecraft.util.math.Vec3d start = client.player.getCameraPosVec(1.0f);
            net.minecraft.util.math.Vec3d look = client.player.getRotationVec(1.0f);
            net.minecraft.util.math.Vec3d end = start.add(look.multiply(maxDistance));
            net.minecraft.util.math.Box searchBox = client.player.getBoundingBox().stretch(look.multiply(maxDistance)).expand(1.0, 1.0, 1.0);

            net.minecraft.entity.player.PlayerEntity hitPlayer = null;
            double closestDist = maxDistance * maxDistance;

            for (net.minecraft.entity.Entity entity : client.world.getOtherEntities(client.player, searchBox, e -> e instanceof net.minecraft.entity.player.PlayerEntity && !e.isSpectator())) {
                net.minecraft.util.math.Box entityBox = entity.getBoundingBox().expand(0.1);
                java.util.Optional<net.minecraft.util.math.Vec3d> hit = entityBox.raycast(start, end);
                if (hit.isPresent()) {
                    double dist = start.squaredDistanceTo(hit.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        hitPlayer = (net.minecraft.entity.player.PlayerEntity) entity;
                    }
                }
            }

            if (hitPlayer != null) {
                com.velocity.config.FriendManager.toggleFriend(hitPlayer.getName().getString());
            }
        } else if (!isMiddleClick) {
            wasMiddleClick = false;
        }
    }
}
