package com.velocity.module.combat;

import com.velocity.config.AimAssistSettings;
import com.velocity.core.EspRenderer;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * TriggerBot — automatically attacks when:
 * 1. The weapon is fully charged (attack indicator '+' is showing).
 * 2. The game crosshair is over a living entity (EntityHitResult).
 * 3. The player is NOT ascending (jumping / flying upward).
 * 4. No GUI screen is open.
 *
 * Attacks are sent via Win32 SendInput (real OS left-click) rather than
 * internal game methods, for maximum compatibility.
 */
public class TriggerBot {

    private static long nextAttackTime = 0;

    // ── Weapon-check cache (avoids toLowerCase() allocation every tick) ──────
    private static net.minecraft.item.Item lastCheckedItem = null;
    private static boolean lastCheckedIsWeapon = false;

    public static volatile boolean wantsInternalClick = false;

    // ── SendInput constants ──────────────────────────────────────────────────
    private static final int INPUT_MOUSE = 0;
    private static final int INPUT_KEYBOARD = 1;
    private static final int MOUSEEVENTF_LEFTDOWN = 0x0002;
    private static final int MOUSEEVENTF_LEFTUP = 0x0004;
    private static final int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private static final int MOUSEEVENTF_RIGHTUP = 0x0010;
    private static final int KEYEVENTF_KEYUP = 0x0002;

    // ── JNA structures for SendInput ─────────────────────────────────────────

    @Structure.FieldOrder({ "dx", "dy", "mouseData", "dwFlags", "time", "dwExtraInfo" })
    public static class MOUSEINPUT extends Structure {
        public int dx;
        public int dy;
        public int mouseData;
        public int dwFlags;
        public int time;
        public long dwExtraInfo; // ULONG_PTR
    }

    public static class INPUT_UNION extends Union {
        public MOUSEINPUT mi;
        public KEYBDINPUT ki;
    }

    @Structure.FieldOrder({ "wVk", "wScan", "dwFlags", "time", "dwExtraInfo" })
    public static class KEYBDINPUT extends Structure {
        public short wVk;
        public short wScan;
        public int dwFlags;
        public int time;
        public long dwExtraInfo;
    }

    @Structure.FieldOrder({ "type", "input" })
    public static class INPUT extends Structure {
        public int type;
        public INPUT_UNION input;
    }

    // ── Native SendInput binding (loaded once) ───────────────────────────────
    public interface User32Send extends com.sun.jna.win32.StdCallLibrary {
        User32Send INSTANCE = Native.load("user32", User32Send.class);

        int SendInput(int nInputs, INPUT[] pInputs, int cbSize);
    }

