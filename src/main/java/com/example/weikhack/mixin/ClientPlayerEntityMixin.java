package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void weikhack$noSlowdown(CallbackInfoReturnable<Boolean> cir) {
        if (WeikhackMod.isNoSlowdownEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getActiveItemSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void weikhack$keepItemUseSpeed(CallbackInfoReturnable<Float> cir) {
        if (WeikhackMod.isNoSlowdownEnabled()) {
            cir.setReturnValue(1.0F);
        }
    }
}
