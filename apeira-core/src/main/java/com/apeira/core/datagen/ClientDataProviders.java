package com.apeira.core.datagen;

import java.util.List;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Builds the providers that use client-only classes. Kept apart from {@link ApeiraDataGenerators} so that class can
 * load on a dedicated server, where the vanilla model generators do not exist.
 */
final class ClientDataProviders {
    private ClientDataProviders() {}

    static DataProvider models(PackOutput output, ApeiraRegistrar registrar, List<Supplier<? extends ApeiraBlockStateProvider>> blockStates,
            List<Supplier<? extends ApeiraItemModelProvider>> itemModels) {
        return new ApeiraModelProvider(output, registrar,
                blockStates.stream().<ApeiraBlockStateProvider>map(Supplier::get).toList(),
                itemModels.stream().<ApeiraItemModelProvider>map(Supplier::get).toList());
    }
}
