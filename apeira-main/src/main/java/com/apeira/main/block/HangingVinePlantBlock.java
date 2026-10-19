package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The rest of one of Xenon's hanging vines, above its tip ({@link HangingVineBlock}). */
public class HangingVinePlantBlock extends GrowingPlantBodyBlock {
    private final Supplier<? extends GrowingPlantHeadBlock> head;

    public HangingVinePlantBlock(Properties properties, Supplier<? extends GrowingPlantHeadBlock> head) {
        super(properties, Direction.DOWN, HangingVineBlock.SHAPE, false);
        this.head = head;
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return head.get();
    }

    /** The vine's item ({@link HangingVineBlock#item()}), as this block has none of its own. */
    public ItemLike item() {
        return head.get() instanceof HangingVineBlock vine ? vine.item() : head.get();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(item());
    }
}
