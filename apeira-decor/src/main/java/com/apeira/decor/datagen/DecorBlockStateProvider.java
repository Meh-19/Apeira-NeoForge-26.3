package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.decor.registry.DecorBlocks;

/**
 * Block states for {@link DecorBlocks}. Furniture models are hand-made (converted from the server pack) and live in
 * {@code src/main/resources/assets/apeira/models/block/}, facing north; block items use the block's model.
 */
public final class DecorBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        furniture(DecorBlocks.OAK_CHAIR.get());
    }
}
