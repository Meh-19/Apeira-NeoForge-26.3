package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.main.registry.MainBlocks;

/** Block states and block models for {@link MainBlocks}. Block items use their block's model. */
public final class MainBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        simpleBlock(MainBlocks.CARVED_APEIRA_STONE.get());
        simpleBlock(MainBlocks.APEIRA_BRICK.get());
    }
}
