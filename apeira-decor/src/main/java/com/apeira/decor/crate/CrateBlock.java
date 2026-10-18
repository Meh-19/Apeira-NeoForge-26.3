package com.apeira.decor.crate;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.registry.DecorBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cosmetic crate, ported from CosmoLib's crate entity as a block with a {@link CrateBlockEntity}. Right-click opens
 * it; the rest of the opening is the block entity's (see {@link CrateBlockEntity}). Every crate block is a valid block of
 * {@link DecorBlockEntities#CRATE}, whichever module registers it.
 *
 * <p>The block itself is invisible: its GeckoLib model, {@code geckolib/models/crate/<name>.geo.json}, is drawn by the
 * block entity renderer, which turns it by {@link #ROTATION}. Like CosmoLib's crate it faces the player who placed it,
 * in 45 degree steps. Its block model only gives the breaking particles.
 *
 * <p>The loot is {@code data/<namespace>/crate_loot/<name>.json} ({@link CrateLoot}). A closed crate can be broken
 * and drops itself through its loot table; an open one cannot be broken, by players or explosions.
 */
public class CrateBlock extends Block implements EntityBlock {
    /** The model's facing, in sixteenths of a turn like a sign's; always even, since crates turn in 45 degree steps. */
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    /** How many times the opener can reroll, as in CosmoLib. */
    public static final int DEFAULT_REROLLS = 3;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    /** Bedrock's explosion resistance, so explosions leave an open crate alone. */
    private static final float OPEN_EXPLOSION_RESISTANCE = 3_600_000.0F;

    public CrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    /** How many rerolls an opener gets. */
    public int rerolls() {
        return DEFAULT_REROLLS;
    }

    /** The rotation facing a player looking along {@code yaw}, snapped to 45 degrees. */
    public static int rotationFacing(float yaw) {
        return Math.floorMod(Math.round(yaw / 45.0F) + 4, 8) * 2;
    }

    /** The crate's yaw in degrees, as an entity's: 0 faces south, 90 west. */
    public static float yaw(BlockState state) {
        return state.getValue(ROTATION) * 22.5F;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ROTATION, rotationFacing(context.getRotation()));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != DecorBlockEntities.CRATE.get() ? null
                : (tickerLevel, pos, tickerState, blockEntity) -> ((CrateBlockEntity) blockEntity).serverTick();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity crate) || crate.state() != CrateState.CLOSED) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return player instanceof ServerPlayer serverPlayer && crate.open(serverPlayer) ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    /** Whether someone is opening the crate at {@code pos}. */
    public static boolean isOpen(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CrateBlockEntity crate && crate.state() != CrateState.CLOSED;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return isOpen(level, pos) ? 0.0F : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool, boolean willHarvest,
            FluidState fluid) {
        return !isOpen(level, pos) && super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return isOpen(level, pos) ? OPEN_EXPLOSION_RESISTANCE : super.getExplosionResistance(state, level, pos, explosion);
    }
}
