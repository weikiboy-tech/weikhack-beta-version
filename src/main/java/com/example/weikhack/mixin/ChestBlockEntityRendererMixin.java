package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackChestEspRenderer;
import net.minecraft.client.render.block.entity.ChestBlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntityRenderer.class)
public class ChestBlockEntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/block/entity/state/ChestBlockEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", shift = At.Shift.BEFORE)
    )
    private void weikhack$renderChestEspOutline(
            ChestBlockEntityRenderState state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        WeikhackChestEspRenderer.submitChestBlockEntityOutline(state, matrices, queue);
    }
}
