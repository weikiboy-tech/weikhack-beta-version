package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityTeamColorMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void weikhack$espTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (WeikhackMod.shouldEspHighlight((Entity) (Object) this)) {
            cir.setReturnValue(0x00FF00);
        }
    }
}
