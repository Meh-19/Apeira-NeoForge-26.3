package com.apeira.main.datagen;

import java.util.List;
import java.util.stream.Stream;

import com.apeira.core.datagen.ApeiraBlockTagsProvider;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Block tags for {@link MainBlocks}, {@link MainWoods}, {@link MainStones}, {@link MainLanterns}, Xenon's blocks and
 * {@link MainCrates}:
 * which tool mines each block and the tool tier it needs to drop, and the wood tags of {@link MainBlockItemTags},
 * through which axes mine the woods (and hoes the leaves).
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

        // ORForged's other blocks, tagged as ORForged tagged them: the mineral bricks and pillars and the stone variants
        // with a pickaxe, packed dirt and sands and silt with a shovel, the lanterns with an axe. ORForged left some stone
        // variants untagged, so nothing mined them; they are mined with a pickaxe like the rest.
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            if (mineral.brick() != MainBlocks.DIAMOND_BRICK) {
                add(BlockTags.MINEABLE_WITH_PICKAXE, mineral.brick());
            }
            add(BlockTags.MINEABLE_WITH_PICKAXE, mineral.pillar());
        }
        add(BlockTags.MINEABLE_WITH_PICKAXE, MainStones.all().stream().filter(stone -> !MainStones.soils().contains(stone))
                .toArray(DeferredBlock<?>[]::new));
        add(BlockTags.MINEABLE_WITH_SHOVEL, MainStones.soils().toArray(DeferredBlock<?>[]::new));
        add(BlockTags.NEEDS_STONE_TOOL, MainStones.ANDESITE_BRICKS, MainStones.CHISELED_ANDESITE, MainStones.SLATE, MainStones.SLATE_BRICKS,
                MainStones.CHISELED_SLATE, MainStones.POLISHED_SLATE, MainStones.DRIED_SILT, MainStones.SILT_BRICKS,
                MainStones.CHISELED_SILT_BRICKS, MainStones.POLISHED_SILT, MainStones.SOUL_BRICKS, MainStones.POLISHED_SOUL_SAND,
                MainStones.CHISELED_SOUL_BRICKS, MainStones.POSSESSED_CHISELED_SOUL_BRICKS);
        add(BlockTags.MINEABLE_WITH_AXE, MainLanterns.all().stream().map(MainLanterns.Lantern::block).toArray(DeferredBlock<?>[]::new));

        // Xenon's blocks, tagged as Xenon tagged them: its stones, shingles, concrete bricks, crystals and bone blocks need
        // a stone pickaxe; its soils and moss blocks a shovel; its wooden blocks an axe. Xenon left its crop crates,
        // baskets and bundles untagged, so nothing mined them; they are mined with an axe like its other crates.
        List<DeferredBlock<?>> soils = Stream.concat(MainRocks.soils().stream(), Stream.of(MainRocks.PACKED_DIRT_SHAPES.slab())).toList();
        DeferredBlock<?>[] stones = Stream.of(MainRocks.all().stream().filter(rock -> !soils.contains(rock)),
                MainDyedBlocks.CONCRETE_BRICKS.stream().flatMap(MainBlockTagProvider::withShapes),
                Stream.<DeferredBlock<?>>of(MainDyedBlocks.TERRACOTTA_SHINGLES), MainDyedBlocks.TERRACOTTA_SHINGLES_SHAPES.shapes().stream(),
                MainDyedBlocks.TERRACOTTA_SHINGLES_COLOURS.stream().flatMap(MainBlockTagProvider::withShapes),
                MainCrystals.all().stream(), MainBuildingBlocks.bones().stream()).flatMap(s -> s).toArray(DeferredBlock<?>[]::new);
        add(BlockTags.MINEABLE_WITH_PICKAXE, stones);
        add(BlockTags.NEEDS_STONE_TOOL, stones);
        add(BlockTags.MINEABLE_WITH_PICKAXE, MainBuildingBlocks.BLIGHTED_SHROOMLIGHT);
        add(BlockTags.MINEABLE_WITH_SHOVEL, soils.toArray(DeferredBlock<?>[]::new));
        add(BlockTags.MINEABLE_WITH_SHOVEL, MainBuildingBlocks.DAMP_MOSS_BLOCK, MainBuildingBlocks.GRIMOSS_BLOCK,
                MainBuildingBlocks.GRIMWEED_BLOCK, MainBuildingBlocks.PALE_GRIMWEED_BLOCK);
        add(BlockTags.MINEABLE_WITH_AXE, Stream.of(MainBuildingBlocks.woodenCrates().stream(), MainBuildingBlocks.cropCrates().stream(),
                MainBuildingBlocks.bundles().stream(), Stream.of(MainBuildingBlocks.SHOJI, MainBuildingBlocks.SALVAGING_TABLE))
                .flatMap(s -> s).toArray(DeferredBlock<?>[]::new));
        add(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP, MainRocks.POROUS_DOLOMITE);
    }

    private static Stream<DeferredBlock<?>> withShapes(MainDyedBlocks.Dyed dyed) {
        return Stream.concat(Stream.of(dyed.block()), dyed.shapes().shapes().stream());
    }
}
