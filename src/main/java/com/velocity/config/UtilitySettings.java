package com.velocity.config;

/**
 * Central config store for Utility feature settings.
 */
public class UtilitySettings {

    // ── Hover Refill ──────────────────────────────────────────────────────────
    /** When enabled, hovering over a healing potion in inventory moves it to hotbar. */
    public static boolean hoverRefillEnabled = false;

    // ── Heal Keybind ──────────────────────────────────────────────────────────
    /** When enabled, the heal key throws a hotbar potion and restores slot. */
    public static boolean healKeybindEnabled = false;
    /** GLFW key code for the heal keybind. -1 = unbound. */
    public static int healKeybindKey = -1;
    /** Min ticks to wait after switching to potion before throwing. */
    public static int healSwitchMinTicks = 2;
    /** Max ticks to wait after switching to potion before throwing. */
    public static int healSwitchMaxTicks = 4;
    /** Min ticks to wait after throwing before switching back to sword. */
    public static int healRestoreMinTicks = 2;
    /** Max ticks to wait after throwing before switching back to sword. */
    public static int healRestoreMaxTicks = 5;

    // ── Auto W-Tap ────────────────────────────────────────────────────────────
}
