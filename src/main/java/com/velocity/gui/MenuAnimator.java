package com.velocity.gui;

import com.velocity.gui.framework.UiEasing;

/**
 * Menu open/close fade and scale for Velocity overlay.
 */
public final class MenuAnimator {
    private MenuAnimator() {}

    private static float menuAlpha = 0f;
    private static float menuScale = 0.97f;
    private static boolean targetOpen = false;
    private static boolean closing = false;
    private static boolean animating = false;

    public static void onToggleRequest(boolean wantOpen) {
        targetOpen = wantOpen;
        if (!wantOpen) {
            closing = true;
        } else {
            closing = false;
            animating = true;
        }
    }

    public static void tick(boolean overlayReportsOpen, float deltaTime) {
        if (overlayReportsOpen && !closing) {
            targetOpen = true;
        }

        float alphaTarget = targetOpen ? 1f : 0f;
        float scaleTarget = targetOpen ? 1f : 0.97f;

        menuAlpha = UiEasing.ease(menuAlpha, alphaTarget, 12f, UiEasing.STATIC);
        menuScale = UiEasing.ease(menuScale, scaleTarget, 12f, UiEasing.STATIC);

        animating = menuAlpha > 0.01f && menuAlpha < 0.99f;
        if (!targetOpen && menuAlpha <= 0.01f) {
            menuAlpha = 0f;
            closing = false;
        }
    }

    public static boolean shouldDrawMenu() {
        return menuAlpha > 0.01f || targetOpen;
    }

    public static boolean isClosing() {
        return closing && menuAlpha > 0.01f;
    }

    public static boolean isFullyClosed() {
        return !targetOpen && menuAlpha <= 0.01f;
    }

    public static void syncOpenState(boolean open) {
        if (open) {
            targetOpen = true;
            closing = false;
            if (menuAlpha < 0.5f) menuAlpha = 0.5f;
        }
    }

    public static float getAlpha() {
        return menuAlpha;
    }

    public static float getScale() {
        return menuScale;
    }

    public static boolean wantsOpen() {
        return targetOpen;
    }
}
