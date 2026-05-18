package com.velocity.config;

/**
 * Settings for Light Source ESP.
 * Auto-persisted by ConfigManager via reflection.
 */
public class LightSourceEspSettings {

    // ── Master toggle ────────────────────────────────────────────────────────
    public static boolean enabled = false;

    // ── Scan radius (blocks from player) ─────────────────────────────────────
    public static int scanRadius = 32;

    // ── Rescan interval (ticks between rescans) ──────────────────────────────
    public static int rescanTicks = 10;

    // ── Minimum luminance to show (1-15, filters dim sources) ────────────────
    public static int minLuminance = 1;
}
