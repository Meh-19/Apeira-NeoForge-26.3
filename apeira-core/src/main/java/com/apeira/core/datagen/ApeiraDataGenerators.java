package com.apeira.core.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.DatapackRegistryGatherer;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

/**
 * Collects one module's data providers and adds them to the data generator when NeoForge fires the gather events.
 *
 * <p>Each module creates one instance from its {@link ApeiraRegistrar} in its {@code @Mod} class, declares providers
 * in holder classes and passes them to {@link #register(IEventBus, DataModule...)} from the mod constructor:
 *
 * <pre>{@code
 * public static final ApeiraDataGenerators DATA = ApeiraDataGenerators.create(REGISTRAR);
 *
 * public final class DecorData {
 *     public static void init(ApeiraDataGenerators data) {
 *         data.blockStates(DecorBlockStates::new)
 *             .itemModels(DecorItemModels::new)
 *             .language(DecorLanguage::new)
 *             .recipes(DecorRecipes::new)
 *             .blockLoot(DecorBlockLoot::new)
 *             .blockTags(DecorBlockTags::new)
 *             .dataMaps(DecorDataMaps::new);
 *     }
 * }
 *
 * // In the module's @Mod constructor:
 * DATA.register(modEventBus, DecorData::init);
 * }</pre>
 *
 * <h2>Which event runs what</h2>
 *
 * <p>NeoForge 26.x splits data generation in three:
 * <ul>
 * <li>{@link GatherDataEvent.Client} fires for the {@code clientData} run, which is the run apeira-main configures.
 * It generates everything, so assets <em>and</em> data providers are added here.</li>
 * <li>{@link GatherDataEvent.Server} fires for a {@code serverData} run, which only generates data, so only
 * {@linkplain #server data providers} are added here.</li>
 * <li>{@link GatherDataRegistryEntriesEvent} fires before both. Recipes are datapack registry entries in 26.x and are
 * generated from here, through {@link DatapackRegistryGatherer#recipe}, rather than as a {@link DataProvider}. Loot
 * tables are too, through {@link DatapackRegistryGatherer#lootTable}.</li>
 * </ul>
 *
 * <p>Generated files are only written for mods named with {@code --mod} in the data run. Entries are generated for
 * this generator's registrar namespace, and models are validated against that registrar's blocks and items, so
 * several modules can share the {@code apeira} namespace without checking each other's entries.
 */
public final class ApeiraDataGenerators {
    private final ApeiraRegistrar registrar;
    private final List<DataProviderFactory> clientProviders = new ArrayList<>();
    private final List<DataProviderFactory> serverProviders = new ArrayList<>();
    private final List<Supplier<? extends ApeiraBlockStateProvider>> blockStates = new ArrayList<>();
    private final List<Supplier<? extends ApeiraItemModelProvider>> itemModels = new ArrayList<>();
    private final List<RecipeFactory> recipes = new ArrayList<>();
    private final List<RegistryEntries> registryEntries = new ArrayList<>();
    private @Nullable IEventBus modEventBus;

    private ApeiraDataGenerators(ApeiraRegistrar registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    /** Creates a generator set for the entries and namespace of {@code registrar}. */
    public static ApeiraDataGenerators create(ApeiraRegistrar registrar) {
        return new ApeiraDataGenerators(registrar);
    }

    /**
     * Runs each module so its providers are queued, then listens for the gather events. Call once, from the module's
     * {@code @Mod} constructor.
     */
    public void register(IEventBus modEventBus, DataModule... modules) {
        if (this.modEventBus != null) {
            throw new IllegalStateException("Data generators for namespace '" + namespace() + "' are already registered");
        }
        for (DataModule module : modules) {
            module.register(this);
        }
        this.modEventBus = modEventBus;
        modEventBus.addListener(this::onGatherRegistryEntries);
        modEventBus.addListener(this::onGatherClientData);
        modEventBus.addListener(this::onGatherServerData);
    }

    private void onGatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        if (recipes.isEmpty() && registryEntries.isEmpty()) {
            return;
        }
        // Gather for the registrar's namespace rather than the mod id, which differs for library modules.
        DatapackRegistryGatherer gatherer = event.gatherFor(namespace());
        recipes.forEach(factory -> gatherer.recipe((recipeOutput, advancementOutput) ->
                factory.create(recipeOutput, advancementOutput, namespace())));
        registryEntries.forEach(entries -> entries.gather(gatherer));
    }

