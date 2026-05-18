package com.velocity.module.combat;

import com.velocity.config.AimAssistSettings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TriggerBot V2 (Zero-Tick & High Performance).
 * Uses manual mathematical raycasting instead of relying on the client's delayed crosshairTarget.
 * Uses direct interaction packets instead of Win32 SendInput to instantly register attacks with the server.
 *
 * Optimizations:
 *  - getEntitiesByClass() pre-filters at the engine level (skips items, XP orbs, arrows, etc.)
 *  - Tick delta 0.0f for true zero-latency charge & camera detection
 *  - Block raycast skipped for close-range targets (≤2 blocks)
 *  - Weapon-check cache avoids per-tick string allocation
 */
public class TriggerBotV2 {

    private static long nextAttackTime = 0;

    // ── Weapon-check cache (avoids toLowerCase() allocation every tick) ──────
    private static net.minecraft.item.Item lastCheckedItem = null;
    private static boolean lastCheckedIsWeapon = false;

    // ── Squared distance thresholds ─────────────────────────────────────────
    private static final double CULL_DIST_SQ = 8.0 * 8.0;       // Max entity scan range
    private static final double SKIP_WALL_CHECK_SQ = 2.0 * 2.0; // Skip block raycast if this close

    public static void tick(MinecraftClient client) {
        if (!AimAssistSettings.triggerBotEnabled)
            return;

        if (client == null || client.world == null || client.player == null)
            return;
        if (client.player.isDead())
            return;
        if (client.player.isUsingItem())
            return;
        if (client.currentScreen != null)
            return;

        long now = System.currentTimeMillis();
        if (now < nextAttackTime)
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

        // Smart Crit check
        if (AimAssistSettings.triggerBotSmartCrit) {
            // Only delay the attack if we are actively mid-air and moving upwards (waiting for the fall)
            if (!client.player.isOnGround() && client.player.getVelocity().y >= 0) {
                return; // Wait until falling for critical hit
            }
            if (!client.player.isOnGround() && client.player.fallDistance <= 0.0f) {
                return; // Wait until officially falling
            }
        }

        // Zero-delay charge detection: delta 0.0f = exact current tick, no interpolation latency
        float currentCharge = client.player.getAttackCooldownProgress(0.0f);
        boolean isFullCharge = currentCharge >= 1.0f;

        // Low HP Auto-kill logic check
        boolean readyToAttack = isFullCharge;
        if (AimAssistSettings.triggerBotLowHpOverride) {
            // We will decide readiness inside the entity loop below.
        } else if (!isFullCharge) {
            return;
        }

        // Zero-delay camera: delta 0.0f = no interpolation, reports position at exact current tick
        Vec3d cameraPos = client.player.getCameraPosVec(0.0f);
        Vec3d lookVec = client.player.getRotationVec(0.0f);

        // Using standard vanilla 3.0 block entity interaction bounds
        double maxReach = 3.0;
        Vec3d maxReachVec = cameraPos.add(lookVec.multiply(maxReach));

        Entity bestTarget = null;
        double closestDistSq = maxReach * maxReach;

        // ── Engine-level pre-filtered entity scan ────────────────────────────
        // getEntitiesByClass() only returns LivingEntity within the search box,
        // skipping all items, XP orbs, arrows, falling blocks, etc.
        double scanRadius = 8.0;
        Box searchBox = new Box(
                cameraPos.x - scanRadius, cameraPos.y - scanRadius, cameraPos.z - scanRadius,
                cameraPos.x + scanRadius, cameraPos.y + scanRadius, cameraPos.z + scanRadius
        );

        for (LivingEntity target : client.world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
            if (target == client.player) continue;

            // Early distance cull — skip entities beyond 8 blocks (cheap squared-distance check)
            if (cameraPos.squaredDistanceTo(target.getEntityPos()) > CULL_DIST_SQ) continue;

            // Filters
            if (target.isDead() || target.getHealth() <= 0) continue;
            if (target.isInvulnerable() || target.hurtTime > 0) continue;
            if (AimAssistSettings.ignoreTeammates && target instanceof net.minecraft.entity.player.PlayerEntity && client.player.isTeammate(target)) continue;
            if (com.velocity.config.EspSettings.friendsSystemEnabled && target instanceof net.minecraft.entity.player.PlayerEntity && com.velocity.config.FriendManager.isFriend(target.getName().getString())) continue;
            
            // Raytrace mathematical collision (vanilla hitbox, no expansion)
            Box box = target.getBoundingBox();
            
            Optional<Vec3d> hitPoint = box.raycast(cameraPos, maxReachVec);
            if (hitPoint.isPresent()) {
                double distSq = cameraPos.squaredDistanceTo(hitPoint.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    bestTarget = target;
                }
            }
        }

        if (bestTarget == null) {
            return;
        }

        // We have a mathematical hit!
        LivingEntity target = (LivingEntity) bestTarget;

        // Block Raycast (Wall Check) — skip for very close targets (≤2 blocks)
        // At point-blank range there physically can't be a wall between you and the target
        if (closestDistSq > SKIP_WALL_CHECK_SQ) {
            net.minecraft.world.RaycastContext context = new net.minecraft.world.RaycastContext(
                    cameraPos, target.getBoundingBox().getCenter(),
                    net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                    net.minecraft.world.RaycastContext.FluidHandling.NONE,
                    client.player
            );
            net.minecraft.util.hit.BlockHitResult blockHit = client.world.raycast(context);
            if (blockHit != null && blockHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                // A block is in the way!
                return;
            }
        }

        if (!readyToAttack && AimAssistSettings.triggerBotLowHpOverride) {
            double baseDamage = client.player.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ATTACK_DAMAGE);
            double scaledDamage = Math.max(0.5, currentCharge * baseDamage);
            if (scaledDamage >= target.getHealth()) {
                readyToAttack = true; // Enough damage to execute
            }
        }

        if (!readyToAttack) {
            return; // Not fully charged or not eligible for execution
        }

        boolean clicked = false;
        
        switch (AimAssistSettings.triggerBotClickMethod) {
            case 0: // Win32
                clicked = TriggerBot.sendLeftClick();
                break;
            case 1: // Internal
                TriggerBot.wantsInternalClick = true;
                clicked = true;
                break;
            case 2: // Mix
                TriggerBot.wantsInternalClick = true;
                boolean win32Success = TriggerBot.sendLeftClick();
                clicked = true;
                break;
        }

        if (clicked) {
            int minDelay = AimAssistSettings.triggerBotMinDelayMs;
            int maxDelay = AimAssistSettings.triggerBotMaxDelayMs;
            int delay = minDelay;
            if (maxDelay > minDelay) {
                delay += ThreadLocalRandom.current().nextInt(maxDelay - minDelay);
            }
            nextAttackTime = now + delay;
        }
    }
}
