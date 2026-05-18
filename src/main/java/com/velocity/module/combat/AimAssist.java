package com.velocity.module.combat;

import com.velocity.config.AimAssistSettings;
import com.velocity.core.EspRenderer;
import com.velocity.core.Win32Setup;
import com.velocity.mixin.MouseInputMixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Aim-assist core logic — Raven-style dual-speed algorithm.
 *
 * INPUT ARCHITECTURE — avoiding the feedback loop:
 * ──────────────────────────────────────────────────
 * Detection: MouseInputMixin on Mouse.onCursorPos — only fires for real OS
 * mouse events.
 * Application: direct player.setYaw() / setPitch() writes — does NOT generate
 * mouse events.
 * ⇒ The mixin never sees our angle corrections ⇒ no feedback loop.
 *
 * GCD: Minecraft quantises mouse-derived yaw/pitch deltas to
 * gcd = (sensitivity × 0.6 + 0.2)³ × 8
 * We snap every delta to the nearest GCD multiple before applying it.
 *
 * Raven algorithm: uses two speed values per axis.
 * Speed 1 (main pull) and Speed 2 (compliment / randomized modifier)
 * to create natural-looking aim correction with built-in jitter.
 *
 * Free movement (Fusion-style):
 * Yaw is corrected toward the target. Pitch is free within [head..feet] range.
 * If the player's pitch is already between head-pitch and feet-pitch, we leave
 * it alone. If it goes above the head or below the feet, we clamp it back.
 */
public class AimAssist {

    // ── Timer ─────────────────────────────────────────────────────────────────
    private static long assistActiveUntil = 0;

    // ── Current target (valid even when timer is expired, for nametag glow) ──
    private static Entity targetCandidate = null;

    // ── GCD cache ─────────────────────────────────────────────────────────────
    private static double cachedGcd = -1;
    private static double lastSensitivity = -1;

    // ── Focus Tracking ────────────────────────────────────────────────────────
    private static boolean focusMode = false;
    private static Entity focusedEntity = null;
    private static boolean focusKeyWasDown = false;

    // ── Deadzone tracking ─────────────────────────────────────────────────────
    private static double lastCursorX = Double.NaN;
    private static double lastCursorY = Double.NaN;

    // ── Pre-allocated buffers (zero GC in hot path) ──────────────────────────
    private static final double[] ANGLE_BUF = new double[2]; // reused by angleDelta()
    private static final Vec3d[] BODY_PTS = new Vec3d[3];    // reused by getBodyPoints()

    // ─────────────────────────────────────────────────────────────────────────
    // Called by MouseInputMixin whenever the user physically moves the mouse.
    // We store the raw cursor position and evaluate the deadzone.
    // ─────────────────────────────────────────────────────────────────────────
    public static void onRawMouseInput(double cursorX, double cursorY) {
        if (!AimAssistSettings.enabled)
            return;

        // Don't trigger aim assist while a GUI screen is open
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.currentScreen != null)
            return;

        // Compute delta from previous position
        if (!Double.isNaN(lastCursorX)) {
            double dx = cursorX - lastCursorX;
            double dy = cursorY - lastCursorY;
            double moveMagnitude = Math.sqrt(dx * dx + dy * dy);

            // Deadzone: ignore tiny movements (hand tremors)
            if (moveMagnitude < AimAssistSettings.deadzonePixels) {
                lastCursorX = cursorX;
                lastCursorY = cursorY;
                return;
            }

            // Direction check: only trigger if movement is roughly toward the target
            if (AimAssistSettings.deadzoneDirectionCheck && targetCandidate != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    Vec3d eyePos = client.player.getEyePos();
                    Vec3d targetPos = getTargetPoint(targetCandidate);
                    double[] angleDelta = angleDelta(eyePos, targetPos,
                            client.player.getYaw(), client.player.getPitch());

                    // Check if mouse direction aligns with target direction (within ~90°)
                    double dot = dx * angleDelta[0] + dy * angleDelta[1];
                    if (dot < 0) {
                        // Moving away from target — don't trigger assist
                        lastCursorX = cursorX;
                        lastCursorY = cursorY;
                        return;
                    }
                }
            }

