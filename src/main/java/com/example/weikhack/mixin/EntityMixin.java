package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void weikhack$noBlockSlowdown(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if (WeikhackMod.isNoSlowdownEnabled() && (Object) this == MinecraftClient.getInstance().player) {
            ci.cancel();
        }
    }

}
