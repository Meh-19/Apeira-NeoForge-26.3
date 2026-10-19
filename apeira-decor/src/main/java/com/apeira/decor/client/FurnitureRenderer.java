package com.apeira.decor.client;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.block.ApeiraFurnitureBlock;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a placed furniture piece, as CosmoLib's {@code FurnitureBlockEntityRenderer} did: the piece's item, painted like
 * the block, in the {@code none} display context, turned by the block's {@link ApeiraFurnitureBlock#ROTATION}. The
 * item definition shows the block model there, or for a large piece, one that reaches further than a block model may,
 * its {@code models/item/furniture/<name>_placed.json}, scaled and lifted as {@link Furniture.Large} says. Paint and
 * finishes show through the item's {@code apeira:paint} tint, as on any other item.
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
        if (!(blockEntity.getBlockState().getBlock() instanceof FurnitureBlock block)) {
            state.item.clear();
            return;
        }
        Furniture.Large large = block.furniture().large();
        state.large = large != null ? large : Furniture.Large.BLOCK_MODEL;
        state.yaw = ApeiraFurnitureBlock.yaw(blockEntity.getBlockState());
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
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // A north-facing piece is turned by (yaw + 180) degrees clockwise, as its shape is.
        poseStack.translate(0.5F, large.bottom() / 16.0F, 0.5F);
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yaw);
        poseStack.scale(large.scale(), large.scale(), large.scale());
        // Model space has the block's corner at the origin; items are drawn centred on it, so move back by half a block.
        poseStack.translate(0.0F, 0.5F - large.modelBottom() / 16.0F, 0.0F);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Large pieces reach outside their block, so pieces are kept while the block itself is off screen. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        Furniture.Large large = Furniture.Large.BLOCK_MODEL;
        float yaw;
    }
}
