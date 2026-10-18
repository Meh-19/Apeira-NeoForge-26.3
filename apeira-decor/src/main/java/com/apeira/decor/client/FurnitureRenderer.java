package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.furniture.Furniture;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlockEntity;
import com.apeira.decor.paint.Paint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a large furniture piece, one that reaches further than a block model may, at its full size. The piece's item
 * definition shows its {@code models/item/furniture/<name>_placed.json} in the {@code none} display context, so the
 * renderer draws the piece's item, painted like the block, scaled and turned as {@link Furniture.Large} says.
 */
public final class FurnitureRenderer implements BlockEntityRenderer<FurnitureBlockEntity, FurnitureRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public FurnitureRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FurnitureBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.large = blockEntity.getBlockState().getBlock() instanceof FurnitureBlock block ? block.furniture().large() : null;
        state.facing = blockEntity.getBlockState().getValueOrElse(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        if (state.large == null) {
            state.item.clear();
            return;
        }
        ItemStack stack = new ItemStack(blockEntity.getBlockState().getBlock());
        if (blockEntity.paint() != null) {
            Paint.set(stack, blockEntity.paint());
        }
        itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.NONE, blockEntity.getLevel(), null,
                (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Furniture.Large large = state.large;
        if (large == null || state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // The block model of a north-facing piece is turned by (facing + 180) degrees clockwise; turn the same way.
        poseStack.translate(0.5F, large.bottom() / 16.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.facing.toYRot());
        poseStack.scale(large.scale(), large.scale(), large.scale());
        // Model space has the block's corner at the origin; items are drawn centred on it, so move back by half a block.
        poseStack.translate(0.0F, 0.5F - large.modelBottom() / 16.0F, 0.0F);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Large pieces reach outside their block, so they are kept while the block itself is off screen. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Furniture.@Nullable Large large;
        Direction facing = Direction.NORTH;
    }
}
