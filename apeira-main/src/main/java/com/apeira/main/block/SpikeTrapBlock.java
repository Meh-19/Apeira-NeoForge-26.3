package com.apeira.main.block;

import org.jspecify.annotations.Nullable;

import com.apeira.main.block.entity.SpikeTrapBlockEntity;
import com.apeira.main.registry.MainBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Xenon's spike trap: faces away from the placing player, like a piston. A redstone signal pushes its spikes out
 * ({@link #SPIKES_OUT}), breaking the block in front if its hardness is at most 3 (dropping it), and while they are out
 * its block entity hurts every living thing in that block for 1 damage a tick (cactus damage).
 */
public class SpikeTrapBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty SPIKES_OUT = BooleanProperty.create("spikes_out");
    /** The hardest block the spikes break: stone is 1.5, obsidian 50. */
    private static final float MAX_BREAKABLE_HARDNESS = 3.0F;

    public SpikeTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(SPIKES_OUT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPIKES_OUT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(SPIKES_OUT)) {
            return;
        }
        level.setBlock(pos, state.setValue(SPIKES_OUT, powered), Block.UPDATE_ALL);
        level.playSound(null, pos, powered ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (powered) {
            BlockPos target = pos.relative(state.getValue(FACING));
            BlockState targetState = level.getBlockState(target);
            float hardness = targetState.getDestroySpeed(level, target);
            if (!targetState.isAir() && hardness >= 0 && hardness <= MAX_BREAKABLE_HARDNESS) {
                level.destroyBlock(target, true);
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpikeTrapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide() && type == MainBlockEntities.SPIKE_TRAP.get()
                ? (tickLevel, pos, tickState, blockEntity) -> SpikeTrapBlockEntity.serverTick(tickLevel, pos, tickState)
                : null;
    }
}
