package com.apeira.decor.client.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.client.EmissiveRenderTypes;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.decor.crate.CrateBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Draws a crate's GeckoLib model ({@link CrateModel}) turned by the block's {@link CrateBlock#ROTATION}, and the rolled
 * items with {@link CrateItemLayer}. The model's front faces north, as CosmoLib's crate entity's did.
 */
public class CrateRenderer extends GeoBlockRenderer<CrateBlockEntity, BlockEntityRenderState> {
    /** The crate's yaw in degrees ({@link CrateBlock#yaw}). */
    static final DataTicket<Float> YAW = DataTicket.create("apeira_crate_yaw", Float.class);

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new CrateModel());
        withRenderLayer(new CrateItemLayer(this, context.itemModelResolver(), context.font()));
    }

    @Override
    public void addRenderData(CrateBlockEntity crate, @Nullable Void relatedObject, BlockEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(YAW, CrateBlock.yaw(crate.getBlockState()));
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<BlockEntityRenderState> renderPassInfo) {
        // CosmoLib turned its crate entity by (180 - yaw); the block keeps the same facing.
        renderPassInfo.poseStack().rotateDegrees(Axis.YP, 180.0F - yaw(renderPassInfo.renderState()));
    }

    static float yaw(BlockEntityRenderState renderState) {
        Float yaw = renderState.getGeckolibData(YAW);
        return yaw == null ? 0.0F : yaw;
    }

    /** The model reaches past its block (lids swing open, items rise about two blocks), so it is culled by a larger box. */
    @Override
    public AABB getRenderBoundingBox(CrateBlockEntity crate) {
        return new AABB(crate.getBlockPos()).inflate(1.0).expandTowards(0.0, 2.0, 0.0);
    }

    /** Translucent, with the alpha 254 texels glowing ({@link EmissiveRenderTypes}). */
    @Override
    public RenderType getRenderType(BlockEntityRenderState renderState, Identifier texture) {
        return EmissiveRenderTypes.entityTranslucent(texture);
    }
}
