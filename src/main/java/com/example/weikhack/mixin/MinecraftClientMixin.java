package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void weikhack$tick(CallbackInfo ci) {
        WeikhackMod.onClientTick((MinecraftClient) (Object) this);
    }
}
