package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorFurniture;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Block tags for {@link DecorBlocks} and {@link DecorFurniture}: which tool mines each block. */
public final class DecorBlockTagProvider extends ApeiraBlockTagsProvider {
    public DecorBlockTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Furniture and the painting table: mined fastest with an axe, like planks, and drop with any tool or none.
        add(BlockTags.MINEABLE_WITH_AXE, DecorBlocks.PAINTING_TABLE);
        add(BlockTags.MINEABLE_WITH_AXE, DecorFurniture.all().toArray(DeferredBlock[]::new));
    }
}
