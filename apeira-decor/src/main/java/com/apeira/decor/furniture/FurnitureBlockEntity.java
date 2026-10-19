package com.apeira.decor.furniture;

import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.decor.registry.DecorBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block entity of every {@link FurnitureBlock}, which {@code FurnitureRenderer} draws, as CosmoLib's furniture
 * block entities were. It keeps the piece's paint like any {@link PaintBlockEntity}.
 */
public class FurnitureBlockEntity extends PaintBlockEntity {
    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(DecorBlockEntities.FURNITURE.get(), pos, state);
    }
}
