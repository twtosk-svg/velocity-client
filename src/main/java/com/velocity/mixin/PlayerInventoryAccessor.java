package com.velocity.mixin;

import com.velocity.module.utility.HealKeybind;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes PlayerInventory.selectedSlot for reading and writing
 * from outside the mixin system (e.g. HealKeybind).
 */
@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {

    @Accessor("selectedSlot")
    int getSelectedSlot();

    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);
}
