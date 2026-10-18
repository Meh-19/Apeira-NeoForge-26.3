package com.apeira.decor.block;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for furniture: a block that faces one of the four horizontal directions, with a shape that turns with it.
 *
 * <p>Shapes and models are authored facing north, with the front of the furniture (the side a chair's sitter looks
 * out of) on the north side. When placed, furniture faces the player who placed it. Give the north-facing shape to the
 * constructor; build it with {@code scripts/parse_blockbench_hitboxes.py} from the furniture's model. The block state
 * is generated with {@code ApeiraBlockStateProvider#furniture}, which turns the model the same way.
 *
 * <p>Minecraft 26.x no longer has block codecs, so subclasses only need a {@code (Properties)} constructor for
 * {@code DeferredRegister.Blocks#registerBlock}.
 */
public abstract class ApeiraFurnitureBlock extends HorizontalDirectionalBlock {
    private final Map<Direction, VoxelShape> shapes;

    protected ApeiraFurnitureBlock(BlockBehaviour.Properties properties, VoxelShape northShape) {
        super(properties);
        this.shapes = rotations(northShape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Returns {@code north} turned to face each horizontal direction, as the block state's model is turned. */
    public static Map<Direction, VoxelShape> rotations(VoxelShape north) {
        return Shapes.rotateHorizontal(north);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }
}
