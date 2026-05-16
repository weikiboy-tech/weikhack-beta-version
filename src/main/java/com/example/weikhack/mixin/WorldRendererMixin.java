package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackChestEspRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderBlockDamage", at = @At("HEAD"))
    private void weikhack$renderChestEsp(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate vertexConsumers,
            WorldRenderState renderState,
            CallbackInfo ci
    ) {
        WeikhackChestEspRenderer.render(vertexConsumers, matrices, renderState);
    }
}