            assistActiveUntil = System.currentTimeMillis() + AimAssistSettings.assistDurationMs;
        }

        lastCursorX = cursorX;
        lastCursorY = cursorY;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called every frame from EspRenderer.render().
    // tickDelta is used for frame-rate-independent smoothing.
    // ─────────────────────────────────────────────────────────────────────────
    public static void tick(MinecraftClient client, float tickDelta) {
        if (client == null || client.world == null || client.player == null) {
            targetCandidate = null;
            return;
        }

        // Don't aim-assist while a GUI screen is open (inventory, chat, etc.)
        if (client.currentScreen != null) {
            targetCandidate = null;
            return;
        }

        if (!AimAssistSettings.enabled) {
            targetCandidate = null;
            return;
        }

        // Focus mode toggle logic
        if (AimAssistSettings.focusKeybindKey >= 0) {
            boolean focusKeyDown = (Win32Setup.INSTANCE.GetAsyncKeyState(AimAssistSettings.focusKeybindKey) & 0x8000) != 0;
            if (focusKeyDown && !focusKeyWasDown) {
                focusMode = !focusMode;
                if (!focusMode) {
                    focusedEntity = null;
                } else if (targetCandidate != null) {
                    focusedEntity = targetCandidate;
                }
            }
            focusKeyWasDown = focusKeyDown;
        } else {
            focusMode = false;
            focusedEntity = null;
            focusKeyWasDown = false;
        }

        if (focusMode && focusedEntity != null) {
            if (!isValidTarget(focusedEntity) || focusedEntity.isRemoved() || !focusedEntity.isAlive()) {
                focusMode = false;
                focusedEntity = null;
            }
        }

        // ── 1. Find best target ──────────────────────────────────────────────
        if (focusMode) {
            if (focusedEntity != null) {
                targetCandidate = focusedEntity;
            } else {
                targetCandidate = findBestTarget(client);
                if (targetCandidate != null) {
                    focusedEntity = targetCandidate;
                }
            }
        } else {
            targetCandidate = findBestTarget(client);
        }

        // ── 2. Mouse4 (XBUTTON1) aim trigger ─────────────────────────────────
        if (AimAssistSettings.mouse4Aim
                && (Win32Setup.INSTANCE.GetAsyncKeyState(Win32Setup.VK_XBUTTON1) & 0x8000) != 0) {
            assistActiveUntil = System.currentTimeMillis() + AimAssistSettings.assistDurationMs;
        }

        // ── 3. If timer active and we have a target, apply aim correction ────
        if (targetCandidate != null && System.currentTimeMillis() < assistActiveUntil) {
            boolean apply = true;
            if (focusMode) {
                if (client.player.distanceTo(targetCandidate) > AimAssistSettings.maxDistance) {
                    apply = false;
                } else {
                    Vec3d eyePos = client.player.getEyePos();
                    float playerYaw = client.player.getYaw();
                    float playerPitch = client.player.getPitch();
                    Vec3d[] checkPoints = getBodyPoints(targetCandidate);
                    double smallestAngle = Double.MAX_VALUE;
                    for (Vec3d point : checkPoints) {
                        double[] delta = angleDelta(eyePos, point, playerYaw, playerPitch);
                        double totalAngle = Math.sqrt(delta[0] * delta[0] + delta[1] * delta[1]);
                        if (totalAngle < smallestAngle) smallestAngle = totalAngle;
                    }
                    if (smallestAngle > AimAssistSettings.fovDegrees) apply = false;
                    if (AimAssistSettings.visibilityCheck && !isVisible(client, eyePos, targetCandidate)) apply = false;
                }
            }
            
            if (apply) {
                applyAim(client, targetCandidate, tickDelta);
            }
        }
    }

    /**
     * Returns the current target candidate for nametag-glow rendering.
     * May be non-null even when the assist timer is expired.
     */
    public static Entity getTargetCandidate() {
        return targetCandidate;
    }

    /**
     * Returns true if focus mode is actively toggled ON.
     */
    public static boolean isFocusMode() {
        return focusMode;
    }

    /**
     * Returns true if the assist timer is currently active
     * (i.e. ongoing aim correction is being applied).
     */
    public static boolean isAssisting() {
        return AimAssistSettings.enabled && System.currentTimeMillis() < assistActiveUntil;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Target selection
    // ═════════════════════════════════════════════════════════════════════════

    private static Entity findBestTarget(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        float playerYaw = client.player.getYaw();
        float playerPitch = client.player.getPitch();

        Entity best = null;
        double bestAngle = Double.MAX_VALUE;
        double maxDistSq = AimAssistSettings.maxDistance * AimAssistSettings.maxDistance;
        double fovDegreesSq = AimAssistSettings.fovDegrees * AimAssistSettings.fovDegrees;

        for (Entity entity : client.world.getEntities()) {
            // Distance filter (squared is much faster and bypasses allocating body points)
            if (client.player.squaredDistanceTo(entity) > maxDistSq)
                continue;

            if (!isValidTarget(entity))
                continue;

            Vec3d[] checkPoints = getBodyPoints(entity);
            double smallestAngle = Double.MAX_VALUE;
            boolean withinFov = false;

            for (Vec3d point : checkPoints) {
                double[] delta = angleDelta(eyePos, point, playerYaw, playerPitch);
                double totalAngleSq = delta[0] * delta[0] + delta[1] * delta[1];
                
                // Only sqrt if within the squared FOV bounds (optimization)
                if (totalAngleSq <= fovDegreesSq) {
                    withinFov = true;
                    double totalAngle = Math.sqrt(totalAngleSq);
                    if (totalAngle < smallestAngle) {
                        smallestAngle = totalAngle;
                    }
                }
            }

            // FOV filter
            if (!withinFov)
                continue;

            // Lazy exit: Don't bother doing expensive raycasts if this entity 
            // is not closer to the crosshair than our current best target!
            if (smallestAngle >= bestAngle)
                continue;

            // Visibility filter (Expensive raycast)
            if (AimAssistSettings.visibilityCheck && !isVisible(client, eyePos, entity))
                continue;

            bestAngle = smallestAngle;
            best = entity;
        }
        return best;
    }

    private static boolean isValidTarget(Entity entity) {
        // Invisible check
        if (entity.isInvisible())
            return false;
            
        // Invulnerability check
        if (entity.isInvulnerable())
            return false;

        // Dead check: skip dead entities
        if (entity instanceof LivingEntity living) {
            if (living.isDead() || living.getHealth() <= 0)
                return false;
        }

        if (entity instanceof ZombieEntity && AimAssistSettings.targetZombies)
            return true;
        if (entity instanceof VillagerEntity && AimAssistSettings.targetVillagers)
            return true;
        if (entity instanceof net.minecraft.entity.passive.BatEntity && AimAssistSettings.targetBats)
            return true;
        if (entity instanceof net.minecraft.entity.passive.RabbitEntity && AimAssistSettings.targetRabbits)
            return true;
        if (entity instanceof PlayerEntity player && AimAssistSettings.targetPlayers) {
            // Skip spectator and creative players
            if (player.isSpectator() || player.isCreative())
                return false;
            // Teammate check
            if (AimAssistSettings.ignoreTeammates && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.isTeammate(player))
                return false;
            // Friend check
            if (com.velocity.config.EspSettings.friendsSystemEnabled && com.velocity.config.FriendManager.isFriend(player.getName().getString()))
                return false;
            return true;
        }
        return false;
    }

    /**
     * Returns the point we aim at — entity eye position for living entities.
     */
    static Vec3d getTargetPoint(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.getEyePos();
        }
        return entity.getEntityPos().add(0, entity.getHeight() / 2.0, 0);
    }

    /**
     * Returns three points on the entity's body: head (top), center, and feet.
     * Used for FOV gating so any visible body part counts.
     */
    private static Vec3d[] getBodyPoints(Entity entity) {
        Vec3d feet = entity.getEntityPos();
        double height = entity.getHeight();
        BODY_PTS[0] = feet.add(0, height, 0);       // head
        BODY_PTS[1] = feet.add(0, height * 0.5, 0); // center
        BODY_PTS[2] = feet;                          // feet
        return BODY_PTS;
    }

    /**
     * Returns the pitch angle to the entity's head (top of bounding box).
     */
    private static float pitchToHead(Vec3d eyePos, Entity entity) {
        Vec3d headPos = entity.getEntityPos().add(0, entity.getHeight(), 0);
        return pitchTo(eyePos, headPos);
    }

    /**
     * Returns the pitch angle to the entity's feet (bottom of bounding box).
     */
    private static float pitchToFeet(Vec3d eyePos, Entity entity) {
        Vec3d feetPos = entity.getEntityPos();
        return pitchTo(eyePos, feetPos);
    }

    private static float pitchTo(Vec3d eyePos, Vec3d target) {
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    /**
     * Writes {deltaYaw, deltaPitch} into the pre-allocated ANGLE_BUF and returns it.
     * Callers must read values before the next call (single-threaded render thread).
     */
    static double[] angleDelta(Vec3d eyePos, Vec3d target, float playerYaw, float playerPitch) {
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz)); // MC convention
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        ANGLE_BUF[0] = MathHelper.wrapDegrees(targetYaw - playerYaw);
        ANGLE_BUF[1] = MathHelper.wrapDegrees(targetPitch - playerPitch);
        return ANGLE_BUF;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Visibility check (raycast)
    // ═════════════════════════════════════════════════════════════════════════

    private static boolean isVisible(MinecraftClient client, Vec3d eyePos, Entity target) {
        Vec3d targetEyes = getTargetPoint(target);
        RaycastContext ctx = new RaycastContext(
                eyePos, targetEyes,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player);
        BlockHitResult result = client.world.raycast(ctx);
        return result.getType() == HitResult.Type.MISS
                || result.getPos().squaredDistanceTo(targetEyes) < 1.0;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GCD computation
    // ═════════════════════════════════════════════════════════════════════════

    private static double getMouseMultiplier(MinecraftClient client) {
        double sensitivity = client.options.getMouseSensitivity().getValue();
        if (sensitivity != lastSensitivity) {
            lastSensitivity = sensitivity;
            double d = sensitivity * 0.6 + 0.2;
            cachedGcd = d * d * d * 8.0;
        }
        return cachedGcd;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Raven-style aim correction
    // ═════════════════════════════════════════════════════════════════════════

    private static void applyAim(MinecraftClient client, Entity target, float tickDelta) {
        Vec3d eyePos = client.player.getEyePos();
        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        double mouseMult = getMouseMultiplier(client);

        // ── Free movement (Fusion-style): yaw-only with pitch clamping ───────
        if (AimAssistSettings.freeMovement) {
            applyFreeMovement(client, target, tickDelta, eyePos, currentYaw, currentPitch, mouseMult);
            return;
        }

        // ── Standard mode: correct both yaw and pitch (Raven formula) ────────
        Vec3d targetPos = getTargetPoint(target);
        double[] delta = angleDelta(eyePos, targetPos, currentYaw, currentPitch);

        double remainYaw = delta[0];
        double remainPitch = delta[1];

        // ── Yaw correction ───────────────────────────────────────────────────
        int pixelsX = 0;
        if (remainYaw > 1.0 || remainYaw < -1.0) {
            double complimentSpeed = remainYaw
                    * (ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.complimentYaw - 1.47328,
                            AimAssistSettings.complimentYaw + 2.48293) / 100.0);
            double val = complimentSpeed + (remainYaw / (101.0
                    - (float) ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.speedYaw - 4.723847,
                            AimAssistSettings.speedYaw)));
            pixelsX = (int) Math.round(val / (mouseMult * 0.15));
        }

        // ── Pitch correction ─────────────────────────────────────────────────
        int pixelsY = 0;
        if (remainPitch > 1.0 || remainPitch < -1.0) {
            double complimentSpeed = remainPitch
                    * (ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.complimentPitch - 1.47328,
                            AimAssistSettings.complimentPitch + 2.48293) / 100.0);
            double val = complimentSpeed + (remainPitch / (101.0
                    - (float) ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.speedPitch - 4.723847,
                            AimAssistSettings.speedPitch)));
            pixelsY = (int) Math.round(val / (mouseMult * 0.15));
        }

        if (pixelsX != 0 || pixelsY != 0) {
            client.player.changeLookDirection(pixelsX * mouseMult, pixelsY * mouseMult);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Fusion-style free movement (Raven yaw + pitch clamping)
    // ═════════════════════════════════════════════════════════════════════════
    // Yaw: corrected toward target using Raven formula.
    // Pitch: FREE within [headPitch..feetPitch] range. If the player tries to
    // look above the head or below the feet, we clamp them back (smoothly).

    private static void applyFreeMovement(MinecraftClient client, Entity target,
            float tickDelta, Vec3d eyePos,
            float currentYaw, float currentPitch,
            double mouseMult) {

        Vec3d targetPos = getTargetPoint(target);
        double[] delta = angleDelta(eyePos, targetPos, currentYaw, currentPitch);
        double remainYaw = delta[0];

        // ── Yaw correction (Raven formula) ───────────────────────────────────
        int pixelsX = 0;
        if (remainYaw > 1.0 || remainYaw < -1.0) {
            double complimentSpeed = remainYaw
                    * (ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.complimentYaw - 1.47328,
                            AimAssistSettings.complimentYaw + 2.48293) / 100.0);
            double val = complimentSpeed + (remainYaw / (101.0
                    - (float) ThreadLocalRandom.current().nextDouble(
                            AimAssistSettings.speedYaw - 4.723847,
                            AimAssistSettings.speedYaw)));
            pixelsX = (int) Math.round(val / (mouseMult * 0.15));
        }

        // ── Pitch clamping (free within head-to-feet range) ──────────────────
        float headPitch = pitchToHead(eyePos, target);
        float feetPitch = pitchToFeet(eyePos, target);

        // Ensure min <= max (headPitch is usually smaller/more negative = looking up)
        float minPitch = Math.min(headPitch, feetPitch);
        float maxPitch = Math.max(headPitch, feetPitch);

        int pixelsY = 0;
        if (currentPitch < minPitch) {
            // Player is aiming above the head — smoothly pull down
            double pullDown = (minPitch - currentPitch) * 0.3 * Math.min(tickDelta, 2.0);
            pixelsY = (int) Math.round(pullDown / (mouseMult * 0.15));
        } else if (currentPitch > maxPitch) {
            // Player is aiming below the feet — smoothly pull up
            double pullUp = (maxPitch - currentPitch) * 0.3 * Math.min(tickDelta, 2.0);
            pixelsY = (int) Math.round(pullUp / (mouseMult * 0.15));
        }
        // Otherwise pitch is within range — don't touch it (free movement)

        if (pixelsX != 0 || pixelsY != 0) {
            client.player.changeLookDirection(pixelsX * mouseMult, pixelsY * mouseMult);
        }
    }
}
