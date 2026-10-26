package com.apeira.archae.client;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.block.AnimatedArtifactBlockEntity;
import com.apeira.archae.block.ArtifactBlock;
import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.TintedBones;
import com.apeira.decor.paint.Paint;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Draws an animated artifact's GeckoLib model ({@link ArtifactGeoModel}) as Argon's {@code TintableGeoFurnitureRenderer}
 * did: turned by the block's {@link ArtifactBlock#ROTATION}, translucent (texels of alpha 254 glow,
 * {@link EmissiveRenderTypes}), and with the paint on every bone whose name contains {@code _tintable} and on its child
 * bones ({@link TintedBones}). A finish shows as its representative colour.
 */
public class AnimatedArtifactRenderer extends GeoBlockRenderer<AnimatedArtifactBlockEntity, BlockEntityRenderState> {
    private static final DataTicket<Integer> ROTATION = DataTicket.create("apeira_artifact_rotation", Integer.class);
    /** The paint's tint, or -1 for none. */
    private static final DataTicket<Integer> PAINT = DataTicket.create("apeira_artifact_paint", Integer.class);

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
        return EmissiveRenderTypes.entityTranslucentCull(texture);
    }

    /** As GeckoLib's own, with the paint on the {@code _tintable} bones. */
    @Override
    public void submitRenderTasks(RenderPassInfo<BlockEntityRenderState> renderPassInfo, OrderedSubmitNodeCollector renderTasks,
            @Nullable RenderType renderType) {
        if (!TintedBones.submit(renderPassInfo, renderTasks, renderType, renderPassInfo.getOrDefaultGeckolibData(PAINT, -1))) {
            super.submitRenderTasks(renderPassInfo, renderTasks, renderType);
        }
    }
}
