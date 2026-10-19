package com.apeira.archae.datagen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.apeira.archae.assembly.AssemblyRecipe;
import com.apeira.archae.assembly.AssemblyRecipes;
import com.apeira.archae.registry.ArchaeArtifacts;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

/**
 * Writes an assembly table recipe ({@code data/apeira/assembly/<artifact>.json}) for every artifact made from a
 * fragment, as Argon's {@code ModAssemblyRecipeProvider} did: {@code cost} of the fragment make the artifact.
 */
public final class AssemblyRecipeProvider implements DataProvider {
    private final PackOutput.PathProvider paths;

    public AssemblyRecipeProvider(PackOutput output) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, AssemblyRecipes.DIRECTORY);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        Map<Identifier, AssemblyRecipe> recipes = new LinkedHashMap<>();
        for (ArchaeArtifacts.Artifact artifact : ArchaeArtifacts.all()) {
            if (artifact.fragment() != null) {
                recipes.put(artifact.block().getId(), new AssemblyRecipe(artifact.fragment().get(), artifact.item().get(), artifact.cost()));
            }
        }
        return DataProvider.saveAll(output, AssemblyRecipe.CODEC, paths, recipes);
    }

    @Override
    public String getName() {
        return "Assembly Recipes";
    }
}
