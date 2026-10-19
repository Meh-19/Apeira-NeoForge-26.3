package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.world.MainBiomeModifiers;
import com.apeira.main.world.MainBiomes;
import com.apeira.main.world.MainConfiguredFeatures;
import com.apeira.main.world.MainPlacedFeatures;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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
                .registryEntries(gatherer -> gatherer.lootTable(new LootTableProvider.SubProviderEntry(MainEntityLootSubProvider::new,
                        LootContextParamSets.ENTITY)))
                .registryEntries(gatherer -> gatherer.add(Registries.FEATURE, MainConfiguredFeatures::bootstrap)
                        .add(Registries.PLACED_FEATURE, MainPlacedFeatures::bootstrap)
                        .add(Registries.BIOME, MainBiomes::bootstrap)
                        .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, MainBiomeModifiers::bootstrap))
                .blockTags(MainBlockTagProvider::new)
                .itemTags(MainItemTagProvider::new)
                .server(MainEntityTagProvider::new)
                .dataMaps(MainDataMapProvider::new);
    }
}
