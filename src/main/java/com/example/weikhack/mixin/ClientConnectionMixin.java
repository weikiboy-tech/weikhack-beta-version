package com.example.weikhack.mixin;

import com.example.weikhack.WeikhackMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void weikhack$cancelFreecamMovementPackets(Packet<?> packet, CallbackInfo ci) {
        if (WeikhackMod.shouldCancelFreecamMovementPacket(packet)) {
            ci.cancel();
        } else if (WeikhackMod.shouldDelayFakeLagPacket((ClientConnection) (Object) this, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void weikhack$cancelFreecamMovementPacketsWithListener(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, CallbackInfo ci) {
        if (WeikhackMod.shouldCancelFreecamMovementPacket(packet)) {
            ci.cancel();
        } else if (listener == null && WeikhackMod.shouldDelayFakeLagPacket((ClientConnection) (Object) this, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void weikhack$cancelFreecamMovementPacketsWithFlush(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (WeikhackMod.shouldCancelFreecamMovementPacket(packet)) {
            ci.cancel();
        } else if (listener == null && WeikhackMod.shouldDelayFakeLagPacket((ClientConnection) (Object) this, packet)) {
            ci.cancel();
        }
    }
}
