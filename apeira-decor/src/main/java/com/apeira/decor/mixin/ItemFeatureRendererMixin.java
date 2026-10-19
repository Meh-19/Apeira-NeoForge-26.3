package com.apeira.decor.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.apeira.decor.client.FinishRenderTypes;
import com.mojang.blaze3d.vertex.QuadInstance;

import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Draws item quads tinted with a paint finish with the finish's render type ({@link FinishRenderTypes#forQuad}). Every
 * item goes through here: in the world, in hands, on players and in the GUI's item atlas. The quad's tint is already in
 * the quad instance when its vertex builder is asked for.
 */
@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {
    @Shadow
    @Final
    private QuadInstance quadInstance;

    @ModifyArg(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private RenderType apeira$drawFinish(RenderType renderType) {
        return FinishRenderTypes.forQuad(renderType, quadInstance);
    }
}
