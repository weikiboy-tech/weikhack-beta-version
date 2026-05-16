package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackChestEspRenderer;
import com.example.weikhack.WeikhackMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void weikhack$renderActiveModules(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        WeikhackChestEspRenderer.renderHud(context);

        if (!WeikhackMod.isActiveListEnabled()) {
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) {
            return;
        }

        int y = 6;
        for (String module : WeikhackMod.getActiveModuleNames()) {
            context.drawTextWithShadow(client.textRenderer, Text.literal(module), 6, y, 0xFF6EE7B7);
            y += 10;
        }
    }
}
