package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Block tags for {@link MainBlocks} and {@link MainCrates}: which tool mines each block and the tool tier it needs to drop. */
public final class MainBlockTagProvider extends ApeiraBlockTagsProvider {
    public MainBlockTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Stone-like blocks: mined with a pickaxe, stone tier or better.
        add(BlockTags.MINEABLE_WITH_PICKAXE, MainBlocks.CARVED_APEIRA_STONE, MainBlocks.APEIRA_BRICK);
        add(BlockTags.NEEDS_STONE_TOOL, MainBlocks.CARVED_APEIRA_STONE, MainBlocks.APEIRA_BRICK);
        // Crates are wooden: mined with an axe.
        add(BlockTags.MINEABLE_WITH_AXE, MainCrates.all().toArray(DeferredBlock<?>[]::new));
    }
}
