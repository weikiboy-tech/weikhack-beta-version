package com.example.weikhack.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLayer.class)
public interface RenderLayerAccessor {
    @Invoker("of")
    static RenderLayer weikhack$of(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
