package com.apeira.main.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Xenon's leaf litter and shelf fungus: a flat cover placed in one of four random turns ({@link #ROTATION}, a quarter
 * turn each, which the block state applies to the model). As in Xenon it needs nothing under it.
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
        return defaultBlockState().setValue(ROTATION, context.getLevel().getRandom().nextInt(4));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
