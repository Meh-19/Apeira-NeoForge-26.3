package com.apeira.main.block;

import java.util.function.Supplier;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;

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
}
