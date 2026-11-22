package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.toolskin.ToolSkinRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Recipe serializers of the decor module. */
public final class DecorRecipes {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** {@code apeira:smithing_tool_skin}: putting a tool skin on a tool in the smithing table. */
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolSkinRecipe>> TOOL_SKIN = R
            .forRegistry(Registries.RECIPE_SERIALIZER).register("smithing_tool_skin", () -> ToolSkinRecipe.SERIALIZER);

    private DecorRecipes() {}

    public static void init(ApeiraRegistrar registrar) {}
}
