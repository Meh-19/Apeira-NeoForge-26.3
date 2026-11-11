package com.apeira.archae.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.archae.registry.ArchaeBlockEntities;
import com.apeira.archae.registry.ArtifactFootprints;
import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.multiblock.Footprint;
import com.apeira.decor.multiblock.Multiblock;
import com.apeira.decor.multiblock.MultiblockController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An artifact: a piece of Argon's archaeology furniture, on apeira-decor's {@link ApeiraFurnitureBlock} (CosmoLib's
 * furniture system, which Argon's {@code AbstractFurnitureBlock} was): turned in eight 45 degree steps
 * ({@link #ROTATION}) to face the player who placed it.
 *
 * <p>Artifacts are drawn by {@code ArtifactRenderer}, which lays the block's item model into the block as its
 * {@link Form#mode() display mode} says (Argon's models are Origin Realms display models that reach far past a block
 * model's bounds); the block model itself only gives the breaking particles. Every artifact keeps its paint in an
 * {@link ArtifactBlockEntity}, and paintable ones take paint from the paintbrush and painting table.
 *
 * <p>An artifact may hand its placement to a wall or ceiling variant, a separate block placed when the player clicks
 * the side or the bottom of a block, and a variant drops its base artifact rather than itself.
 *
 * <p>An artifact is the controller of an apeira-decor multiblock piece: its {@link Footprint} (from
 * {@link ArtifactFootprints}, where its model's cubes are as it is drawn) may cover the blocks around it, which then
 * hold {@code apeira:multiblock_part} blocks with the artifact's shape in them. It is placed only where all of them
 * are free, and breaking any part breaks the artifact. The footprint turns with the artifact: by its
 * {@link #ROTATION} in 45 degree steps, or for one drawn on a wall by its {@code facing}.
 */
public class ArtifactBlock extends ApeiraFurnitureBlock implements MultiblockController {
    private final boolean paintable;
    private final Form form;
    private final @Nullable Supplier<? extends Block> wall;
    private final @Nullable Supplier<? extends Block> ceiling;
    private final @Nullable Supplier<? extends Block> drop;
    private @Nullable Footprint footprint;

    /**
     * @param paintable whether the paintbrush and painting table paint it
     * @param wall      the variant placed against the side of a block, or {@code null}
     * @param ceiling   the variant placed under a block, or {@code null}
     * @param drop      the artifact a variant drops, or {@code null} for itself
     */
    public ArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, Form form, @Nullable Supplier<? extends Block> wall,
            @Nullable Supplier<? extends Block> ceiling, @Nullable Supplier<? extends Block> drop) {
        super(properties, form.shape());
        this.paintable = paintable;
        this.form = form;
        this.wall = wall;
        this.ceiling = ceiling;
        this.drop = drop;
    }

    public ArtifactBlock(BlockBehaviour.Properties properties, boolean paintable, Form form) {
        this(properties, paintable, form, null, null, null);
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

    /** The artifact's footprint, looked up by its id the first time it is needed. */
    @Override
    public Footprint footprint(BlockState state) {
        Footprint known = footprint;
        if (known == null) {
            known = ArtifactFootprints.of(BuiltInRegistries.BLOCK.getKey(this).getPath());
            footprint = known;
        }
        return known;
    }

    /** By {@link #ROTATION}, or for an artifact drawn on a wall by its facing (south is turn 0, as the table has it). */
    @Override
    public int turn(BlockState state) {
        if (displayMode(state) == DisplayMode.WALL) {
            return state.getOptionalValue(HorizontalDirectionalBlock.FACING).map(facing -> Math.round(facing.toYRot() / 45.0F)).orElse(0);
        }
        return state.getValue(ROTATION);
    }

    @Override
    public boolean partsCollide(BlockState state) {
        return hasCollision;
    }

    /** The artifact's shape in its own block: the part of its footprint there. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return controllerShape(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Multiblock.placeParts(level, pos, state, oldState);
    }

    /** The artifact's parts go with it, however it is removed. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Multiblock.removeParts(level, pos, state);
    }

    /**
     * An artifact's display mode and Argon's outline, one per kind of Argon furniture block. The outline is no longer
     * the artifact's shape (its footprint is), only the north shape {@link ApeiraFurnitureBlock} is built with.
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
