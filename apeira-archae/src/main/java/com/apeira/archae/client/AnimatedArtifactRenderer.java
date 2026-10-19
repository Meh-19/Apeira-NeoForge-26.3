package com.apeira.archae.client;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.block.AnimatedArtifactBlockEntity;
import com.apeira.archae.block.ArtifactBlock;
import com.apeira.decor.paint.Paint;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Draws an animated artifact's GeckoLib model ({@link ArtifactGeoModel}) as Argon's {@code TintableGeoFurnitureRenderer}
 * did: turned by the block's {@link ArtifactBlock#ROTATION}, translucent, and with the paint on every bone whose name
 * contains {@code _tintable} and on its child bones. A finish shows as its representative colour.
 */
public class AnimatedArtifactRenderer extends GeoBlockRenderer<AnimatedArtifactBlockEntity, BlockEntityRenderState> {
    private static final DataTicket<Integer> ROTATION = DataTicket.create("apeira_artifact_rotation", Integer.class);
    /** The paint's tint, or -1 for none. */
    private static final DataTicket<Integer> PAINT = DataTicket.create("apeira_artifact_paint", Integer.class);
    private static final String TINTABLE = "_tintable";

    public AnimatedArtifactRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new ArtifactGeoModel());
    }

    @Override
    public void addRenderData(AnimatedArtifactBlockEntity artifact, @Nullable Void relatedObject, BlockEntityRenderState renderState,
            float partialTick) {
        renderState.addGeckolibData(ROTATION, artifact.getBlockState().getValueOrElse(ArtifactBlock.ROTATION, 0));
        Integer paint = artifact.paint();
        renderState.addGeckolibData(PAINT, paint != null ? Paint.tint(paint) : -1);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> renderPassInfo) {
        int rotation = renderPassInfo.getOrDefaultGeckolibData(ROTATION, 0);
        renderPassInfo.poseStack().rotateDegrees(Axis.YP, rotation * -45.0F);
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucentCull(texture);
    }

    /** As GeckoLib's own, but walks the bones here so the {@code _tintable} ones get the paint. */
    @Override
    public void submitRenderTasks(RenderPassInfo<BlockEntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks,
            @Nullable RenderType renderType) {
        if (renderType == null || renderPassInfo.model().isMissingno()) {
            super.submitRenderTasks(renderPassInfo, renderTasks, renderType);
            return;
        }
        int light = renderPassInfo.packedLight();
        int overlay = renderPassInfo.packedOverlay();
        int color = renderPassInfo.renderColor();
        int paint = renderPassInfo.getOrDefaultGeckolibData(PAINT, -1);
        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, buffer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> {
                for (GeoBone bone : renderPassInfo.model().topLevelBones()) {
                    renderBone(renderPassInfo, bone, buffer, light, overlay, color, paint);
                }
            });
            poseStack.popPose();
        });
    }

    private static void renderBone(RenderPassInfo<?> renderPassInfo, GeoBone bone, VertexConsumer buffer, int light, int overlay, int color,
            int paint) {
        int boneColor = paint != -1 && bone.name().contains(TINTABLE) ? paint : color;
        PoseStack poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        RenderUtil.prepMatrixForBoneAndUpdateListeners(poseStack, bone, renderPassInfo);
        bone.render(renderPassInfo, poseStack, buffer, light, overlay, boneColor);
        if (bone.frameSnapshot == null || !bone.frameSnapshot.areChildrenHidden()) {
            for (GeoBone child : bone.children()) {
                renderBone(renderPassInfo, child, buffer, light, overlay, boneColor, paint);
            }
        }
        poseStack.popPose();
    }
}
