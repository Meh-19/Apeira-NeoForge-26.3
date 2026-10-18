package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraDataGenerators;

/** The decor module's data providers, passed to {@link com.apeira.decor.ApeiraDecor#DATA}. */
public final class DecorData {
    private DecorData() {}

    public static void init(ApeiraDataGenerators data) {
        // A lambda rather than a constructor reference, so the client-only model classes are not loaded on a dedicated server.
        data.blockStates(() -> new DecorBlockStateProvider())
                .itemModels(() -> new DecorItemModelProvider())
                .language(DecorLanguageProvider::new)
                .recipes(DecorRecipeProvider::new)
                .blockLoot(DecorBlockLootSubProvider::new)
                .blockTags(DecorBlockTagProvider::new);
    }
}