    // ── Pre-allocated SendInput structs (reused every click — zero GC) ───────
    private static final INPUT[] CLICK_INPUTS;
    private static final INPUT[] RIGHT_CLICK_INPUTS;
    private static final INPUT[] KEY_INPUTS;
    static {
        // Left click
        CLICK_INPUTS = (INPUT[]) new INPUT().toArray(2);
        CLICK_INPUTS[0].type = INPUT_MOUSE;
        CLICK_INPUTS[0].input.setType(MOUSEINPUT.class);
        CLICK_INPUTS[0].input.mi.dwFlags = MOUSEEVENTF_LEFTDOWN;
        CLICK_INPUTS[0].input.mi.dx = 0;
        CLICK_INPUTS[0].input.mi.dy = 0;
        CLICK_INPUTS[0].input.mi.mouseData = 0;
        CLICK_INPUTS[0].input.mi.time = 0;
        CLICK_INPUTS[0].input.mi.dwExtraInfo = 0;
        CLICK_INPUTS[1].type = INPUT_MOUSE;
        CLICK_INPUTS[1].input.setType(MOUSEINPUT.class);
        CLICK_INPUTS[1].input.mi.dwFlags = MOUSEEVENTF_LEFTUP;
        CLICK_INPUTS[1].input.mi.dx = 0;
        CLICK_INPUTS[1].input.mi.dy = 0;
        CLICK_INPUTS[1].input.mi.mouseData = 0;
        CLICK_INPUTS[1].input.mi.time = 0;
        CLICK_INPUTS[1].input.mi.dwExtraInfo = 0;

        // Right click
        RIGHT_CLICK_INPUTS = (INPUT[]) new INPUT().toArray(2);
        RIGHT_CLICK_INPUTS[0].type = INPUT_MOUSE;
        RIGHT_CLICK_INPUTS[0].input.setType(MOUSEINPUT.class);
        RIGHT_CLICK_INPUTS[0].input.mi.dwFlags = MOUSEEVENTF_RIGHTDOWN;
        RIGHT_CLICK_INPUTS[0].input.mi.dx = 0;
        RIGHT_CLICK_INPUTS[0].input.mi.dy = 0;
        RIGHT_CLICK_INPUTS[0].input.mi.mouseData = 0;
        RIGHT_CLICK_INPUTS[0].input.mi.time = 0;
        RIGHT_CLICK_INPUTS[0].input.mi.dwExtraInfo = 0;
        RIGHT_CLICK_INPUTS[1].type = INPUT_MOUSE;
        RIGHT_CLICK_INPUTS[1].input.setType(MOUSEINPUT.class);
        RIGHT_CLICK_INPUTS[1].input.mi.dwFlags = MOUSEEVENTF_RIGHTUP;
        RIGHT_CLICK_INPUTS[1].input.mi.dx = 0;
        RIGHT_CLICK_INPUTS[1].input.mi.dy = 0;
        RIGHT_CLICK_INPUTS[1].input.mi.mouseData = 0;
        RIGHT_CLICK_INPUTS[1].input.mi.time = 0;
        RIGHT_CLICK_INPUTS[1].input.mi.dwExtraInfo = 0;

        // Key press (reusable — vk set at call time)
        KEY_INPUTS = (INPUT[]) new INPUT().toArray(2);
        KEY_INPUTS[0].type = INPUT_KEYBOARD;
        KEY_INPUTS[0].input.setType(KEYBDINPUT.class);
        KEY_INPUTS[0].input.ki.dwFlags = 0; // key down
        KEY_INPUTS[0].input.ki.time = 0;
        KEY_INPUTS[0].input.ki.dwExtraInfo = 0;
        KEY_INPUTS[1].type = INPUT_KEYBOARD;
        KEY_INPUTS[1].input.setType(KEYBDINPUT.class);
        KEY_INPUTS[1].input.ki.dwFlags = KEYEVENTF_KEYUP;
        KEY_INPUTS[1].input.ki.time = 0;
        KEY_INPUTS[1].input.ki.dwExtraInfo = 0;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Tick — called every frame from EspRenderer.render()
    // ═════════════════════════════════════════════════════════════════════════

    public static void tick(MinecraftClient client) {
        if (!AimAssistSettings.triggerBotEnabled)
            return;
        if (client == null || client.world == null || client.player == null)
            return;
        if (client.player.isDead())
            return;
        if (client.player.isUsingItem())
            return;

        // Don't trigger while a GUI is open
        if (client.currentScreen != null)
            return;

        // Crosshair must be over a living entity
        if (client.crosshairTarget == null)
            return;
        if (client.crosshairTarget.getType() != HitResult.Type.ENTITY)
            return;
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit))
            return;
        if (!(entityHit.getEntity() instanceof LivingEntity target))
            return;
        if (target.isDead() || target.getHealth() <= 0)
            return;
            
        // Invincibility / I-Frame check (Players don't take damage if hurtTime > 0)
        if (target.isInvulnerable() || target.hurtTime > 0)
            return;

        // Teammate Check
        if (AimAssistSettings.ignoreTeammates && target instanceof net.minecraft.entity.player.PlayerEntity && client.player.isTeammate(target))
            return;

        // Friend check
        if (com.velocity.config.EspSettings.friendsSystemEnabled && target instanceof net.minecraft.entity.player.PlayerEntity && com.velocity.config.FriendManager.isFriend(target.getName().getString()))
            return;

        // Weapon Only check (cached — no string alloc unless item changes)
        if (AimAssistSettings.triggerBotWeaponOnly) {
            net.minecraft.item.Item heldItem = client.player.getMainHandStack().getItem();
            if (heldItem != lastCheckedItem) {
                String itemName = heldItem.getTranslationKey().toLowerCase();
                lastCheckedIsWeapon = itemName.contains("sword") || itemName.contains("_axe")
                        || itemName.contains("mace") || itemName.contains("trident");
                lastCheckedItem = heldItem;
            }
            if (!lastCheckedIsWeapon) {
                return;
            }
        }

        long now = System.currentTimeMillis();

