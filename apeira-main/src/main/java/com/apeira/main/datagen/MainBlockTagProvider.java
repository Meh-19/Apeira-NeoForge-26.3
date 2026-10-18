package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainWoods;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Block tags for {@link MainBlocks}, {@link MainWoods} and {@link MainCrates}: which tool mines each block and the
 * tool tier it needs to drop, and the wood tags of {@link MainBlockItemTags}, through which axes mine the woods (and
 * hoes the leaves).
 */
public final class MainBlockTagProvider extends ApeiraBlockTagsProvider {
    public MainBlockTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // ORForged's mineral blocks: mined with a pickaxe; ores need stone, deepslate ores iron.
        add(BlockTags.MINEABLE_WITH_PICKAXE, MainBlocks.PLATINUM_BLOCK, MainBlocks.PLATINUM_BRICKS, MainBlocks.PLATINUM_PILLAR,
                MainBlocks.PLATINUM_ORE, MainBlocks.DEEPSLATE_PLATINUM_ORE, MainBlocks.RUBY_BLOCK, MainBlocks.RUBY_BRICKS,
                MainBlocks.RUBY_PILLAR, MainBlocks.RUBY_ORE, MainBlocks.DEEPSLATE_RUBY_ORE, MainBlocks.DIAMOND_BRICK);
        add(BlockTags.NEEDS_STONE_TOOL, MainBlocks.PLATINUM_ORE, MainBlocks.RUBY_ORE);
        add(BlockTags.NEEDS_IRON_TOOL, MainBlocks.DEEPSLATE_PLATINUM_ORE, MainBlocks.DEEPSLATE_RUBY_ORE);
        blockItemTags(MainBlockItemTags::addTo);
        // Crates are wooden: mined with an axe.
        add(BlockTags.MINEABLE_WITH_AXE, MainCrates.all().toArray(DeferredBlock<?>[]::new));
    }
}
