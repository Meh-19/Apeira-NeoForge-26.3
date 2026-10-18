package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The tip of one of Xenon's hanging vines (damp moss, grimoss, sculk vines, warped veil), a vanilla growing plant like
 * weeping vines: it hangs from a full face or from the rest of its vine ({@link HangingVinePlantBlock}), grows down
 * into air with a chance of 0.11 per random tick, and bone meal grows it one block.
 *
 * <p>Xenon's names are the other way round: its {@code <vine>_hang}, {@code sculk_vines} and {@code warped_veil}
 * blocks are the tips, drawn with its {@code ..._bottom} or {@code ..._end} models.
 */
public class HangingVineBlock extends GrowingPlantHeadBlock {
    public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    private final Supplier<? extends Block> body;

    public HangingVineBlock(Properties properties, Supplier<? extends Block> body) {
        super(properties, Direction.DOWN, SHAPE, false, 0.11);
        this.body = body;
    }

    @Override
    protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
        return 1;
    }

    @Override
    protected boolean canGrowInto(BlockState state) {
        return state.isAir();
    }

    @Override
    protected Block getBodyBlock() {
        return body.get();
    }
}
