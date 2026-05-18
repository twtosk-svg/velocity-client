package com.velocity.mixin;

import com.velocity.module.utility.HoverRefill;
import com.velocity.core.EspRenderer;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes HandledScreen.focusedSlot (the slot currently under the cursor)
 * so HoverRefill can read it from EspRenderer without relying on a private
 * method shadow.
 */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("focusedSlot")
    Slot getFocusedSlot();
}
