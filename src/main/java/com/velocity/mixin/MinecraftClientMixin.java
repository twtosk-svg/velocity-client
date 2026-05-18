package com.velocity.mixin;

import com.velocity.gui.OverlayManager;
import com.velocity.core.EspRenderer;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private static boolean initializedOverlay = false;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderEnd(boolean tick, CallbackInfo ci) {
        if (!initializedOverlay) {
            OverlayManager.init();
            initializedOverlay = true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        EspRenderer.render(client.getRenderTickCounter().getTickProgress(true));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (com.velocity.module.combat.TriggerBot.wantsInternalClick) {
            com.velocity.module.combat.TriggerBot.wantsInternalClick = false;
            ((MinecraftClientAccessor) this).invokeDoAttack();
        }
    }
}
