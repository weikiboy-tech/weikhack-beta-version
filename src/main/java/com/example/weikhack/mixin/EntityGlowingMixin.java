package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityGlowingMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void weikhack$forceEspGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (WeikhackMod.shouldEspHighlight((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
