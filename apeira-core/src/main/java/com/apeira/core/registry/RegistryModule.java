package com.apeira.core.registry;

/**
 * One group of registry entries, such as a module's blocks or items, handed to
 * {@link ApeiraRegistrar#register(net.neoforged.bus.api.IEventBus, RegistryModule...)}.
 *
 * <p>The usual shape is a holder class whose static fields are created from the registrar, plus a static
 * {@code init} method passed as a method reference. Calling it forces the class to load, so every field is queued on
 * its register before NeoForge fires the registry events:
 *
 * <pre>{@code
 * public final class DecorBlocks {
 *     private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;
 *
 *     public static final DeferredBlock<ChairBlock> OAK_CHAIR =
 *             R.blockWithItem("oak_chair", ChairBlock::new, p -> p.strength(2.0F));
 *
 *     public static void init(ApeiraRegistrar registrar) {}
 * }
 *
 * // In the module's @Mod constructor:
 * REGISTRAR.register(modEventBus, DecorBlocks::init, DecorItems::init);
 * }</pre>
 */
@FunctionalInterface
public interface RegistryModule {
    void register(ApeiraRegistrar registrar);
}
