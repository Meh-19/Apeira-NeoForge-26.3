package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraRecipeProvider;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorItems;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/** Recipes for the decor module, CosmoLib's for the painting table and paintbrush. */
public final class DecorRecipeProvider extends ApeiraRecipeProvider {
    public DecorRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace) {
        super(recipes, advancements, namespace);
    }

    @Override
    protected void buildRecipes() {
        shaped(RecipeCategory.DECORATIONS, DecorBlocks.PAINTING_TABLE)
                .pattern("WW")
                .pattern("PP")
                .pattern("PP")
                .define('W', Items.WOOL.white())
                .define('P', ItemTags.PLANKS)
                .unlockedBy(getHasName(Items.WOOL.white()), has(Items.WOOL.white()))
                .save(output);
        shaped(RecipeCategory.TOOLS, DecorItems.PAINTBRUSH)
                .pattern("F")
                .pattern("I")
                .pattern("S")
                .define('F', Items.FEATHER)
                .define('I', Items.COPPER_INGOT)
                .define('S', Items.STICK)
                .unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
                .save(output);
    }
}
