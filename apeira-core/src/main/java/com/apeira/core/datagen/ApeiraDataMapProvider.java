package com.apeira.core.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.builtin.Flammable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Transformable;
import net.neoforged.neoforge.registries.datamaps.builtin.VillagerCompostable;

/**
 * Base for one module's NeoForge data maps, written to {@code data/<namespace>/data_maps/<registry>/<map>.json}
 * (for example {@code data/neoforge/data_maps/item/villager_compostables.json} for a NeoForge map, or
 * {@code data/apeira/data_maps/item/<map>.json} for a map registered by an Apeira module).
 *
 * <p>Custom item values, such as sell prices or quest rewards, are data maps the module registers itself with
 * {@code RegisterDataMapTypesEvent} and fills here with {@link #item}.
 *
 * <h2>Furnace fuels and composter chances</h2>
 *
 * <p>Minecraft 26.x made both data-driven in vanilla, and NeoForge 26.3 removed its {@code furnace_fuels} and
 * {@code compostables} data maps. An item opts in through {@code Item.Properties#cookingFuel} and
 * {@code Item.Properties#compostable}, which take a key into the {@code minecraft:context_int_provider} datapack
 * registry: vanilla ships keys such as {@code ContextIntProviders.COOKING_TIME_COAL} or
 * {@code ContextIntProviders.COMPOSTABLE_MEDIUM}, and custom values are registry entries generated with
 * {@link ApeiraDataGenerators#registryEntries}.
 */
public abstract class ApeiraDataMapProvider extends DataMapProvider {
    protected ApeiraDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    /** Creates a provider from the context {@link ApeiraDataGenerators#dataMaps} passes to its factory. */
    protected ApeiraDataMapProvider(ApeiraDataGenerators.Context context) {
        this(context.output(), context.lookup());
    }

    /** Sets {@code item}'s value in an item data map. */
    protected <T> void item(DataMapType<Item, T> type, ItemLike item, T value) {
        builder(type).add(holder(item), value, false);
    }

    /** Sets the value of every item in {@code tag}. Entries for single items take precedence. */
    protected <T> void item(DataMapType<Item, T> type, TagKey<Item> tag, T value) {
        builder(type).add(tag, value, false);
    }

    /** Sets {@code block}'s value in a block data map. */
    protected <T> void block(DataMapType<Block, T> type, Block block, T value) {
        builder(type).add(block.builtInRegistryHolder(), value, false);
    }

    /** Sets the value of every block in {@code tag}. */
    protected <T> void block(DataMapType<Block, T> type, TagKey<Block> tag, T value) {
        builder(type).add(tag, value, false);
    }

    /** Lets farmer villagers put {@code item} in composters. The item must also be {@code compostable}. */
    protected void villagerCompostable(ItemLike item) {
        item(NeoForgeDataMaps.VILLAGER_COMPOSTABLES, item, new VillagerCompostable(true));
    }

    /** Makes {@code block} catch fire ({@code igniteOdds}) and burn away ({@code burnOdds}); planks use 5 and 20. */
    protected void flammable(Block block, int igniteOdds, int burnOdds) {
        block(NeoForgeDataMaps.FLAMMABLE, block, new Flammable(igniteOdds, burnOdds));
    }

    /** Lets axes strip {@code log} into {@code stripped}, keeping its axis, as vanilla logs are stripped. */
    protected void strippable(Block log, Block stripped) {
        block(NeoForgeDataMaps.TRANSFORMABLES, log, Transformable.stripping(log, stripped));
    }

    private static Holder<Item> holder(ItemLike item) {
        return item.asItem().builtInRegistryHolder();
    }

    @Override
    public String getName() {
        return "Data Maps - " + getClass().getSimpleName();
    }
}
