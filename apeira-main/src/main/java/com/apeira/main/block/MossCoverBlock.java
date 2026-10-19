package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Xenon's damp moss and grimoss: a one-pixel cover on a full face, and the one item of its moss. Placed against the
 * underside of a block or of a hanging vine of the same moss, it becomes that vine's tip; placed on a top face, the
 * cover. Clicking another face places whichever of the two can stay there, the cover first.
 */
@SuppressWarnings("null")
public class MossCoverBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    private final Supplier<? extends Block> hanging;
    private final Supplier<? extends Block> hangingBody;

    /** {@code hanging} is the vine's tip and {@code hangingBody} the rest of it. */
    public MossCoverBlock(Properties properties, Supplier<? extends Block> hanging, Supplier<? extends Block> hangingBody) {
        super(properties);
        this.hanging = hanging;
        this.hangingBody = hangingBody;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState hangingState = hanging.get().defaultBlockState();
        boolean hangs = hangingState.canSurvive(context.getLevel(), context.getClickedPos());
        if (context.getClickedFace() == Direction.DOWN && hangs) {
            return hangingState;
        }
        if (defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())) {
            return defaultBlockState();
        }
        return hangs ? hangingState : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && !above.is(hanging.get())
                && !above.is(hangingBody.get());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
