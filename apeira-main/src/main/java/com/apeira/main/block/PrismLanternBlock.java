package com.apeira.main.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Xenon's prism lantern: right-clicking it switches {@link #LIT}, which swaps its still texture for a blinking one. As
 * in Xenon, its light (a crystal block's 5) is the same either way.
 */
public class PrismLanternBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public PrismLanternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(LIT), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
