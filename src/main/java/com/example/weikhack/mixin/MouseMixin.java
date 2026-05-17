package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Mouse.class)
public class MouseMixin {
    @Redirect(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void weikhack$redirectFreecamLook(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        if (!WeikhackMod.handleFreecamLook(cursorDeltaX, cursorDeltaY)) {
            player.changeLookDirection(cursorDeltaX, cursorDeltaY);
        }
    }
}
