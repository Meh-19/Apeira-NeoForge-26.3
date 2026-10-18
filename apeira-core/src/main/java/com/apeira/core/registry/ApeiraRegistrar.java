package com.apeira.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Owns one module's set of {@link DeferredRegister}s for a single namespace and attaches them all to the mod event
 * bus in one call, so modules never repeat the create-and-register boilerplate.
 *
 * <p>Each module creates one registrar in its {@code @Mod} class and passes its entry holders to
 * {@link #register(IEventBus, RegistryModule...)} from the mod constructor. See {@link RegistryModule} for the
 * holder-class pattern.
 *
 * <p>Registrars default to the shared {@link ApeiraIds#NAMESPACE apeira} namespace. Several modules may use the same
 * namespace, but entry names must then be unique across all of them.
 */
public final class ApeiraRegistrar {
    private final String namespace;
    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final DeferredRegister.DataComponents dataComponents;
    private final DeferredRegister<BlockEntityType<?>> blockEntityTypes;
    private final DeferredRegister<FluidType> fluidTypes;
    private final DeferredRegister<Fluid> fluids;
    /** Every register this registrar owns, keyed by registry, in creation order. */
    private final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> registers = new LinkedHashMap<>();
    private @Nullable IEventBus modEventBus;

    private ApeiraRegistrar(String namespace) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.blocks = track(DeferredRegister.createBlocks(namespace));
        this.items = track(DeferredRegister.createItems(namespace));
        this.dataComponents = track(DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, namespace));
        this.blockEntityTypes = track(DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, namespace));
        this.fluidTypes = track(DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, namespace));
        this.fluids = track(DeferredRegister.create(Registries.FLUID, namespace));
    }

    /** Creates a registrar for the shared {@code apeira} namespace. */
    public static ApeiraRegistrar create() {
        return new ApeiraRegistrar(ApeiraIds.NAMESPACE);
    }

    /** Creates a registrar for a custom namespace. */
    public static ApeiraRegistrar create(String namespace) {
        return new ApeiraRegistrar(namespace);
    }

    private <R extends DeferredRegister<?>> R track(R register) {
        registers.put(register.getRegistryKey(), register);
        return register;
    }

    /**
     * Runs each module so its entries are queued, then attaches every register to the mod event bus. Call once, from
     * the module's {@code @Mod} constructor.
     */
    public void register(IEventBus modEventBus, RegistryModule... modules) {
        if (this.modEventBus != null) {
            throw new IllegalStateException("Registrar for namespace '" + namespace + "' is already registered");
        }
        for (RegistryModule module : modules) {
            module.register(this);
        }
        this.modEventBus = modEventBus;
        registers.values().forEach(register -> register.register(modEventBus));
    }

    /**
     * Returns this registrar's register for any other registry (sounds, creative tabs, menus, ...), creating it on
     * first use. Registers created after {@link #register} are attached to the bus immediately.
     */
    @SuppressWarnings("unchecked")
    public <T> DeferredRegister<T> forRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        DeferredRegister<?> existing = registers.get(registryKey);
        if (existing != null) {
            return (DeferredRegister<T>) existing;
        }
        DeferredRegister<T> created = track(DeferredRegister.create(registryKey, namespace));
        if (modEventBus != null) {
            created.register(modEventBus);
        }
        return created;
    }

    /** Returns {@code <namespace>:<path>} for this registrar's namespace. */
    public Identifier id(String path) {
        return ApeiraIds.id(namespace, path);
    }

    public String namespace() {
        return namespace;
    }

    public DeferredRegister.Blocks blocks() {
        return blocks;
    }

    public DeferredRegister.Items items() {
        return items;
    }

    public DeferredRegister.DataComponents dataComponents() {
        return dataComponents;
    }

    public DeferredRegister<BlockEntityType<?>> blockEntityTypes() {
        return blockEntityTypes;
    }

    public DeferredRegister<FluidType> fluidTypes() {
        return fluidTypes;
    }

    public DeferredRegister<Fluid> fluids() {
        return fluids;
    }

    /** All registers owned by this registrar, in creation order. */
    public Collection<DeferredRegister<?>> registers() {
        return Collections.unmodifiableCollection(registers.values());
    }

    /** Registers a block together with a plain {@link net.minecraft.world.item.BlockItem} of the same name. */
    public <B extends Block> DeferredBlock<B> blockWithItem(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = blocks.registerBlock(name, factory, properties);
        items.registerSimpleBlockItem(block);
        return block;
    }

    /** Registers a block with default properties together with a plain block item of the same name. */
    public <B extends Block> DeferredBlock<B> blockWithItem(String name, Function<BlockBehaviour.Properties, ? extends B> factory) {
        return blockWithItem(name, factory, UnaryOperator.identity());
    }

    /** Registers an item data component type; see {@link DataComponentType.Builder} for codecs and syncing. */
    public <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> dataComponent(String name,
            UnaryOperator<DataComponentType.Builder<D>> builder) {
        return dataComponents.registerComponentType(name, builder);
    }

    /** Registers a block entity type valid for the given blocks, which are resolved at registration time. */
    @SafeVarargs
    public final <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> blockEntityType(String name,
            BlockEntityType.BlockEntitySupplier<? extends T> factory, Supplier<? extends Block>... validBlocks) {
        if (validBlocks.length == 0) {
            throw new IllegalArgumentException("Block entity type '" + name + "' needs at least one valid block");
        }
        return blockEntityTypes.register(name, () -> {
            Set<Block> resolved = new HashSet<>();
            for (Supplier<? extends Block> block : validBlocks) {
                resolved.add(block.get());
            }
            return new BlockEntityType<T>(factory, Set.copyOf(resolved));
        });
    }

    /** Registers a NeoForge fluid type (the physical and visual properties shared by a source and flowing fluid). */
    public DeferredHolder<FluidType, FluidType> fluidType(String name, FluidType.Properties properties) {
        return fluidTypes.register(name, () -> new FluidType(properties));
    }
}
