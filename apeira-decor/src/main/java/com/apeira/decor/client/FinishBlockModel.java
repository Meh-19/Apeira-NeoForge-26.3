package com.apeira.decor.client;

import java.util.List;

import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

/**
 * The block model of a paintable block, which leaves the block out of the chunk mesh while it is painted with a finish.
 * Chunk meshes have no finish render type, so {@link PaintFinishRenderer} draws such a block instead, with this model's
 * {@link #delegate()} parts.
 */
public final class FinishBlockModel extends DelegateBlockStateModel {
    public FinishBlockModel(BlockStateModel delegate) {
        super(delegate);
    }

    /** The model the block is drawn with. */
    public BlockStateModel delegate() {
        return delegate;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
            List<BlockStateModelPart> parts) {
        if (!hasFinish(level, pos)) {
            super.collectParts(level, pos, state, random, parts);
        }
    }

    /** Whether the block at {@code pos} is painted with a finish. */
    static boolean hasFinish(BlockAndTintGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof PaintBlockEntity block && block.paint() != null
                && FinishType.fromPaint(block.paint()) != null;
    }
}
