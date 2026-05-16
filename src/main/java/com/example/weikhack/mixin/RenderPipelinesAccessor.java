package com.example.weikhack.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("RENDERTYPE_LINES_SNIPPET")
    static RenderPipeline.Snippet weikhack$getLinesSnippet() {
        throw new AssertionError();
    }

    @Invoker("register")
    static RenderPipeline weikhack$register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}
