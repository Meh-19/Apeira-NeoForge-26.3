package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.data.loot.LootTableSubProvider;

/**
 * Loot tables for {@link MainBlocks}, {@link MainWoods}, {@link MainStones}, {@link MainLanterns} and {@link MainCrates}. Every Apeira block needs one, or data
 * generation fails.
 */
public final class MainBlockLootSubProvider extends ApeiraBlockLootProvider {
    public MainBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(MainBlocks.PLATINUM_BLOCK.get());
        dropSelf(MainBlocks.PLATINUM_BRICKS.get());
        dropSelf(MainBlocks.PLATINUM_PILLAR.get());
        dropSelf(MainBlocks.RUBY_BLOCK.get());
        dropSelf(MainBlocks.RUBY_BRICKS.get());
        dropSelf(MainBlocks.RUBY_PILLAR.get());
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            dropSelf(mineral.brick().get());
            dropSelf(mineral.pillar().get());
        }
        // Ores drop their material, more with Fortune, or themselves with Silk Touch.
        add(MainBlocks.PLATINUM_ORE.get(), createOreDrop(MainBlocks.PLATINUM_ORE.get(), MainItems.PLATINUM.get()));
        add(MainBlocks.DEEPSLATE_PLATINUM_ORE.get(), createOreDrop(MainBlocks.DEEPSLATE_PLATINUM_ORE.get(), MainItems.PLATINUM.get()));
        add(MainBlocks.RUBY_ORE.get(), createOreDrop(MainBlocks.RUBY_ORE.get(), MainItems.RUBY.get()));
        add(MainBlocks.DEEPSLATE_RUBY_ORE.get(), createOreDrop(MainBlocks.DEEPSLATE_RUBY_ORE.get(), MainItems.RUBY.get()));

        for (MainWoods.Wood wood : MainWoods.woods()) {
            wood.logBlocks().forEach(log -> dropSelf(log.get()));
            planks(wood.planks());
            dropSelf(wood.verticalPlanks().get());
        }
        // ORForged's trees have no saplings: their leaves and wisteria's flowers drop sticks, or themselves with shears.
        MainWoods.allLeaves().forEach(leaves -> add(leaves.get(), createLeavesDropsWithoutSapling(leaves.get())));
        dropSelf(MainWoods.PALM_CROWN.get());
        MainWoods.fungalPlankSets().forEach(this::planks);
        dropSelf(MainWoods.VERTICAL_FUNGAL_PLANKS.get());
        MainWoods.carvedPlanks().forEach(carved -> dropSelf(carved.get()));
        MainWoods.beams().forEach(beam -> dropSelf(beam.get()));
        MainWoods.VANILLA_VERTICAL_PLANKS.forEach(vertical -> dropSelf(vertical.vertical().get()));
        MainStones.all().forEach(stone -> dropSelf(stone.get()));
        MainLanterns.all().forEach(lantern -> dropSelf(lantern.block().get()));

        // A closed crate drops itself; an open one cannot be broken.
        MainCrates.all().forEach(crate -> dropSelf(crate.get()));
    }

    /** Planks and their shapes drop themselves; a double slab drops two slabs. */
    private void planks(PlankSet planks) {
        planks.all().forEach(block -> {
            if (block == planks.slab()) {
                add(block.get(), createSlabItemTable(block.get()));
            } else {
                dropSelf(block.get());
            }
        });
    }
}