        // ── 2. Standard TriggerBot logic ──────────────────────────────────────────
        if (AimAssistSettings.triggerBotEnabled) {
            // Delay check
            if (now < nextAttackTime)
                return;

            // Smart Crit check
            if (AimAssistSettings.triggerBotSmartCrit) {
                if (!client.player.isOnGround() && client.player.getVelocity().y >= 0) {
                    return; // Wait until falling
                }
                if (!client.player.isOnGround() && client.player.fallDistance <= 0.0f) {
                    return; // Wait until officially falling
                }
            } else {
                // Legacy check
                if (!client.player.isOnGround() && client.player.getVelocity().y > 0)
                    return;
            }

            // Attack charge / Low HP override check
            float progress = client.player.getAttackCooldownProgress(0.5f);
            boolean fullyCharged = progress >= 1.0f;
            boolean bypassCharge = false;

            if (AimAssistSettings.triggerBotLowHpOverride && !fullyCharged) {
                try {
                    // Fetch exact internal weapon damage (automatically includes base damage + Sharpness/Smite + Strength effects)
                    float currentDamage = (float) client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
                    
                    // The damage attribute is the max damage. We scale it by the attack progress cooldown
                    float damageMult = 0.2f + progress * progress * 0.8f;
                    currentDamage = currentDamage * damageMult;

                    // Crit bonus (roughly 1.5x)
                    if (!client.player.isOnGround() && client.player.fallDistance > 0.0f) {
                        currentDamage *= 1.5f;
                    }

                    // Armor mitigation: prevent premature swinging against armored targets
                    int armor = target.getArmor();
                    if (armor > 0) {
                        float assumedToughness = 8.0f; // Diamond-level toughness ensures we don't overestimate damage output
                        float punchThrough = currentDamage / (2.0f + assumedToughness / 4.0f);
                        float mitigation = Math.min(20.0f, Math.max(armor / 5.0f, armor - punchThrough)) / 25.0f;
                        currentDamage = currentDamage * (1.0f - mitigation);
                    }

                    if (target.getHealth() <= currentDamage) {
                        bypassCharge = true; // Lethal blow
                    }
                } catch (Exception ignored) {
                }
            }

            if (!fullyCharged && !bypassCharge)
                return;

            // ── Fire! ────────────────────────────────────────────────────────────────
            boolean clicked = false;
            switch (AimAssistSettings.triggerBotClickMethod) {
                case 0: // Win32
                    clicked = sendLeftClick();
                    break;
                case 1: // Internal
                    wantsInternalClick = true;
                    clicked = true;
                    break;
                case 2: // Mix
                    wantsInternalClick = true;
                    boolean win32Success = sendLeftClick();
                    clicked = true;
                    break;
            }

            if (clicked) {
                int delayMs = AimAssistSettings.triggerBotMinDelayMs;
                if (AimAssistSettings.triggerBotMaxDelayMs > AimAssistSettings.triggerBotMinDelayMs) {
                    delayMs = java.util.concurrent.ThreadLocalRandom.current().nextInt(
                            AimAssistSettings.triggerBotMinDelayMs,
                            AimAssistSettings.triggerBotMaxDelayMs + 1);
                }
                nextAttackTime = now + delayMs;
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Win32 SendInput — left mouse down + up (pre-allocated, zero alloc)
    // ═════════════════════════════════════════════════════════════════════════

    private static long lastOsClickTime = 0;

    public static boolean sendLeftClick() {
        if (System.currentTimeMillis() - lastOsClickTime < 100) return false;
        lastOsClickTime = System.currentTimeMillis();

        try {
            User32Send.INSTANCE.SendInput(2, CLICK_INPUTS, CLICK_INPUTS[0].size());
            return true;
        } catch (Exception e) {
            System.err.println("[Velocity] SendInput left-click failed: " + e.getMessage());
            return false;
        }
    }

    /** Sends a real OS right-click via SendInput (for undetectable item use). */
    public static void sendRightClick() {
        try {
            User32Send.INSTANCE.SendInput(2, RIGHT_CLICK_INPUTS, RIGHT_CLICK_INPUTS[0].size());
        } catch (Exception e) {
            System.err.println("[Velocity] SendInput right-click failed: " + e.getMessage());
        }
    }

    /** Sends a real OS key press via SendInput (for undetectable hotbar switching). */
    public static void sendKeyPress(int vk) {
        try {
            KEY_INPUTS[0].input.ki.wVk = (short) vk;
            KEY_INPUTS[1].input.ki.wVk = (short) vk;
            User32Send.INSTANCE.SendInput(2, KEY_INPUTS, KEY_INPUTS[0].size());
        } catch (Exception e) {
            System.err.println("[Velocity] SendInput key press failed: " + e.getMessage());
        }
    }
}
