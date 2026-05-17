package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true)
    private void weikhack$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (WeikhackMod.isSafeWalkEnabled() && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
