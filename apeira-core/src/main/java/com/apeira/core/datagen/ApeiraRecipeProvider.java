package com.apeira.core.datagen;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

/**
 * Base for one module's recipes.
 *
 * <p>Recipes are datapack registry entries in Minecraft 26.x: a recipe provider is no longer a data provider but a
 * bootstrap for the {@code minecraft:recipe} and {@code minecraft:advancement} registries, run from
 * {@code GatherDataRegistryEntriesEvent}. Register subclasses with {@link ApeiraDataGenerators#recipes}; their
 * constructor takes the two bootstrap contexts and the module's namespace, so a method reference works:
 *
 * <pre>{@code
 * public final class DecorRecipes extends ApeiraRecipeProvider {
 *     public DecorRecipes(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace) {
 *         super(recipes, advancements, namespace);
 *     }
 *
 *     @Override
 *     protected void buildRecipes() {
 *         shaped(RecipeCategory.DECORATIONS, DecorBlocks.OAK_CHAIR)
 *                 .pattern("P  ").pattern("PPP").pattern("S S")
 *                 .define('P', Items.OAK_PLANKS).define('S', Items.STICK)
 *                 .unlockedBy(getHasName(Items.OAK_PLANKS), has(Items.OAK_PLANKS))
 *                 .save(output);
 *     }
 * }
 * }</pre>
 *
 * <p>Saving with {@code save(output)} names a recipe after its result. Vanilla's {@code save(output, String)} and its
 * storage-block helpers parse bare names into the {@code minecraft} namespace, so use {@link #save(RecipeBuilder, String)}
 * and the helpers here for any other name.
 */
public abstract class ApeiraRecipeProvider extends RecipeProvider {
    private final String namespace;

    protected ApeiraRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace) {
        super(recipes, advancements);
        this.namespace = namespace;
    }

    public String namespace() {
        return namespace;
    }

    /** Returns the recipe key {@code <namespace>:<path>}. */
    protected ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, ApeiraIds.id(namespace, path));
    }

    /** Saves {@code builder} as {@code <namespace>:<path>}. */
    protected void save(RecipeBuilder builder, String path) {
        builder.save(output, key(path));
    }

    /** Smelts {@code input} into {@code result}, saved as {@code <result>_from_smelting_<input>}. */
    protected void smelting(ItemLike input, RecipeCategory category, CookingBookCategory book, ItemLike result, float experience,
            int cookingTime) {
        save(SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), category, book, result, experience, cookingTime)
                .unlockedBy(getHasName(input), has(input)), getItemName(result) + "_from_smelting_" + getItemName(input));
    }

    /** Blasts {@code input} into {@code result}, saved as {@code <result>_from_blasting_<input>}. */
    protected void blasting(ItemLike input, RecipeCategory category, CookingBookCategory book, ItemLike result, float experience,
            int cookingTime) {
        save(SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), category, book, result, experience, cookingTime)
                .unlockedBy(getHasName(input), has(input)), getItemName(result) + "_from_blasting_" + getItemName(input));
    }

    /** Smelts {@code input} in a furnace, and in a blast furnace in half the time, like vanilla ores. */
    protected void smeltingAndBlasting(ItemLike input, RecipeCategory category, ItemLike result, float experience, int cookingTime) {
        smelting(input, category, CookingBookCategory.MISC, result, experience, cookingTime);
        blasting(input, category, CookingBookCategory.MISC, result, experience, cookingTime / 2);
    }

    /**
     * Cooks food in a furnace ({@code cookingTime}), smoker (half) and on a campfire (three times), like vanilla
     * meat. Saved as {@code <result>}, {@code <result>_from_smoking} and {@code <result>_from_campfire_cooking}.
     */
    protected void food(ItemLike input, ItemLike result, float experience, int cookingTime) {
        Ingredient ingredient = Ingredient.of(input);
        String name = getItemName(result);
        save(SimpleCookingRecipeBuilder.smelting(ingredient, RecipeCategory.FOOD, CookingBookCategory.FOOD, result, experience, cookingTime)
                .unlockedBy(getHasName(input), has(input)), name);
        save(SimpleCookingRecipeBuilder.smoking(ingredient, RecipeCategory.FOOD, result, experience, cookingTime / 2)
                .unlockedBy(getHasName(input), has(input)), name + "_from_smoking");
        save(SimpleCookingRecipeBuilder.campfireCooking(ingredient, RecipeCategory.FOOD, result, experience, cookingTime * 3)
                .unlockedBy(getHasName(input), has(input)), name + "_from_campfire_cooking");
    }

    /** Cuts {@code count} of {@code result} from one {@code input} in a stonecutter. */
    protected void stonecutting(RecipeCategory category, ItemLike result, ItemLike input, int count) {
        save(SingleItemRecipeBuilder.stonecutting(Ingredient.of(input), category, result, count)
                .unlockedBy(getHasName(input), has(input)), getItemName(result) + "_from_" + getItemName(input) + "_stonecutting");
    }

    /**
     * Packs nine {@code unpacked} into one {@code packed} and back, saved as {@code <packed>} and
     * {@code <unpacked>_from_<packed>}.
     */
    protected void storageBlock(RecipeCategory unpackedCategory, ItemLike unpacked, RecipeCategory packedCategory, ItemLike packed) {
        save(shapeless(unpackedCategory, unpacked, 9).requires(packed).unlockedBy(getHasName(packed), has(packed)),
                getItemName(unpacked) + "_from_" + getItemName(packed));
        save(shaped(packedCategory, packed).define('#', unpacked).pattern("###").pattern("###").pattern("###")
                .unlockedBy(getHasName(unpacked), has(unpacked)), getItemName(packed));
    }
}
