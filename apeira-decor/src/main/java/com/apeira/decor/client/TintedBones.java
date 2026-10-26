package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Draws a GeckoLib model with a tint on every bone whose name contains {@value #TINTABLE} and on its children, the
 * GeckoLib side of the paint table's {@code tintindex}: Argon's {@code TintableGeoFurnitureRenderer}, shared by animated
 * artifacts and the realm's starter balloon. Call it from a renderer's {@code submitRenderTasks}.
 */
public final class TintedBones {
    /** The part of a bone's name that marks it as painted. */
    public static final String TINTABLE = "_tintable";

    private TintedBones() {}

    /**
     * Submits the model's bones as GeckoLib's own {@code submitRenderTasks} does, with {@code tint} (ARGB, or -1 for
     * none) on the tintable bones. Returns false, having drawn nothing, when there is no render type or model, so the
     * caller can fall back on GeckoLib's own.
     */
    public static boolean submit(RenderPassInfo<?> renderPassInfo, OrderedSubmitNodeCollector renderTasks, @Nullable RenderType renderType,
            int tint) {
        if (renderType == null || renderPassInfo.model().isMissingno()) {
            return false;
        }
        int light = renderPassInfo.packedLight();
        int overlay = renderPassInfo.packedOverlay();
        int color = renderPassInfo.renderColor();
        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, buffer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> {
                for (GeoBone bone : renderPassInfo.model().topLevelBones()) {
                    renderBone(renderPassInfo, bone, buffer, light, overlay, color, tint);
                }
            });
            poseStack.popPose();
        });
        return true;
    }

    private static void renderBone(RenderPassInfo<?> renderPassInfo, GeoBone bone, VertexConsumer buffer, int light, int overlay, int color,
            int tint) {
        int boneColor = tint != -1 && bone.name().contains(TINTABLE) ? tint : color;
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        RenderUtil.prepMatrixForBoneAndUpdateListeners(poseStack, bone, renderPassInfo);
        bone.render(renderPassInfo, poseStack, buffer, light, overlay, boneColor);
        if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
            for (GeoBone child : bone.children()) {
                renderBone(renderPassInfo, child, buffer, light, overlay, boneColor, tint);
            }
        }
        poseStack.popPose();
    }
}
