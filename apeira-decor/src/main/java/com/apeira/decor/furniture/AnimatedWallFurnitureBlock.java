package com.apeira.decor.furniture;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * An animated furniture piece that hangs only on a wall, such as the cat clock: placed against the side of a block, its
 * {@link #FACING} pointing into that wall as {@link FacingFurnitureBlock}'s wall pieces do, so its model's north side
 * (Blockbench's) faces the player who placed it. {@link FurnitureBlock#turn} turns its footprint by that facing (south
 * is turn 0, the wall on the block's south side) and {@code AnimatedFurnitureRenderer} turns the model with it.
 */
public class AnimatedWallFurnitureBlock extends AnimatedFurnitureBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public AnimatedWallFurnitureBlock(BlockBehaviour.Properties properties, String folder, boolean paintable, String animation) {
        this(properties, folder, paintable, animation, null);
    }

    /**
     * An animated wall variant of another piece (the circus flag's), dropping that piece.
     *
     * @param drop the piece it drops, or {@code null} for itself
     */
    public AnimatedWallFurnitureBlock(BlockBehaviour.Properties properties, String folder, boolean paintable, String animation,
            @Nullable Supplier<? extends Block> drop) {
        super(properties, folder, paintable, animation, null, null, drop);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** Only against the side of a block. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        return side.getAxis().isHorizontal() ? defaultBlockState().setValue(FACING, side.getOpposite()) : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction wall = state.getValue(FACING);
        return level.getBlockState(pos.relative(wall)).isFaceSturdy(level, pos.relative(wall), wall.getOpposite());
    }

    @Override
    public DisplayMode displayMode(BlockState state) {
        return DisplayMode.WALL;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
