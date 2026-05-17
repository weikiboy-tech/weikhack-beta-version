package com.example.weikhack.mixin;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("selectedSlot")
    int weikhack$getSelectedSlot();

    @Accessor("selectedSlot")
    void weikhack$setSelectedSlot(int selectedSlot);
}
