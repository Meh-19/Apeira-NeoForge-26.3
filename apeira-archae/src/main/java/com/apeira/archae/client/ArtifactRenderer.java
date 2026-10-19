package com.apeira.archae.client;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.block.ArtifactBlock;
import com.apeira.archae.block.ArtifactBlockEntity;
import com.apeira.archae.block.DisplayMode;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws an artifact as Argon's {@code ArtifactDisplayBlockEntityRenderer} did: the block's item, painted with the
 * block's paint, in the {@code fixed} display context at half size, laid into the block as its {@link DisplayMode}
 * says. Argon's models are Origin Realms item-frame models, which is why they are drawn lying down and turned about the
 * model's Z axis. Finishes show through apeira-decor's item rendering.
 */
public final class ArtifactRenderer implements BlockEntityRenderer<ArtifactBlockEntity, ArtifactRenderer.State> {
    private static final float SCALE = 0.5F;

    private final ItemModelResolver itemModelResolver;

    public ArtifactRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ArtifactBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockState block = blockEntity.getBlockState();
        state.mode = block.getBlock() instanceof ArtifactBlock artifact ? artifact.displayMode(block) : DisplayMode.TOP_FACE;
        state.rotation = block.getValueOrElse(ArtifactBlock.ROTATION, 0);
        state.facing = block.getOptionalValue(HorizontalDirectionalBlock.FACING).orElse(null);
        ItemStack stack = new ItemStack(block.getBlock());
        if (blockEntity.paint() != null) {
            Paint.set(stack, blockEntity.paint());
        }
        itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.FIXED, blockEntity.getLevel(), null,
                (int) blockEntity.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        switch (state.mode) {
            case WALL -> {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                if (state.facing != null) {
                    poseStack.rotateDegrees(Axis.YP, -state.facing.toYRot());
                    poseStack.translate(0.0F, 0.0F, 0.45F);
                }
            }
            case FLOOR, BLOCK_UP -> {
                poseStack.translate(0.5F, state.mode == DisplayMode.FLOOR ? 0.0F : 1.0F, 0.5F);
                poseStack.rotateDegrees(Axis.XP, 90.0F);
                poseStack.rotateDegrees(Axis.ZP, 180.0F);
                poseStack.rotateDegrees(Axis.ZP, state.rotation * 45.0F);
            }
            case TOP_FACE -> {
                poseStack.translate(0.5F, 0.999F, 0.5F);
                poseStack.rotateDegrees(Axis.XP, 180.0F);
                poseStack.rotateDegrees(Axis.XP, 90.0F);
                poseStack.rotateDegrees(Axis.ZP, 180.0F);
                poseStack.rotateDegrees(Axis.ZP, -state.rotation * 45.0F);
            }
        }
        poseStack.scale(SCALE, SCALE, SCALE);
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** Artifact models reach past their block, so they are kept while the block itself is off screen. */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item = new ItemStackRenderState();
        DisplayMode mode = DisplayMode.TOP_FACE;
        int rotation;
        @Nullable Direction facing;
    }
}
