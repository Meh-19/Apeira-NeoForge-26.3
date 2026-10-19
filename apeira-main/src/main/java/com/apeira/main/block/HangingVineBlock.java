package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
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
 *
 * <p>A vine has one item, {@link #item()}: its tip's for sculk vines and warped veil, the moss cover's for the moss
 * hangs, which the cover's item places.
 */
public class HangingVineBlock extends GrowingPlantHeadBlock {
    public static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    private final Supplier<? extends Block> body;
    private final Supplier<? extends ItemLike> item;

    /** A vine picked and dropped as its own item. */
    public HangingVineBlock(Properties properties, Supplier<? extends Block> body) {
        this(properties, body, null);
    }

    /** A vine picked and dropped as {@code item}; {@code null} for its own. */
    public HangingVineBlock(Properties properties, Supplier<? extends Block> body, Supplier<? extends ItemLike> item) {
        super(properties, Direction.DOWN, SHAPE, false, 0.11);
        this.body = body;
        this.item = item != null ? item : () -> this;
    }

    /** The vine's one item. */
    public ItemLike item() {
        return item.get();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(item());
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
