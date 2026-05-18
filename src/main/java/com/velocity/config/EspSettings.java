package com.velocity.config;


import com.velocity.core.EspRenderer;

/**
 * Central config store for ESP rendering settings.
 * Previously these were private fields in EspRenderer — externalized here
 * so ConfigManager can serialize/deserialize them.
 */
public class EspSettings {

    // ── Master toggles ────────────────────────────────────────────────────────
    public static boolean espEnabled = true;
    public static boolean espBoxesEnabled = true;
    public static float[] boxColor = { 1.0f, 1.0f, 1.0f, 1.0f }; // White
    public static boolean nametagsEnabled = true;
    public static boolean advancedNametagsEnabled = true;
    public static boolean healthbarsEnabled = true;
    public static boolean armorBarsEnabled = true;
    public static boolean absorptionBarsEnabled = true;
    public static boolean distanceEnabled = true;
    public static float maxDistance = 120f;
    public static boolean streamproofEnabled = true;
    public static boolean equipmentEspEnabled = false;
    // ── Skeleton ESP ──────────────────────────────────────────────────────────
    public static boolean skeletonEspEnabled = false;
    public static float[] skeletonColor = { 1.0f, 1.0f, 1.0f, 1.0f }; // White
    public static float skeletonThickness = 1.5f;
    public static int skeletonOutlineMode = 1; // 0=None, 1=Black, 2=White

    // ── Friends & Radar ───────────────────────────────────────────────────────
    public static boolean friendsSystemEnabled = true;
    public static boolean friendEspOverride = true;
    public static float[] friendColor = { 0.0f, 1.0f, 1.0f, 1.0f }; // Cyan/Aqua
    public static boolean adminOverlayEnabled = true;
    public static boolean playerRadarEnabled = false;

    // ── Healthbar ─────────────────────────────────────────────────────────────
    public static int healthPosition = 1; // 0=Left, 1=Right, 2=Top, 3=Bottom
    public static float healthbarThickness = 4f;

    // ── Health v2 (pink glow) ─────────────────────────────────────────────────
    public static boolean healthV2Enabled = false;
    public static float healthV2GlowStrength = 4f;

    // ── Shield indicator ──────────────────────────────────────────────────────
    public static boolean shieldEspEnabled = true;
    public static float[] shieldColor = { 0.0f, 1.0f, 1.0f, 0.5f }; // Cyan, 50% opacity
    public static float shieldEspThickness = 2.0f;
    public static int shieldEspMode = 0; // 0=Outline, 1=Fill, 2=Both

    // ── Eye Tracers ───────────────────────────────────────────────────────────
    public static boolean eyeTraceEnabled = false;
    public static float[] eyeTraceColor = { 1.0f, 0.0f, 0.0f, 1.0f }; // Red
    public static float eyeTraceLength = 2.0f; // Blocks

    // ── Tracers ──────────────────────────────────────────────────────────────
    public static boolean tracersEnabled = false;
    public static float[] tracerColor = { 1.0f, 1.0f, 1.0f, 0.7f }; // White, 70% opacity
}
