package com.apeira.decor.toolskin;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.apeira.decor.registry.DecorRecipes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * {@code apeira:smithing_tool_skin}: the vanilla smithing table puts a tool skin on a tool, as on Origin Realms (template slot
 * the skin, base slot the tool, the addition slot empty, the skinned tool in the result). {@code template} and {@code base}
 * only say what the table's slots accept ({@code #apeira:tool_skins} and {@code #apeira:tool_skin/skinnable}); a pair
 * matches when the skin fits the tool's group and the tool has no skin yet ({@link ToolSkins#canApply}). Taking the result
 * uses up the skin and the tool as any smithing recipe does.
 */
public class ToolSkinRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<ToolSkinRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base))
            .apply(i, ToolSkinRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSkinRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
            ToolSkinRecipe::new);
    public static final RecipeSerializer<ToolSkinRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient template;
    private final Ingredient base;

    public ToolSkinRecipe(Recipe.CommonInfo commonInfo, Ingredient template, Ingredient base) {
        super(commonInfo);
        this.template = template;
        this.base = base;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return input.addition().isEmpty() && template.test(input.template()) && base.test(input.base())
                && ToolSkins.canApply(input.template(), input.base());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return ToolSkins.apply(input.template(), input.base());
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(template);
    }

    @Override
    public Ingredient baseIngredient() {
        return base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.empty();
    }

    @Override
    public RecipeSerializer<ToolSkinRecipe> getSerializer() {
        return DecorRecipes.TOOL_SKIN.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(List.of(template, base));
    }
}
