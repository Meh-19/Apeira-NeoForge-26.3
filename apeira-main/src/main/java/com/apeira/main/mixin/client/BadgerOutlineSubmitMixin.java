package com.apeira.main.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.apeira.main.client.crate.SelectionOutline;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * An item submitted with {@link SelectionOutline#MARK} is only the Badger Crate selection outline: one submit with all
 * its quads in the solid phase, drawn by {@link BadgerOutlineFeatureMixin}, instead of the item and a glowing outline.
 */
@Mixin(SubmitNodeCollection.class)
public abstract class BadgerOutlineSubmitMixin {
    @Inject(method = "submitItem", at = @At("HEAD"), cancellable = true)
    private void apeira$selectionOutline(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords,
            int outlineColor, int[] tintLayers, ItemQuads quads, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        if (outlineColor == SelectionOutline.MARK) {
            ((SubmitNodeCollection) (Object) this).solid.submit(new ItemFeatureRenderer.Submit(poseStack.last().copy(), displayContext,
                    lightCoords, OverlayTexture.NO_OVERLAY, outlineColor, tintLayers, quads.all(), ItemStackRenderState.FoilType.NONE));
            ci.cancel();
        }
    }
}
