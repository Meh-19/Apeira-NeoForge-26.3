package com.apeira.decor.furniture;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A still furniture piece that hangs only on a wall (the bathroom mirror, lamp and shower head): placed against the side
 * of a block, its {@link #FACING} pointing into that wall as {@link FacingFurnitureBlock}'s wall pieces' does. Clicked
 * under a block it places its ceiling variant, when it has one; it cannot stand on the floor.
 */
public class WallFurnitureBlock extends FurnitureBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private final boolean hasCeiling;

    /** @param ceiling the variant placed under a block, or {@code null} for none */
    public WallFurnitureBlock(BlockBehaviour.Properties properties, boolean paintable, @Nullable Supplier<? extends Block> ceiling) {
        super(properties, paintable, Form.WALL_MOUNTED, null, ceiling, null);
        this.hasCeiling = ceiling != null;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
    }

    public WallFurnitureBlock(BlockBehaviour.Properties properties, boolean paintable) {
        this(properties, paintable, null);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /** Against the side of a block; under one as the ceiling variant; never on the floor. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (side.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, side.getOpposite()).setValue(ROTATION, wallTurn(side.getOpposite()));
        }
        return side == Direction.DOWN && hasCeiling ? super.getStateForPlacement(context) : null;
    }

    @Override
    public DisplayMode displayMode(BlockState state) {
        return displayOverride() != null ? displayOverride() : DisplayMode.WALL;
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
