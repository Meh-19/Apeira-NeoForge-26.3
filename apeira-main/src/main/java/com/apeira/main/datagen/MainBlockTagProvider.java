package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.registry.MainBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;

/** Block tags for {@link MainBlocks}: which tool mines each block and the tool tier it needs to drop. */
public final class MainBlockTagProvider extends ApeiraBlockTagsProvider {
    public MainBlockTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Stone-like blocks: mined with a pickaxe, stone tier or better.
        add(BlockTags.MINEABLE_WITH_PICKAXE, MainBlocks.CARVED_APEIRA_STONE, MainBlocks.APEIRA_BRICK);
        add(BlockTags.NEEDS_STONE_TOOL, MainBlocks.CARVED_APEIRA_STONE, MainBlocks.APEIRA_BRICK);
    }
}
