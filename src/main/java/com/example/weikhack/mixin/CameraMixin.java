package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void weikhack$applyFreecamCamera(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        if (!WeikhackMod.isFreecamEnabled()) {
            return;
        }

        Vec3d position = WeikhackMod.getFreecamRenderPosition(tickProgress);
        if (position == null) {
            return;
        }

        setPos(position);
        setRotation(WeikhackMod.getFreecamYaw(), WeikhackMod.getFreecamPitch());
    }
}
