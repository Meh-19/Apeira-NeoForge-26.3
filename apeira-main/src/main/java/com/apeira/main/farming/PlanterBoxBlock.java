package com.apeira.main.farming;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Origin Realms' planter box: a wooden box of soil that grows one {@link FarmCrop} on its top, kept by a
 * {@link PlanterBoxBlockEntity}.
 *
 * <p>Its look is a multipart block state: the box with its top for {@link #WET} and {@link #SOILED} (Origin Realms'
 * four {@code plant_box} textures), and the wooden rim along each side and its corner posts, each left out where the
 * box joins another planter box ({@link #NORTH}, {@link #EAST}, {@link #SOUTH}, {@link #WEST}), so a row of boxes is
 * one long bed. The crop is drawn above it by {@code client/farming/PlanterBoxRenderer}.
 *
 * <p>Right-click with: a water bucket or a watering can to water it for the day (the bucket fill sound); a seed to plant
 * it, soil or a fertilizer to give it to the growing crop, one item a click, else into the stock, where the next
 * crops take theirs from (whole stacks go into the stock through the menu) ({@link #feedback}: planting, or composter sounds and particles). Anything else, or an empty hand, harvests a ripe crop (a hoe for a better
 * rating, {@link CropQuality}) or opens the box's menu. Breaking the box harvests a ripe crop with the breaking tool.
 */
public class PlanterBoxBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** Watered today: the darker wet soil. */
    public static final BooleanProperty WET = BooleanProperty.create("wet");
    /** The crop is soiled, or soil is stocked for the next one: the soil-covered top. */
    public static final BooleanProperty SOILED = BooleanProperty.create("soiled");
    private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    private static final Component TITLE = Component.translatable("container.apeira.planter_box");
    /**
     * The box with its rims and corner posts, by the sides it joins (bit 0 north, 1 east, 2 south, 3 west). Never a
     * full block while a rim stands: vanilla lights every face of a full-block shape from the block beside it, so the
     * rims and posts, which stand above the box, were lit from the neighbouring blocks and drawn black.
     */
    private static final VoxelShape[] SHAPES = new VoxelShape[16];

    static {
        VoxelShape[] rims = {Block.box(0, 16, 0, 16, 17, 2), Block.box(14, 16, 0, 16, 17, 16), Block.box(0, 16, 14, 16, 17, 16),
                Block.box(0, 16, 0, 2, 17, 16)};
        // Posts at the north-west, north-east, south-east and south-west corners, each between two sides.
        VoxelShape[] posts = {Block.box(0, 16, 0, 2, 18, 2), Block.box(14, 16, 0, 16, 18, 2), Block.box(14, 16, 14, 16, 18, 16),
                Block.box(0, 16, 14, 2, 18, 16)};
        for (int joined = 0; joined < 16; joined++) {
            VoxelShape shape = Shapes.block();
            for (int side = 0; side < 4; side++) {
                if ((joined & 1 << side) == 0) {
                    shape = Shapes.or(shape, rims[side]);
                }
                int next = (side + 3) % 4;
                if ((joined & 1 << side) == 0 || (joined & 1 << next) == 0) {
                    shape = Shapes.or(shape, posts[side]);
                }
            }
            SHAPES[joined] = shape.optimize();
        }
    }

    public PlanterBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(WET, false).setValue(SOILED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, WET, SOILED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> connection : CONNECTIONS.entrySet()) {
            BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(connection.getKey()));
            state = state.setValue(connection.getValue(), neighbour.getBlock() instanceof PlanterBoxBlock);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        BooleanProperty connection = CONNECTIONS.get(direction);
        return connection == null ? state : state.setValue(connection, neighbourState.getBlock() instanceof PlanterBoxBlock);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int joined = (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0) | (state.getValue(SOUTH) ? 4 : 0)
                | (state.getValue(WEST) ? 8 : 0);
        return SHAPES[joined];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlanterBoxBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity box)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide()) {
                box.water();
                player.setItemInHand(hand, BucketItem.getEmptySuccessItem(stack, player));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof WateringCanItem) {
            if (WateringCanItem.water(stack) <= 0) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                WateringCanItem.use(stack, 1);
                box.water();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        int slot = PlanterBoxBlockEntity.stockSlot(stack);
        if (slot >= 0) {
            if (!level.isClientSide()) {
                boolean applied = switch (slot) {
                    case PlanterBoxBlockEntity.SEEDS -> box.plant(FarmCrop.of(stack));
                    case PlanterBoxBlockEntity.SOIL -> box.applySoil();
                    default -> box.applyFertilizer(Fertilizer.of(stack));
                };
                // One item a click: used on the crop, else put in the stock. Whole stacks go in through the menu.
                boolean stocked = !applied && box.addToStock(stack.copyWithCount(1)) > 0;
                if (applied || stocked) {
                    stack.consume(1, player);
                    feedback((ServerLevel) level, pos, slot);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * The sound of an item going into the box: seeds are planted; soil and fertilizer fill it as a composter does, with
     * its particles over the soil.
     */
    static void feedback(ServerLevel level, BlockPos pos, int slot) {
        if (slot == PlanterBoxBlockEntity.SEEDS) {
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        level.playSound(null, pos, slot == PlanterBoxBlockEntity.SOIL ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_READY,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0.02);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity box)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (box.isRipe()) {
                box.harvest(player, player.getMainHandItem(), false);
            } else {
                player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) -> new PlanterBoxMenu(containerId, inventory, box,
                        box.data), TITLE));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** A player breaking the box harvests a ripe crop with what they break it with, as a manual harvest. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity box && box.isRipe()) {
            box.harvest(player, player.getMainHandItem(), false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A comparator reads the crop's growth: 0 with no crop, 15 when ripe. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity box && box.crop() != null) {
            return box.isRipe() ? 15 : Math.max(1, 14 * box.stage() / box.crop().stages());
        }
        return 0;
    }
}
