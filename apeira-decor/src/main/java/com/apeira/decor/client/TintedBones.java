package com.apeira.decor.client;

import java.util.function.Predicate;

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
 * artifacts. {@link #submitSuffixed} tints only bones whose own name ends with a suffix, not their children: the realm's
 * starter balloon ({@code _tint}). Call either from a renderer's {@code submitRenderTasks}.
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
        return submit(renderPassInfo, renderTasks, renderType, tint, bone -> bone.name().contains(TINTABLE), true);
    }

    /**
     * As {@link #submit}, but only the bones whose name ends with {@code suffix} take the tint; their child bones and
     * every other bone keep the texture's own colours.
     */
    public static boolean submitSuffixed(RenderPassInfo<?> renderPassInfo, OrderedSubmitNodeCollector renderTasks,
            @Nullable RenderType renderType, int tint, String suffix) {
        return submit(renderPassInfo, renderTasks, renderType, tint, bone -> bone.name().endsWith(suffix), false);
    }

    private static boolean submit(RenderPassInfo<?> renderPassInfo, OrderedSubmitNodeCollector renderTasks, @Nullable RenderType renderType,
            int tint, Predicate<GeoBone> tinted, boolean inherit) {
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
                    renderBone(renderPassInfo, bone, buffer, light, overlay, color, color, tint, tinted, inherit);
                }
            });
            poseStack.popPose();
        });
        return true;
    }

    /** {@code color} is the parent's colour, {@code base} the model's own. */
    private static void renderBone(RenderPassInfo<?> renderPassInfo, GeoBone bone, VertexConsumer buffer, int light, int overlay, int color,
            int base, int tint, Predicate<GeoBone> tinted, boolean inherit) {
        int boneColor = tint != -1 && tinted.test(bone) ? tint : inherit ? color : base;
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        RenderUtil.prepMatrixForBoneAndUpdateListeners(poseStack, bone, renderPassInfo);
        bone.render(renderPassInfo, poseStack, buffer, light, overlay, boneColor);
        if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
            for (GeoBone child : bone.children()) {
                renderBone(renderPassInfo, child, buffer, light, overlay, boneColor, base, tint, tinted, inherit);
            }
        }
        poseStack.popPose();
    }
}
