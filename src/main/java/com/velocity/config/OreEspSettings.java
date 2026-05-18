package com.velocity.config;

/**
 * Central config store for Ore ESP settings.
 * Each ore type has an individual toggle and a pre-defined color.
 */
public class OreEspSettings {

    // ── Master toggle ─────────────────────────────────────────────────────────
    public static boolean enabled = false;

    // ── Toggle keybind (GLFW key code, -1 = unbound) ─────────────────────────
    public static int oreToggleKey = -1;

    // ── Scan radius (blocks from player) ──────────────────────────────────────
    public static int scanRadius = 32;

    // ── Rescan interval (ticks between full rescans) ──────────────────────────
    public static int rescanTicks = 10;

    // ── Individual ore toggles ────────────────────────────────────────────────
    public static boolean diamond = true;
    public static boolean emerald = true;
    public static boolean lapis = true;
    public static boolean redstone = true;
    public static boolean gold = true;
    public static boolean iron = true;
    public static boolean coal = true;
    public static boolean copper = true;
    public static boolean ancientDebris = true;
    public static boolean quartz = true;
}
