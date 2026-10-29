package com.apeira.main.farming;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Origin Realms' sprinklers: a tank on a post with four spray arms that waters the planter boxes around it, a square of
 * {@link #radius} blocks to each side (its own block left out; one block up or down too), every day it has water.
 *
 * <p>It holds {@link #CAPACITY} days of water. A water bucket fills it; a watering can pours in a day per charge, as many
 * as there is room for (a full can fills an empty sprinkler), and rain now and then adds a day. Each 7:00 AM tick
 * uses up a day ({@link FarmClock.Sprinkler}: the water is worked out from the farming clock, so a sprinkler needs no
 * ticking and keeps watering in unloaded chunks). {@link #LEVEL} is the days of water left and picks Origin Realms'
 * model for it, the tank's water lower each day and no spray when empty. Its water reaches the boxes ring by ring
 * ({@link FarmingService#startWave}).
 */
public class SprinklerBlock extends BaseEntityBlock {
    public static final int CAPACITY = 5;
    public static final int MAX_LEVEL = CAPACITY;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 9, 13), Block.box(7, 9, 7, 9, 12, 9),
            Block.box(0, 12, 6, 16, 16, 10), Block.box(6, 12, 0, 10, 16, 16));

    private final int radius;

    /** A sprinkler watering {@code radius} blocks to each side: 2 is 5x5, 3 is 7x7. */
    public SprinklerBlock(int radius, BlockBehaviour.Properties properties) {
        super(properties);
        this.radius = radius;
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    public int radius() {
        return radius;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SprinklerBlockEntity(pos, state);
    }

    /** A new sprinkler is registered empty. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
            FarmClock clock = FarmClock.get(serverLevel.getServer());
            if (clock.sprinkler(serverLevel.dimension(), pos) == null) {
                clock.setSprinkler(new FarmClock.Sprinkler(serverLevel.dimension(), pos, clock.day(), 0, radius));
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        boolean bucket = stack.is(Items.WATER_BUCKET);
        boolean can = stack.getItem() instanceof WateringCanItem && WateringCanItem.water(stack) > 0;
        if (!bucket && !can) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (state.getValue(LEVEL) >= MAX_LEVEL) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.apeira.farming.sprinkler_full"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (bucket) {
                FarmingService.setSprinklerWater(serverLevel, pos, CAPACITY, radius);
                player.setItemInHand(hand, BucketItem.getEmptySuccessItem(stack, player));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                FarmClock clock = FarmClock.get(serverLevel.getServer());
                FarmClock.Sprinkler current = clock.sprinkler(serverLevel.dimension(), pos);
                int left = current == null ? 0 : current.waterLeft(clock.day());
                int poured = WateringCanItem.use(stack, CAPACITY - left);
                FarmingService.setSprinklerWater(serverLevel, pos, left + poured, radius);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A comparator reads the days of water left, three per day. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(LEVEL) * 3;
    }
}
