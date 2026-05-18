package com.velocity.mixin;

import com.velocity.module.utility.HoverRefill;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into HandledScreen to detect the currently-hovered slot
 * and pass it to HoverRefill every frame.
 *
 * We inject at the HEAD of render so we always have up-to-date mouseX/Y,
 * and use the public getSlotAt() method rather than shadowing focusedSlot
 * (more reliable across sub-classes).
 */
@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin {

    /** Exposes the protected getSlotAt so we can call it from within the mixin. */
    @Shadow
    protected abstract Slot getSlotAt(double x, double y);

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (!HoverRefill.isEnabled()) return;

        int slotIndex = -1;
        try {
            Slot slot = getSlotAt(mouseX, mouseY);
            if (slot != null) {
                slotIndex = slot.id;
            }
        } catch (Exception ignored) {}

        HoverRefill.tick(client, slotIndex);
    }
}
