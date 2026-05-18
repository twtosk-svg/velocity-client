package com.velocity.mixin;

import com.velocity.module.combat.AimAssist;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects REAL user mouse movement.
 *
 * This fires only for genuine OS-level cursor-position events (hardware mouse,
 * touchpad, etc.). Our aim-assist applies corrections via direct
 * player.setYaw()/setPitch() writes, which do NOT flow through this callback.
 *
 * We pass the raw cursor X/Y so AimAssist can evaluate the deadzone
 * (ignore micro-movements / hand tremors) and direction check.
 */
@Mixin(Mouse.class)
public class MouseInputMixin {

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onCursorPosHead(long window, double x, double y, CallbackInfo ci) {
        AimAssist.onRawMouseInput(x, y);
    }
}
