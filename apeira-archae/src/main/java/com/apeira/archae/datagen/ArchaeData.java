package com.apeira.archae.datagen;

import com.apeira.core.datagen.ApeiraDataGenerators;

/** The archae module's data providers, run by the {@code archaeData} run. */
public final class ArchaeData {
    private ArchaeData() {}

    public static void init(ApeiraDataGenerators data) {
        // Lambdas rather than constructor references, so the client-only model classes are not loaded on a dedicated server.
        data.blockStates(() -> new ArchaeBlockStateProvider())
                .itemModels(() -> new ArchaeItemModelProvider())
                .language(ArchaeLanguageProvider::new)
                .blockLoot(ArchaeBlockLootSubProvider::new)
                .itemTags(ArchaeItemTagProvider::new)
                .server(context -> new AssemblyRecipeProvider(context.output()));
    }
}
