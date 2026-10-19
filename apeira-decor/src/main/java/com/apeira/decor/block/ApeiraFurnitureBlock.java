package com.apeira.decor.block;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.paint.PaintableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Base for furniture, on CosmoLib's furniture system ({@code AbstractFurnitureBlock}), shared by the decor furniture
 * and apeira-archae's artifacts: a block turned in eight 45 degree steps ({@link #ROTATION}) to face the player who
 * placed it, drawn by a block entity renderer that lays the block's item model into it (the block itself renders
 * {@link RenderShape#INVISIBLE}; its block model only gives the breaking particles), with its paint kept in a
 * {@link com.apeira.decor.paint.PaintBlockEntity}.
 *
 * <p>Shapes and models are authored facing north, with the front of the furniture (the side a chair's sitter looks
 * out of) on the north side. Give the north-facing shape to the constructor; it is turned with the block, and at the
 * diagonal rotations it is the box around each turned box. Build it with {@code scripts/parse_blockbench_hitboxes.py}
 * from the furniture's model.
 *
 * <p>Minecraft 26.x no longer has block codecs, so subclasses only need a {@code (Properties)} constructor for
 * {@code DeferredRegister.Blocks#registerBlock}.
 */
public abstract class ApeiraFurnitureBlock extends Block implements PaintableBlock {
    /** The piece's turn, in 45 degree steps: the front faces {@link #yaw(int)}. */
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    public static final int ROTATIONS = 8;
    /** Rotation 0 faces south ({@code Direction#toYRot} 0), so a piece faces the player placing it. */
    private static final int FACE_OFFSET = 4;

    private final VoxelShape[] shapes;

    protected ApeiraFurnitureBlock(BlockBehaviour.Properties properties, VoxelShape northShape) {
        super(properties);
        this.shapes = rotations(northShape);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    /** The way the front of a piece at {@code rotation} faces, in degrees, as {@code Direction#toYRot} measures it. */
    public static float yaw(int rotation) {
        return rotation * 45.0F;
    }

    /** The way the front of a placed piece faces, in degrees. */
    public static float yaw(BlockState state) {
        return yaw(state.getValueOrElse(ROTATION, 0));
    }

    /** The rotation that turns a piece to face the player placing it. */
    public static int rotation(BlockPlaceContext context) {
        return Math.floorMod(Math.round(context.getRotation() / 45.0F) + FACE_OFFSET, ROTATIONS);
    }

    /** Returns {@code north} turned for each {@link #ROTATION}, as the renderer turns the model. */
    public static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] shapes = new VoxelShape[ROTATIONS];
        for (int rotation = 0; rotation < ROTATIONS; rotation++) {
            // A north-facing model is turned clockwise (seen from above) by its yaw plus half a turn.
            double angle = Math.toRadians(yaw(rotation) + 180.0);
            double cos = round(Math.cos(angle));
            double sin = round(Math.sin(angle));
            VoxelShape shape = Shapes.empty();
            for (AABB box : north.toAabbs()) {
                shape = Shapes.or(shape, turn(box, cos, sin));
            }
            shapes[rotation] = shape.optimize();
        }
        return shapes;
    }

    /** The box around {@code box} turned about the block's centre column, clamped to the block. */
    private static VoxelShape turn(AABB box, double cos, double sin) {
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double x : new double[] {box.minX, box.maxX}) {
            for (double z : new double[] {box.minZ, box.maxZ}) {
                double dx = x - 0.5;
                double dz = z - 0.5;
                double turnedX = round(0.5 + dx * cos - dz * sin);
                double turnedZ = round(0.5 + dx * sin + dz * cos);
                minX = Math.min(minX, turnedX);
                maxX = Math.max(maxX, turnedX);
                minZ = Math.min(minZ, turnedZ);
                maxZ = Math.max(maxZ, turnedZ);
            }
        }
        minX = Math.max(minX, 0.0);
        minZ = Math.max(minZ, 0.0);
        maxX = Math.min(maxX, 1.0);
        maxZ = Math.min(maxZ, 1.0);
        return minX < maxX && minZ < maxZ ? Shapes.box(minX, box.minY, minZ, maxX, box.maxY, maxZ) : Shapes.empty();
    }

    /** Drops the floating point noise of turning by right angles, so turned shapes line up with the block. */
    private static double round(double value) {
        return Math.round(value * 1.0E9) / 1.0E9;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION, rotation(context));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(ROTATION)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATIONS));
    }
}
