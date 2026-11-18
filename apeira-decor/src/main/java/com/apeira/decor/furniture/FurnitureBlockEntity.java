package com.apeira.decor.furniture;

import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.decor.registry.DecorBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A furniture piece's paint, drawn by {@code FurnitureRenderer} ({@link DecorBlockEntities#FURNITURE}). */
public class FurnitureBlockEntity extends PaintBlockEntity {
    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        this(DecorBlockEntities.FURNITURE.get(), pos, state);
    }

    protected FurnitureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
