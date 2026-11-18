package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.AnimatedFurnitureBlockEntity;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.client.TintedBones;
import com.apeira.decor.paint.FinishType;
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
import net.minecraft.world.phys.AABB;

/**
 * Draws an animated piece's GeckoLib model ({@link FurnitureGeoModel}) as Argon's {@code TintableGeoFurnitureRenderer}
 * did: turned by the block's {@link FurnitureBlock#ROTATION}, translucent (texels of alpha 254 glow,
 * {@link EmissiveRenderTypes}), and with the paint on every bone whose name contains {@code _tintable} and on its child
 * bones ({@link TintedBones}). A finish shows as its representative colour.
 */
public class AnimatedFurnitureRenderer extends GeoBlockRenderer<AnimatedFurnitureBlockEntity, BlockEntityRenderState> {
    private static final DataTicket<Integer> ROTATION = DataTicket.create("apeira_artifact_rotation", Integer.class);
    /** The paint's tint, or -1 for none. */
    private static final DataTicket<Integer> PAINT = DataTicket.create("apeira_artifact_paint", Integer.class);
    /** Whether the paint is a finish, drawn with {@link FinishRenderTypes}. */
    private static final DataTicket<Boolean> FINISH = DataTicket.create("apeira_furniture_finish", Boolean.class);
    private static final DataTicket<Float> YAW = DataTicket.create("apeira_furniture_yaw", Float.class);

    public AnimatedFurnitureRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new FurnitureGeoModel());
    }

    @Override
    public void addRenderData(AnimatedFurnitureBlockEntity artifact, @Nullable Void relatedObject, BlockEntityRenderState renderState,
            float partialTick) {
        renderState.addGeckolibData(ROTATION, artifact.getBlockState().getValueOrElse(FurnitureBlock.ROTATION, 0));
        Integer paint = artifact.paint();
        // A finish is drawn by the finish shader, which reads the finish from the vertex colour: its paint value, opaque.
        FinishType finish = paint != null ? FinishType.fromPaint(paint) : null;
        renderState.addGeckolibData(PAINT, paint == null ? -1 : finish != null ? 0xFF000000 | finish.paintValue() : Paint.tint(paint));
        renderState.addGeckolibData(FINISH, finish != null);
        renderState.addGeckolibData(YAW, artifact.getBlockState().getBlock() instanceof FurnitureBlock block ? block.modelYaw() : 0.0F);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> renderPassInfo) {
        int rotation = renderPassInfo.getOrDefaultGeckolibData(ROTATION, 0);
        renderPassInfo.poseStack().rotateDegrees(Axis.YP, rotation * -45.0F + renderPassInfo.getOrDefaultGeckolibData(YAW, 0.0F));
    }

    /** Culled by the box around the whole piece, as {@link FurnitureRenderer} is. */
    @Override
    public AABB getRenderBoundingBox(AnimatedFurnitureBlockEntity artifact) {
        return FurnitureRenderer.renderBounds(artifact);
    }

    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        // A finish needs the finish shader; the item pipelines share the entity vertex format, so GeckoLib models use them.
        return renderState.getOrDefaultGeckolibData(FINISH, false) ? FinishRenderTypes.translucent(texture)
                : EmissiveRenderTypes.entityTranslucentCull(texture);
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
