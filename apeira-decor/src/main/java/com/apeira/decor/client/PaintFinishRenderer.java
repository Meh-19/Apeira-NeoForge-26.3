package com.apeira.decor.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.paint.Paint;
import com.apeira.decor.paint.PaintBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a placed paintable block painted with a finish, which {@link FinishBlockModel} leaves out of the chunk mesh: the
 * block model's quads are submitted as item quads, whose {@code tintindex} 0 faces the item renderer draws with the
 * finish (see {@link FinishRenderTypes}). Blocks without a finish are drawn by the chunk mesh, and this draws nothing.
 */
public final class PaintFinishRenderer implements BlockEntityRenderer<PaintBlockEntity, PaintFinishRenderer.State> {
    private static final Direction[] FACES_AND_INSIDE = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, null};

    public PaintFinishRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PaintBlockEntity blockEntity, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.quads = ItemQuads.EMPTY;
        if (!(blockEntity.getLevel() instanceof ClientLevel level) || !FinishBlockModel.hasFinish(level, blockEntity.getBlockPos())) {
            return;
        }
        BlockState blockState = blockEntity.getBlockState();
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockState);
        if (!(model instanceof FinishBlockModel finishModel)) {
            return;
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        finishModel.delegate().collectParts(level, blockEntity.getBlockPos(), blockState,
                RandomSource.create(blockState.getSeed(blockEntity.getBlockPos())), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            for (Direction face : FACES_AND_INSIDE) {
                quads.addAll(part.getQuads(face));
            }
        }
        state.quads = ItemQuads.split(quads);
        state.tints[0] = Paint.itemTint(blockEntity.paint());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!state.quads.isEmpty()) {
            submitNodeCollector.submitItem(poseStack, ItemDisplayContext.NONE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0,
                    state.tints, state.quads, ItemStackRenderState.FoilType.NONE);
        }
    }

    public static final class State extends BlockEntityRenderState {
        ItemQuads quads = ItemQuads.EMPTY;
        /** The paint tint, for the model's {@code tintindex} 0 faces. */
        final int[] tints = {-1};
    }
}
