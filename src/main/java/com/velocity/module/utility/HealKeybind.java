package com.velocity.module.utility;

import com.velocity.module.combat.TriggerBot;
import com.velocity.config.UtilitySettings;
import com.velocity.core.EspRenderer;
import com.velocity.core.Win32Setup;
import com.velocity.mixin.PlayerInventoryAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import com.velocity.mixin.PlayerInventoryAccessor;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Heal Keybind — pressing a configured key while in-game will:
 *   1. Find the first healing potion in the hotbar.
 *   2. Switch to it via simulated number key press (SendInput).
 *   3. Wait configurable game ticks (Gaussian random delay).
 *   4. Throw (right-click) the potion via simulated mouse click (SendInput).
 *   5. Wait configurable game ticks (Gaussian random delay).
 *   6. Switch to the first sword via simulated number key press (SendInput).
 *
 * ANTI-CHEAT DESIGN:
 *   - All inputs go through Win32 SendInput (OS-level simulation)
 *     so they're indistinguishable from real human input.
 *   - Slot switches use keyboard number keys (1-9), not raw packets.
 *   - Item use goes through real right-click, not interactItem().
 *   - Delays use Gaussian random distribution (human-like clustering).
 *   - State machine is tick-aligned via player.age (20 ticks/sec).
 */
public class HealKeybind {

    private static boolean keyWasDown = false;

    // ── State machine ────────────────────────────────────────────────────────
    // IDLE → SWITCHED → THROWING → RESTORING → IDLE
    private static int state = 0; // 0=IDLE, 1=SWITCHED (waiting to throw), 2=THROWN (waiting to restore)
    private static int stateStartAge = 0;  // player.age when state was entered
    private static int targetWaitTicks = 0;
    private static int savedSlot = -1;
    private static int lastProcessedAge = -1; // prevents running multiple times per tick

    /** Called every render frame from EspRenderer. */
    public static void tick(MinecraftClient client) {
        if (client == null || client.player == null || client.interactionManager == null || client.world == null)
            return;

        ClientPlayerEntity player = client.player;

        // ── Deduplicate: only run state machine once per game tick ─────────────
        // render() is called every FRAME (60-240+ fps), but we must only act
        // once per game tick (20/sec). player.age increments once per tick.
        int currentAge = player.age;
        if (currentAge == lastProcessedAge) return;
        lastProcessedAge = currentAge;

        // ── State machine execution (runs every GAME TICK) ────────────────────
        if (state > 0) {
            int ticksElapsed = currentAge - stateStartAge;

            switch (state) {
                case 1: // SWITCHED — waiting to throw
                    if (ticksElapsed >= targetWaitTicks) {
                        // Throw the potion via real right-click (SendInput)
                        TriggerBot.sendRightClick();

                        // Move to waiting-to-restore state
                        state = 2;
                        stateStartAge = currentAge;
                        targetWaitTicks = gaussianTicks(
                                UtilitySettings.healRestoreMinTicks,
                                UtilitySettings.healRestoreMaxTicks);
                    }
                    break;

                case 2: // THROWN — waiting to switch back
                    if (ticksElapsed >= targetWaitTicks) {
                        // Switch to sword via number key, or fall back to saved slot
                        int swordSlot = findSwordSlot(player);
                        if (swordSlot >= 0) {
                            sendHotbarKey(swordSlot);
                        } else {
                            sendHotbarKey(savedSlot);
                        }
                        state = 0; // Done
                    }
                    break;
            }
            return; // Don't process key input while state machine is active
        }

        // ── Feature guard ──────────────────────────────────────────────────────
        if (!UtilitySettings.healKeybindEnabled) return;
        int key = UtilitySettings.healKeybindKey;
        if (key < 0) return; // unbound

        // ── Key edge detection via Win32 ───────────────────────────────────────
        boolean keyDown = false;
        try {
            int vk = glfwKeyToVk(key);
            if (vk > 0) {
                keyDown = (Win32Setup.INSTANCE.GetAsyncKeyState(vk) & 0x8000) != 0;
            }
        } catch (Exception ignored) {}

        boolean pressed = keyDown && !keyWasDown;
        keyWasDown = keyDown;

        if (!pressed) return;

        // ── Find first healing potion in hotbar (slots 0-8) ───────────────────
        int potionSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (HoverRefill.isHealingPotion(stack)) {
                potionSlot = i;
                break;
            }
        }

        if (potionSlot < 0) return; // no potion in hotbar

        // ── Begin state machine: switch to potion slot via number key ──────────
        savedSlot = ((com.velocity.mixin.PlayerInventoryAccessor) (Object) player.getInventory()).getSelectedSlot();
        sendHotbarKey(potionSlot);

        state = 1; // SWITCHED — now waiting to throw
        stateStartAge = currentAge;
        targetWaitTicks = gaussianTicks(
                UtilitySettings.healSwitchMinTicks,
                UtilitySettings.healSwitchMaxTicks);
    }

    /**
     * Sends a hotbar number key press (1-9) via Win32 SendInput.
     * Slot 0 = key '1' (VK 0x31), slot 8 = key '9' (VK 0x39).
     * This is indistinguishable from a real keyboard press.
     */
    private static void sendHotbarKey(int slot) {
        if (slot < 0 || slot > 8) return;
        int vk = 0x31 + slot; // VK_1 = 0x31, VK_2 = 0x32, ..., VK_9 = 0x39
        TriggerBot.sendKeyPress(vk);
    }

    /**
     * Generates a Gaussian-distributed random tick count between min and max.
     * Most values cluster near the center (~68% within ±1σ),
     * mimicking natural human reaction time distribution.
     */
    private static int gaussianTicks(int min, int max) {
        if (min >= max) return min;
        double center = (min + max) / 2.0;
        double sigma = (max - min) / 4.0; // ±2σ covers min-max range
        double value = center + ThreadLocalRandom.current().nextGaussian() * sigma;
        return (int) Math.round(Math.max(min, Math.min(max, value)));
    }

    /** Finds the first sword in the hotbar. Returns slot index or -1. */
    private static int findSwordSlot(ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem().getTranslationKey().toLowerCase().contains("sword")) {
                return i;
            }
        }
        return -1;
    }

    // ── GLFW key → Windows VK code ────────────────────────────────────────────
    public static int glfwKeyToVk(int glfwKey) {
        if (glfwKey >= 65 && glfwKey <= 90) return glfwKey;       // A-Z
        if (glfwKey >= 48 && glfwKey <= 57) return glfwKey;       // 0-9
        if (glfwKey >= 290 && glfwKey <= 301) return 0x70 + (glfwKey - 290); // F1-F12
        switch (glfwKey) {
            case 256: return 0x1B; // ESC
            case 257: return 0x0D; // ENTER
            case 258: return 0x09; // TAB
            case 259: return 0x08; // BACKSPACE
            case 340: return 0xA0; // LEFT SHIFT
            case 341: return 0xA2; // LEFT CTRL
            case 342: return 0xA4; // LEFT ALT
            case 344: return 0xA1; // RIGHT SHIFT
            case 345: return 0xA3; // RIGHT CTRL
            case 32:  return 0x20; // SPACE
            default:  return -1;
        }
    }
}
