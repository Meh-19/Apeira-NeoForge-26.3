package com.apeira.main.block;

import org.jspecify.annotations.Nullable;

import com.apeira.main.block.entity.EchoGlassBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Xenon's echo glass: a solid block that cannot be seen unless the player holds echo glass in their main hand, when its
 * block entity's renderer ({@code client/EchoGlassRenderer}) draws it. Dolomite dust turns it into aquarium glass.
 */
public class EchoGlassBlock extends Block implements EntityBlock {
    public EchoGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoGlassBlockEntity(pos, state);
    }
}
