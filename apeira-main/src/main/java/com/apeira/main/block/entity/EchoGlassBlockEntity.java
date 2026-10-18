package com.apeira.main.block.entity;

import com.apeira.main.registry.MainBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds no data: echo glass has a block entity only so that a renderer can draw it when it should be seen. */
public class EchoGlassBlockEntity extends BlockEntity {
    public EchoGlassBlockEntity(BlockPos pos, BlockState state) {
        super(MainBlockEntities.ECHO_GLASS.get(), pos, state);
    }
}
