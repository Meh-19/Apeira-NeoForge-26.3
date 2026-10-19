package com.apeira.archae.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.registry.ArchaeBlockEntities;
import com.apeira.decor.paint.PaintableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An artifact: a piece of Argon's archaeology furniture, turned in eight 45 degree steps ({@link #ROTATION}) to face
 * the player who placed it, as Argon's {@code AbstractFurnitureBlock} was.
 *
 * <p>Artifacts are drawn by {@code ArtifactRenderer}, which lays the block's item model into the block as its
 * {@link Form#mode() display mode} says (Argon's models are Origin Realms display models that reach far past a block
 * model's bounds); the block model itself only gives the breaking particles. Every artifact keeps its paint in an
 * {@link ArtifactBlockEntity}, and paintable ones take paint from the paintbrush and painting table.
 *
 * <p>An artifact may hand its placement to a wall or ceiling variant, a separate block placed when the player clicks
 * the side or the bottom of a block, and a variant drops its base artifact rather than itself.
 */
public class ArtifactBlock extends Block implements PaintableBlock {
    /** The artifact's turn, in 45 degree steps. */
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    /** Argon's models face the other way from the player at rotation 0, so placement adds half a turn. */
    private static final int FACE_OFFSET = 4;

    private final boolean paintable;
    private final Form form;
    private final @Nullable Supplier<? extends Block> wall;
    private final @Nullable Supplier<? extends Block> ceiling;
    private final @Nullable Supplier<? extends Block> drop;

    /**
     * @param paintable whether the paintbrush and painting table paint it
     * @param wall      the variant placed against the side of a block, or {@code null}
     * @param ceiling   the variant placed under a block, or {@code null}
     * @param drop      the artifact a variant drops, or {@code null} for itself
     */
    public ArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, Form form, @Nullable Supplier<? extends Block> wall,
            @Nullable Supplier<? extends Block> ceiling, @Nullable Supplier<? extends Block> drop) {
        super(properties);
        this.paintable = paintable;
        this.form = form;
        this.wall = wall;
        this.ceiling = ceiling;
        this.drop = drop;
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    public ArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, Form form) {
        this(properties, paintable, form, null, null, null);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    /** The rotation that turns an artifact to face the player placing it. */
    protected static int rotation(BlockPlaceContext context) {
        return Math.floorMod(Math.round(context.getRotation() / 45.0F) + FACE_OFFSET, 8);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        if (side == Direction.DOWN && ceiling != null) {
            return ceiling.get().defaultBlockState().setValue(ROTATION, rotation(context));
        }
        if (side.getAxis().isHorizontal() && wall != null) {
            return wall.get().getStateForPlacement(context);
        }
        return defaultBlockState().setValue(ROTATION, rotation(context));
    }

    /** How the renderer lays the model into the block, for a placed {@code state}. */
    public DisplayMode displayMode(BlockState state) {
        return form.mode();
    }

    /** The block whose item this one drops: its base artifact for a wall or ceiling variant, else itself. */
    public Block dropBlock() {
        return drop != null ? drop.get() : this;
    }

    @Override
    public boolean takesPaint() {
        return paintable;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArtifactBlockEntity(ArchaeBlockEntities.ARTIFACT_DISPLAY.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return form.shape();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 8));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 8));
    }

    /**
     * An artifact's display mode and outline, one per kind of Argon furniture block. Tall artifacts have outlines
     * reaching into the blocks above, as Argon's did.
     */
    public record Form(DisplayMode mode, VoxelShape shape) {
        /** Argon's furniture, wall-and-floor and floor-and-ceiling blocks. */
        public static final Form FURNITURE = new Form(DisplayMode.TOP_FACE, Shapes.block());
        /** Floor decorations: a rug-thin outline. */
        public static final Form DECORATION = new Form(DisplayMode.FLOOR, Block.box(0, 0, 0, 16, 1, 16));
        public static final Form FULL_DECORATION = new Form(DisplayMode.FLOOR, Shapes.block());
        public static final Form FLOOR = new Form(DisplayMode.FLOOR, Shapes.block());
        /** Hung up, like a tarp: drawn on the top face. */
        public static final Form BLOCK_UP = new Form(DisplayMode.BLOCK_UP, Shapes.block());
        /** Argon's wall-mounted banners, which have no facing of their own and are drawn at the block centre. */
        public static final Form WALL_MOUNTED = new Form(DisplayMode.WALL, Shapes.block());
        public static final Form TWO_TALL = new Form(DisplayMode.TOP_FACE, tall(2));
        public static final Form THREE_TALL = new Form(DisplayMode.TOP_FACE, tall(3));
        public static final Form FOUR_TALL = new Form(DisplayMode.TOP_FACE, tall(4));
        public static final Form FOUR_TALL_FLOOR = new Form(DisplayMode.FLOOR, tall(4));
        public static final Form FIVE_TALL = new Form(DisplayMode.TOP_FACE, tall(5));

        private static VoxelShape tall(int blocks) {
            return Block.box(0, 0, 0, 16, blocks * 16, 16);
        }
    }
}
