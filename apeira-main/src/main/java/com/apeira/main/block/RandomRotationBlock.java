package com.apeira.main.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Xenon's leaf litter and shelf fungus: a flat cover in one of four turns ({@link #ROTATION}, a quarter turn each,
 * which the block state applies to the model). The turn comes from the block position ({@link #turnAt}), so the client's
 * predicted block and the server's agree and nothing jumps when it is placed. As in Xenon it needs nothing under it.
 */
public class RandomRotationBlock extends Block {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 14, 1, 14);

    public RandomRotationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION, turnAt(context.getClickedPos()));
    }

    /** A quarter-turn count, 0-3, that looks random but depends only on {@code pos}. */
    public static int turnAt(BlockPos pos) {
        return (int) (Mth.getSeed(pos) >>> 16 & 3);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
