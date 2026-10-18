package com.apeira.core.network;

/**
 * One group of payload registrations, such as a module's furniture payloads, handed to
 * {@link ApeiraNetwork#register(net.neoforged.bus.api.IEventBus, PayloadModule...)}. Mirrors
 * {@link com.apeira.core.registry.RegistryModule}.
 */
@FunctionalInterface
public interface PayloadModule {
    void register(ApeiraNetwork network);
}
