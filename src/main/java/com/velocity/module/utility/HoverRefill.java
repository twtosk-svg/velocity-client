package com.velocity.module.utility;

import com.velocity.config.UtilitySettings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Hover Refill — when the player hovers over ANY healing potion (Instant Health I
 * or II, regular/splash/lingering) in their inventory screen, it is immediately
 * quick-moved to the hotbar.
 *
 * Covers ALL healing potion variants because detection is purely component-based.
 * Hotbar availability is checked via player.getInventory() directly (not screen
 * handler slot indices) so it works identically in the player inventory screen
 * AND in any chest/container screen.
 */
public class HoverRefill {

    private static long lastMoveTime = 0;
    private static final long COOLDOWN_MS = 1;

    /** Fast check used by the mixin to skip work when the feature is off. */
    public static boolean isEnabled() {
        return UtilitySettings.hoverRefillEnabled;
    }

    public static void tick(MinecraftClient client, int hoveredSlotIndex) {
        if (!UtilitySettings.hoverRefillEnabled) return;
        if (client == null || client.player == null || client.interactionManager == null) return;
        if (hoveredSlotIndex < 0) return;

        long now = System.currentTimeMillis();
        if (now - lastMoveTime < COOLDOWN_MS) return;

        ClientPlayerEntity player = client.player;
        if (player.currentScreenHandler == null) return;

        int slotCount = player.currentScreenHandler.slots.size();
        if (hoveredSlotIndex >= slotCount) return;

        // Check that the hovered item is actually a healing potion
        ItemStack stack = player.currentScreenHandler.slots.get(hoveredSlotIndex).getStack();
        if (!isHealingPotion(stack)) return;

        // ── Hotbar free-slot check ────────────────────────────────────────────
        // Use player.getInventory().getStack(0..8) — the real hotbar slots — so
        // this check is correct for BOTH PlayerScreenHandler and any chest handler.
        boolean hotbarHasFreeSlot = false;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = player.getInventory().getStack(i);
            if (hotbarStack.isEmpty() || !isHealingPotion(hotbarStack)) {
                hotbarHasFreeSlot = true;
                break;
            }
        }
        if (!hotbarHasFreeSlot) return;

        // ── Send QUICK_MOVE to server ─────────────────────────────────────────
        try {
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    hoveredSlotIndex,
                    0,
                    SlotActionType.QUICK_MOVE,
                    player
            );
            lastMoveTime = now;
        } catch (Exception e) {
            System.err.println("[Velocity] HoverRefill.tick error: " + e.getMessage());
        }
    }

    /**
     * Returns true if the given ItemStack is ANY healing potion:
     *   - Potion of Instant Health I or II
     *   - Splash Potion of Instant Health I or II
     *   - Lingering Potion of Instant Health I or II
     *
     * Detection is purely component-based — works on all item types that carry a
     * POTION_CONTENTS component with an INSTANT_HEALTH effect.
     */
    public static boolean isHealingPotion(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return false;

        for (net.minecraft.entity.effect.StatusEffectInstance effectInstance : contents.getEffects()) {
            if (effectInstance.getEffectType().value() == StatusEffects.INSTANT_HEALTH.value()) {
                return true;
            }
        }
        return false;
    }
}
