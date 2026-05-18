package com.velocity.config;

import com.velocity.module.combat.AimAssist;
import com.velocity.module.combat.TriggerBot;
import com.velocity.core.EspRenderer;

/**
 * Central config store for all aim-assist settings.
 * All fields are public static so AimAssist and EspRenderer can both read/write
 * them.
 */
public class AimAssistSettings {

    // ── Master toggle ─────────────────────────────────────────────────────────
    public static boolean enabled = false;
    public static boolean middleClickFriendEnabled = true;
    


    // ── TriggerBot ────────────────────────────────────────────────────────────
    /** Auto-attack when weapon is charged and crosshair is over enemy. */
    public static boolean triggerBotEnabled = false;
    
    /** 0 = Legit (Win32), 1 = Zero-Tick (Packets + Raycast) */
    public static int triggerBotVersion = 0;

    /** Click Method: 0 = Win32, 1 = Internal, 2 = Mix */
    public static int triggerBotClickMethod = 0;


    /** Minimum delay (ms) between triggerbot attacks. */
    public static int triggerBotMinDelayMs = 50;
    /** Maximum delay (ms) between triggerbot attacks. */
    public static int triggerBotMaxDelayMs = 150;
    
    /** Only attack when falling if in the air (crithit). */
    public static boolean triggerBotSmartCrit = false;
    /** Attack before full charge if the damage is lethal. */
    public static boolean triggerBotLowHpOverride = false;
    
    /** Only trigger the bot when holding a Sword, Axe, Mace, or Trident. */
    public static boolean triggerBotWeaponOnly = false;
    
    /** Automatically execute OS-level click to break enemy defenses with an axe. */
    public static boolean shieldBreakerEnabled = false;
    /** Minimum cooldown required (out of 100) before breaking the shield. */
    public static float shieldBreakerMinCooldown = 15f;

    // ── Target filters ────────────────────────────────────────────────────────
    public static boolean targetZombies = true;
    public static boolean targetVillagers = true;
    public static boolean targetPlayers = false;
    public static boolean targetBats = false;
    public static boolean targetRabbits = false;
    // ── Timing ────────────────────────────────────────────────────────────────
    /** How long (ms) the assist stays active after the user moves the mouse. */
    public static int assistDurationMs = 500;

    // ── FOV ───────────────────────────────────────────────────────────────────
    /**
     * Max angle (degrees) from crosshair to entity. Only entities within this
     * cone are candidates. This is the full cone radius, NOT a half-angle.
     */
    public static float fovDegrees = 60f;

    // ── Distance ──────────────────────────────────────────────────────────────
    public static float maxDistance = 6.0f;

    // ── Focus Keybind ─────────────────────────────────────────────────────────
    public static int focusKeybindKey = -1;

    // ── Raven-style speed / compliment ────────────────────────────────────────
    /** Speed 1 (yaw) — main pull speed toward target. Higher = faster snap. */
    public static float speedYaw = 45f;
    /** Speed 2 (yaw) — randomized compliment modifier for natural jitter. */
    public static float complimentYaw = 15f;
    /** Speed 1 (pitch) — main pull speed toward target (vertical). */
    public static float speedPitch = 45f;
    /** Speed 2 (pitch) — randomized compliment modifier (vertical). */
    public static float complimentPitch = 15f;

    // ── Deadzone ──────────────────────────────────────────────────────────────
    /**
     * Minimum cursor delta (pixels) per frame to count as intentional movement.
     * Below this, mouse input is ignored (hand tremors / micro-adjustments).
     */
    public static float deadzonePixels = 2f;
    /**
     * If true, only trigger assist when the mouse movement direction is roughly
     * toward the current target (within 90° of the target direction).
     */
    public static boolean deadzoneDirectionCheck = true;

    // ── Free movement (Fusion style) ──────────────────────────────────────────
    /**
     * When enabled, yaw is corrected toward the target but pitch is free to
     * move anywhere between the target's head and feet. You can look at any
     * body part but never aim above the head or below the feet.
     */
    public static boolean freeMovement = true;

    // ── Visibility ────────────────────────────────────────────────────────────
    public static boolean visibilityCheck = true;

    // ── Mouse4 aim ────────────────────────────────────────────────────────────
    /**
     * When enabled, holding Mouse4 (XBUTTON1 / back button) activates aim assist.
     */
    public static boolean mouse4Aim = false;

    // ── FOV circle overlay ────────────────────────────────────────────────────
    public static boolean fovCircleEnabled = true;
    public static float[] fovCircleColor = { 1f, 1f, 1f, 0.4f }; // white, 40% alpha

    // ── Nametag glow for targeted entity ──────────────────────────────────────
    public static boolean nametagGlowEnabled = true;
    public static float[] nametagGlowColor = { 1f, 0.4f, 0.8f, 1f }; // pink

    // ── Global Combat ─────────────────────────────────────────────────────────
    /** Do not aim or swing at players on the same scoreboard team. */
    public static boolean ignoreTeammates = true;
}
