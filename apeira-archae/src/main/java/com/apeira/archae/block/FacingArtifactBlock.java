package com.apeira.archae.block;

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
 * Argon's {@code FurnitureDisplayBlock}: an artifact that also has a horizontal {@link #FACING}. Placed against the
 * side of a block it faces away from it; placed on the floor it faces the player and turns like any artifact.
 *
 * <p>Its display mode follows its id, as Argon's did: a block named {@code *wall*} hangs on the wall it faces away
 * from, one named {@code *side*} lies on the floor, and any other is drawn under the top face. Most wall and ceiling
 * variants are facing artifacts.
 */
public class FacingArtifactBlock extends ArtifactBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /** @param drop the artifact this variant drops, or {@code null} for itself */
    public FacingArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, @Nullable Supplier<? extends Block> drop) {
        super(properties, paintable, Form.FURNITURE, null, null, drop);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (side.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, side.getOpposite());
        }
        return defaultBlockState().setValue(ROTATION, rotation(context)).setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public DisplayMode displayMode(BlockState state) {
        String name = getDescriptionId();
        if (name.contains("wall")) {
            return DisplayMode.WALL;
        }
        return name.contains("side") ? DisplayMode.FLOOR : DisplayMode.TOP_FACE;
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
