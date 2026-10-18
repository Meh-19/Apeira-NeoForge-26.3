package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraItemModelProvider;
import com.apeira.main.registry.MainItems;

/**
 * Item models for {@link MainItems}, each a flat sprite from {@code item/<name>}.
 *
 * <p>Core's {@code ApeiraModelProvider} is the final vanilla model provider that runs every module's model providers,
 * so module item models extend {@link ApeiraItemModelProvider} instead.
 */
public final class MainModelProvider extends ApeiraItemModelProvider {
    @Override
    protected void registerModels() {
        simpleItem(MainItems.APEIRA_INGOT);
        simpleItem(MainItems.APEIRA_GEM);
        simpleItem(MainItems.APEIRA_RATION);
        simpleItem(MainItems.EMBER_SHARD);
        simpleItem(MainItems.MOSS_CLUMP);
    }
}
