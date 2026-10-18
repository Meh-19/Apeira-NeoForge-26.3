package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraDataGenerators;

/** Apeira's data providers, passed to {@link com.apeira.main.Apeira#DATA}. */
public final class MainData {
    private MainData() {}

    public static void init(ApeiraDataGenerators data) {
        // Lambdas rather than constructor references, so the client-only model classes are not loaded on a dedicated server.
        data.blockStates(() -> new MainBlockStateProvider())
                .itemModels(() -> new MainModelProvider())
                .language(MainLanguageProvider::new)
                .recipes(MainRecipeProvider::new)
                .blockLoot(MainBlockLootSubProvider::new)
                .blockTags(MainBlockTagProvider::new)
                .itemTags(MainItemTagProvider::new)
                .dataMaps(MainDataMapProvider::new);
    }
}
