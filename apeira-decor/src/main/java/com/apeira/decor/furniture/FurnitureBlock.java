package com.apeira.decor.furniture;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.registry.DecorBlockEntities;
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
 * A furniture piece, Apeira's one furniture system (first written for Argon's archaeology artifacts, now used by every
 * module's furniture), on {@link ApeiraFurnitureBlock} (CosmoLib's furniture system, which Argon's
 * {@code AbstractFurnitureBlock} was): turned in eight 45 degree steps ({@link #ROTATION}) to face the player who
 * placed it. A tuning from apeira-dev's furniture wand ({@link FurnitureTunings}) overrides its shape while on.
 *
 * <p>Furniture pieces are drawn by {@code FurnitureRenderer}, which lays the block's item model into the block as its
 * {@link Form#mode() display mode} says (Argon's models are Origin Realms display models that reach far past a block
 * model's bounds); the block model itself only gives the breaking particles. Every piece keeps its paint in an
 * {@link FurnitureBlockEntity}, and paintable ones take paint from the paintbrush and painting table.
 *
 * <p>A furniture piece may hand its placement to a wall or ceiling variant, a separate block placed when the player clicks
 * the side or the bottom of a block, and a variant drops its base piece rather than itself.
 *
 * <p>A furniture piece is the controller of an apeira-decor multiblock piece: its {@link Footprint} (from
 * {@link FurnitureFootprints}, where its model's cubes are as it is drawn) may cover the blocks around it, which then
 * hold {@code apeira:multiblock_part} blocks with the piece's shape in them. It is placed only where all of them
 * are free, and breaking any part breaks the piece. The footprint turns with the piece: by its
 * {@link #ROTATION} in 45 degree steps, or for one drawn on a wall by its {@code facing}.
 */
public class FurnitureBlock extends ApeiraFurnitureBlock implements MultiblockController {
    private final boolean paintable;
    private final Form form;
    private final @Nullable Supplier<? extends Block> wall;
    private final @Nullable Supplier<? extends Block> ceiling;
    private final @Nullable Supplier<? extends Block> drop;
    private @Nullable Footprint footprint;
    private float renderOffsetX;
    private float renderOffsetY;
    private float renderOffsetZ;
    private float modelYaw;
    private @Nullable DisplayMode displayAs;

    /**
     * @param paintable whether the paintbrush and painting table paint it
     * @param wall      the variant placed against the side of a block, or {@code null}
     * @param ceiling   the variant placed under a block, or {@code null}
     * @param drop      the piece a variant drops, or {@code null} for itself
     */
    public FurnitureBlock(BlockBehaviour.Properties properties, boolean paintable, Form form, @Nullable Supplier<? extends Block> wall,
            @Nullable Supplier<? extends Block> ceiling, @Nullable Supplier<? extends Block> drop) {
        super(properties, form.shape());
        this.paintable = paintable;
        this.form = form;
        this.wall = wall;
        this.ceiling = ceiling;
        this.drop = drop;
    }

    public FurnitureBlock(BlockBehaviour.Properties properties, boolean paintable, Form form) {
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

    /**
     * Moves the drawn model by {@code x}, {@code y} and {@code z} pixels (world axes, before its turn), for a piece whose
     * display model sits off its block, as apeira-dev's furniture wand exports it. Returns this block.
     */
    @SuppressWarnings("unchecked")
    public <T extends FurnitureBlock> T renderOffset(float x, float y, float z) {
        renderOffsetX = x / 16.0F;
        renderOffsetY = y / 16.0F;
        renderOffsetZ = z / 16.0F;
        return (T) this;
    }

    /**
     * Turns the drawn model by {@code degrees} about the block's centre column, for a model authored facing the other
     * way from the rest (as Ren's windmill is). Returns this block.
     */
    @SuppressWarnings("unchecked")
    public <T extends FurnitureBlock> T modelYaw(float degrees) {
        modelYaw = degrees;
        return (T) this;
    }

    /** How far the drawn model is turned about the block's centre column, in degrees. */
    public float modelYaw() {
        return modelYaw;
    }

    /** How far the renderer moves the model, in blocks: x, y, z. */
    public float[] renderOffset() {
        return new float[] {renderOffsetX, renderOffsetY, renderOffsetZ};
    }

    /** How the renderer lays the model into the block, for a placed {@code state}. */
    public DisplayMode displayMode(BlockState state) {
        return displayAs != null ? displayAs : form.mode();
    }

    /**
     * Lays the model in by {@code mode} whatever the piece's form or id says, for a pack model whose {@code fixed}
     * display was made for another mode (the potted forest fern's ceiling model lies like a floor piece). Returns this block.
     */
    @SuppressWarnings("unchecked")
    public <T extends FurnitureBlock> T displayAs(DisplayMode mode) {
        displayAs = mode;
        return (T) this;
    }

    /**
     * The {@link #ROTATION} of a piece placed against a wall that {@code facing} points into: south is 0, as the
     * footprint table has wall pieces, so a wall piece drawn in another mode ({@link #displayAs}) turns by it as a
     * {@link DisplayMode#WALL} piece turns by its facing.
     */
    protected static int wallTurn(Direction facing) {
        return Math.floorMod(Math.round(facing.toYRot() / 45.0F), ROTATIONS);
    }

    /** The mode {@link #displayAs} set, or {@code null}. */
    protected @Nullable DisplayMode displayOverride() {
        return displayAs;
    }

    /** The block whose item this one drops: its base piece for a wall or ceiling variant, else itself. */
    public Block dropBlock() {
        return drop != null ? drop.get() : this;
    }

    @Override
    public boolean takesPaint() {
        return paintable;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FurnitureBlockEntity(pos, state);
    }

    /** The piece's footprint, looked up by its id the first time it is needed. */
    @Override
    public Footprint footprint(BlockState state) {
        Footprint known = footprint;
        if (known == null) {
            known = FurnitureFootprints.of(BuiltInRegistries.BLOCK.getKey(this).getPath(), form.shape());
            footprint = known;
        }
        return known;
    }

    /** By {@link #ROTATION}, or for a furniture piece drawn on a wall by its facing (south is turn 0, as the table has it). */
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

    /** The piece's shape in its own block: the part of its footprint there. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape tuned = FurnitureTunings.shape(this, state.getValue(ROTATION));
        return tuned != null ? tuned : controllerShape(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Multiblock.placeParts(level, pos, state, oldState);
    }

    /** The piece's parts go with it, however it is removed. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Multiblock.removeParts(level, pos, state);
    }

    /**
     * A furniture piece's display mode and Argon's outline, one per kind of Argon furniture block. The outline is no longer
     * the piece's shape (its footprint is), only the north shape {@link ApeiraFurnitureBlock} is built with.
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