    private void onGatherClientData(GatherDataEvent.Client event) {
        Context context = new Context(event, registrar);
        if (!blockStates.isEmpty() || !itemModels.isEmpty()) {
            event.addProvider(ClientDataProviders.models(context.output(), registrar, blockStates, itemModels));
        }
        clientProviders.forEach(factory -> event.addProvider(factory.create(context)));
        serverProviders.forEach(factory -> event.addProvider(factory.create(context)));
    }

    private void onGatherServerData(GatherDataEvent.Server event) {
        Context context = new Context(event, registrar);
        serverProviders.forEach(factory -> event.addProvider(factory.create(context)));
    }

    /** Adds a provider of client resources (models, sounds, particles, ...), generated by the client data run only. */
    public ApeiraDataGenerators client(DataProviderFactory factory) {
        clientProviders.add(queue(factory));
        return this;
    }

    /** Adds a provider of server data (tags, loot, data maps, ...), generated by both the client and server data runs. */
    public ApeiraDataGenerators server(DataProviderFactory factory) {
        serverProviders.add(queue(factory));
        return this;
    }

    /**
     * Adds block state and block model definitions. All of this generator's block state and item model providers are
     * written by one vanilla model provider, which checks that every block and item of the registrar has a model.
     */
    public ApeiraDataGenerators blockStates(Supplier<? extends ApeiraBlockStateProvider> provider) {
        blockStates.add(queue(provider));
        return this;
    }

    /** Adds item model definitions; see {@link #blockStates}. */
    public ApeiraDataGenerators itemModels(Supplier<? extends ApeiraItemModelProvider> provider) {
        itemModels.add(queue(provider));
        return this;
    }

    /** Adds a language file. The factory receives the pack output and this generator's namespace. */
    public ApeiraDataGenerators language(BiFunction<PackOutput, String, ? extends ApeiraLanguageProvider> factory) {
        return client(context -> factory.apply(context.output(), context.namespace()));
    }

    /** Adds recipes and their unlock advancements. */
    public ApeiraDataGenerators recipes(RecipeFactory factory) {
        recipes.add(queue(factory));
        return this;
    }

    /** Adds block tags, such as the mining tool tags. */
    public ApeiraDataGenerators blockTags(Function<Context, ? extends ApeiraBlockTagsProvider> factory) {
        return server(factory::apply);
    }

    /**
     * Adds block loot tables. Loot tables are datapack registry entries in 26.x, so they are generated through
     * {@link DatapackRegistryGatherer#lootTable} like {@linkplain #recipes recipes}.
     */
    public ApeiraDataGenerators blockLoot(BiFunction<LootTableSubProvider.Context, ApeiraRegistrar, ? extends ApeiraBlockLootProvider> factory) {
        return registryEntries(gatherer -> gatherer.lootTable(new LootTableProvider.SubProviderEntry(
                context -> factory.apply(context, registrar), LootContextParamSets.BLOCK)));
    }

    /** Adds NeoForge data maps. */
    public ApeiraDataGenerators dataMaps(Function<Context, ? extends ApeiraDataMapProvider> factory) {
        return server(factory::apply);
    }

    /**
     * Adds other datapack registry entries (worldgen, damage types, loot tables, advancements, ...). The gatherer
     * only generates entries in this generator's namespace.
     */
    public ApeiraDataGenerators registryEntries(RegistryEntries entries) {
        registryEntries.add(queue(entries));
        return this;
    }

    public ApeiraRegistrar registrar() {
        return registrar;
    }

    public String namespace() {
        return registrar.namespace();
    }

    private <T> T queue(T registration) {
        if (modEventBus != null) {
            throw new IllegalStateException("Data providers must be added before ApeiraDataGenerators#register is called");
        }
        return Objects.requireNonNull(registration, "registration");
    }

    /** What a {@link DataProviderFactory} can build its provider from. */
    public record Context(GatherDataEvent event, ApeiraRegistrar registrar) {
        public PackOutput output() {
            return event.getGenerator().getPackOutput();
        }

        /** The reloadable registries (recipes, loot tables, tags, ...) with all modded entries. */
        public CompletableFuture<HolderLookup.Provider> lookup() {
            return event.getReloadableLookupProvider();
        }

        public String namespace() {
            return registrar.namespace();
        }
    }

    /** Creates a data provider when the gather event fires. */
    @FunctionalInterface
    public interface DataProviderFactory {
        DataProvider create(Context context);
    }

    /** Creates a recipe provider from the recipe and advancement bootstrap contexts and the namespace. */
    @FunctionalInterface
    public interface RecipeFactory {
        ApeiraRecipeProvider create(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace);
    }

    /** Adds datapack registry entries to the gatherer for this generator's namespace. */
    @FunctionalInterface
    public interface RegistryEntries {
        void gather(DatapackRegistryGatherer gatherer);
    }
}
