package com.apeira.decor.furniture;

import com.apeira.decor.multiblock.Multiblock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A furniture piece's item: placed only where every block of the piece's footprint is free, as artifacts are. */
public class FurnitureBlockItem extends BlockItem {
    public FurnitureBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return super.canPlace(context, state) && Multiblock.canPlace(context.getLevel(), context.getClickedPos(), state);
    }
}
