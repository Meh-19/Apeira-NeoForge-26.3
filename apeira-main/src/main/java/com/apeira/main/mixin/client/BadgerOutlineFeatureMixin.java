package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.client.crate.SelectionOutline;

import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.resources.model.geometry.BakedQuad;

/** Draws {@link SelectionOutline#MARK} submits in plain white with {@link SelectionOutline}'s render type. */
@Mixin(ItemFeatureRenderer.class)
public abstract class BadgerOutlineFeatureMixin {
    @Shadow
    @Final
    private QuadInstance quadInstance;

    @Inject(method = "prepareSubmit", at = @At("HEAD"), cancellable = true)
    private void apeira$drawSelectionOutline(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        if (submit.outlineColor() != SelectionOutline.MARK) {
            return;
        }
        quadInstance.setColor(SelectionOutline.WHITE);
        for (BakedQuad quad : submit.quads()) {
            ((RenderTypeFeatureRendererInvoker) this).apeira$getVertexBuilder(SelectionOutline.type(quad.materialInfo().sprite().atlasLocation()))
                    .putBakedQuad(submit.pose(), quad, quadInstance);
        }
        ci.cancel();
    }
}
