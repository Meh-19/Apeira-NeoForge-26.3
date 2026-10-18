package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraRecipeProvider;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Crafting recipes for Apeira's blocks and items. The material recipes are placeholders until Apeira ores exist
 * (Phase 6).
 */
public final class MainRecipeProvider extends ApeiraRecipeProvider {
    public MainRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace) {
        super(recipes, advancements, namespace);
    }

    @Override
    protected void buildRecipes() {
        // Materials
        shaped(RecipeCategory.MISC, MainItems.APEIRA_GEM)
                .pattern(" A ").pattern("ADA").pattern(" A ")
                .define('A', Items.AMETHYST_SHARD).define('D', Items.DIAMOND)
                .unlockedBy(getHasName(Items.DIAMOND), has(Items.DIAMOND))
                .save(output);
        shapeless(RecipeCategory.MISC, MainItems.APEIRA_INGOT)
                .requires(Items.IRON_INGOT).requires(MainItems.APEIRA_GEM)
                .unlockedBy(getHasName(MainItems.APEIRA_GEM), has(MainItems.APEIRA_GEM))
                .save(output);

        // Building blocks
        shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.CARVED_APEIRA_STONE, 8)
                .pattern("SSS").pattern("SGS").pattern("SSS")
                .define('S', Items.STONE_BRICKS).define('G', MainItems.APEIRA_GEM)
                .unlockedBy(getHasName(MainItems.APEIRA_GEM), has(MainItems.APEIRA_GEM))
                .save(output);
        shaped(RecipeCategory.BUILDING_BLOCKS, MainBlocks.APEIRA_BRICK, 4)
                .pattern("CC").pattern("CC")
                .define('C', MainBlocks.CARVED_APEIRA_STONE)
                .unlockedBy(getHasName(MainBlocks.CARVED_APEIRA_STONE), has(MainBlocks.CARVED_APEIRA_STONE))
                .save(output);
        stonecutting(RecipeCategory.BUILDING_BLOCKS, MainBlocks.APEIRA_BRICK, MainBlocks.CARVED_APEIRA_STONE, 1);

        // Food, fuel and compost
        shapeless(RecipeCategory.FOOD, MainItems.APEIRA_RATION)
                .requires(Items.BREAD).requires(Items.COOKED_BEEF).requires(Items.APPLE)
                .unlockedBy(getHasName(Items.COOKED_BEEF), has(Items.COOKED_BEEF))
                .save(output);
        shapeless(RecipeCategory.MISC, MainItems.EMBER_SHARD, 2)
                .requires(Items.BLAZE_POWDER).requires(Items.COAL)
                .unlockedBy(getHasName(Items.BLAZE_POWDER), has(Items.BLAZE_POWDER))
                .save(output);
        shapeless(RecipeCategory.MISC, MainItems.MOSS_CLUMP, 4)
                .requires(Items.MOSS_BLOCK)
                .unlockedBy(getHasName(Items.MOSS_BLOCK), has(Items.MOSS_BLOCK))
                .save(output);
    }
}
