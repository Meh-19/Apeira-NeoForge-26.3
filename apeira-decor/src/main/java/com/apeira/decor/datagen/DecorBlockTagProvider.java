package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.decor.registry.DecorBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;

/** Block tags for {@link DecorBlocks}: which tool mines each block. */
public final class DecorBlockTagProvider extends ApeiraBlockTagsProvider {
    public DecorBlockTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Wooden furniture: mined fastest with an axe, like planks, and drops with any tool or none.
        add(BlockTags.MINEABLE_WITH_AXE, DecorBlocks.OAK_CHAIR);
    }
}
