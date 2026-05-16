package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackCommands;
import com.example.weikhack.WeikhackMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Unique
    private Vec3d weikhack$velocityBeforeExplosion;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void weikhack$handleChatCommand(String message, CallbackInfo ci) {
        if (WeikhackCommands.handleChatMessage(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void weikhack$cancelVelocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (WeikhackMod.isNoKnockbackEnabled() && client.player != null && packet.getEntityId() == client.player.getId()) {
            ci.cancel();
        }
    }

    @Inject(method = "onExplosion", at = @At("HEAD"))
    private void weikhack$rememberExplosionVelocity(ExplosionS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (WeikhackMod.isNoKnockbackEnabled() && client.player != null && packet.playerKnockback().isPresent()) {
            weikhack$velocityBeforeExplosion = client.player.getVelocity();
            return;
        }

        weikhack$velocityBeforeExplosion = null;
    }

    @Inject(method = "onExplosion", at = @At("TAIL"))
    private void weikhack$restoreExplosionVelocity(ExplosionS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (WeikhackMod.isNoKnockbackEnabled() && client.player != null && weikhack$velocityBeforeExplosion != null) {
            client.player.setVelocity(weikhack$velocityBeforeExplosion);
        }
        weikhack$velocityBeforeExplosion = null;
    }
}
