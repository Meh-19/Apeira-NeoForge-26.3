package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A placed raw or cooked turkey, eaten in three bites like a cake ({@link #BITES}). Each bite restores 3 hunger and 0.3
 * saturation raw, with a 30% chance of Hunger for 30 seconds, or 6 and 0.6 cooked; after the third the turkey becomes a
 * skeleton. It needs a solid block under it and drops nothing: the turkey item placed it, and pick-block gives it back.
 */
public class TurkeyMeatBlock extends Block {
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 2);
    private static final VoxelShape[] SHAPES = {Block.box(3, 0, 3, 13, 8, 13), Block.box(3, 0, 3, 13, 7, 13), Block.box(3, 0, 3, 13, 6, 13)};
    private static final float POISON_CHANCE = 0.3F;

    private final boolean cooked;
    private final Supplier<? extends Item> item;
    private final Supplier<? extends Block> skeleton;

    public TurkeyMeatBlock(Properties properties, boolean cooked, Supplier<? extends Item> item, Supplier<? extends Block> skeleton) {
        super(properties);
        this.cooked = cooked;
        this.item = item;
        this.skeleton = skeleton;
        registerDefaultState(stateDefinition.any().setValue(BITES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.getFoodData().eat(cooked ? 6 : 3, cooked ? 0.6F : 0.3F);
            if (!cooked && level.getRandom().nextFloat() < POISON_CHANCE) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600, 0));
            }
            level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            level.gameEvent(player, GameEvent.EAT, pos);
            int bites = state.getValue(BITES);
            level.setBlock(pos, bites >= 2 ? skeleton.get().defaultBlockState() : state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(item.get());
    }
}
