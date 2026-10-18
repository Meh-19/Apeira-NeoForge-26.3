package com.apeira.core.datagen;

/**
 * One group of data providers, such as a module's furniture models and recipes, handed to
 * {@link ApeiraDataGenerators#register(net.neoforged.bus.api.IEventBus, DataModule...)}. Mirrors
 * {@link com.apeira.core.registry.RegistryModule}.
 */
@FunctionalInterface
public interface DataModule {
    void register(ApeiraDataGenerators generators);
}
