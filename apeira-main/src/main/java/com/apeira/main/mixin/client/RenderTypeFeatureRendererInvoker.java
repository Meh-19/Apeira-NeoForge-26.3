package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Lets {@link BadgerOutlineFeatureMixin} get a vertex builder for its own render type. */
@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererInvoker {
    @Invoker("getVertexBuilder")
    VertexConsumer apeira$getVertexBuilder(RenderType renderType);
}
