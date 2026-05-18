package com.velocity.module.combat;

import com.velocity.config.AimAssistSettings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * ShieldBreaker — automatically attacks to disable a shield when:
 * 1. Your crosshair is over a player using a shield.
 * 2. You are holding an Axe.
 * 3. Your attack cooldown exceeds the minimum threshold specified by the user.
 * 
 * Invokes Win32 SendInput to perfectly simulate legitimate human clicks to bypass Grim & NCP.
 */
public class ShieldBreaker {
    
    private static long nextAttackTime = 0;

    // ── Axe-check cache (avoids toLowerCase() allocation every tick) ─────────
    private static Item lastCheckedItem = null;
    private static boolean lastCheckedIsAxe = false;

    public static void tick(MinecraftClient client) {
        if (!AimAssistSettings.shieldBreakerEnabled) return;
        if (client == null || client.world == null || client.player == null) return;
        if (client.player.isDead()) return;
        if (client.player.isUsingItem()) return;
        if (client.currentScreen != null) return;
        
        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;

        // Ensure we are holding an Axe (cached — no string alloc unless item changes)
        Item heldItem = client.player.getMainHandStack().getItem();
        if (heldItem != lastCheckedItem) {
            lastCheckedIsAxe = heldItem.getTranslationKey().toLowerCase().contains("_axe");
            lastCheckedItem = heldItem;
        }
        if (!lastCheckedIsAxe) {
            return;
        }

        // Validate cooldown
        float progress = client.player.getAttackCooldownProgress(0.5f) * 100f; // Scale 0.0 to 100.0
        if (progress < AimAssistSettings.shieldBreakerMinCooldown) {
            return;
        }

        // Check crosshair target
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        
        if (target.isDead() || target.getHealth() <= 0) return;
        if (target.isInvulnerable() || target.hurtTime > 0) return;
        if (AimAssistSettings.ignoreTeammates && target instanceof net.minecraft.entity.player.PlayerEntity && client.player.isTeammate(target)) return;

        // Fast Shield Check (No 5-tick server lag)
        boolean isShielding = target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem;
        if (!isShielding) {
            return;
        }

        // Disable Shield!
        TriggerBot.sendLeftClick();
        
        // Anti-cheat delay with jitter to prevent pattern detection
        int jitter = java.util.concurrent.ThreadLocalRandom.current().nextInt(-50, 51);
        nextAttackTime = now + 250 + jitter;
    }
}
