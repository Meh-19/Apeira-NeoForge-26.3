package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Apeira's items. Block items are registered with their blocks in {@link MainBlocks}.
 *
 * <p>Food, furnace fuel and composter chances are vanilla item properties in 26.x. Fuel burn times and compost chances
 * are keys into the {@code minecraft:context_int_provider} registry; the vanilla keys are in {@link ContextIntProviders}.
 */
public final class MainItems {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    // Crafting materials
    public static final DeferredItem<Item> APEIRA_INGOT = R.items().registerSimpleItem("apeira_ingot");
    public static final DeferredItem<Item> APEIRA_GEM = R.items().registerSimpleItem("apeira_gem");

    // Food: 10 hunger points and 20 saturation, more than any vanilla food.
    public static final DeferredItem<Item> APEIRA_RATION = R.items().registerSimpleItem("apeira_ration",
            p -> p.food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0F).build()));

    // Fuel: burns as long as a blaze rod (12 items in a furnace).
    public static final DeferredItem<Item> EMBER_SHARD = R.items().registerSimpleItem("ember_shard",
            p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_BLAZE_ROD));

    // Compostable: raises a composter's level with the same chance as an apple (65%).
    public static final DeferredItem<Item> MOSS_CLUMP = R.items().registerSimpleItem("moss_clump",
            p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));

    private MainItems() {}

    public static void init(ApeiraRegistrar registrar) {}
}
