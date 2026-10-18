package com.apeira.main.datagen;

import java.util.List;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.datagen.ApeiraDataMapProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainWoods;

import net.minecraft.core.HolderLookup;

/**
 * NeoForge data maps for {@link MainWoods}: axes strip logs and woods ({@code neoforge:transformables}), and wood and
 * leaves burn ({@code neoforge:flammable}) with ORForged's odds, which are vanilla's: logs, woods and beams 5 and 5,
 * planks 5 and 20, leaves 30 and 60. Stairs, slabs, fences and fence gates burn like vanilla's, 5 and 20.
 */
public final class MainDataMapProvider extends ApeiraDataMapProvider {
    public MainDataMapProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void gather(HolderLookup.Provider registries) {
        for (MainWoods.Wood wood : MainWoods.woods()) {
            strippable(wood.log().get(), wood.strippedLog().get());
            strippable(wood.wood().get(), wood.strippedWood().get());
            wood.logBlocks().forEach(log -> flammable(log.get(), 5, 5));
            planks(wood.planks());
            flammable(wood.verticalPlanks().get(), 5, 20);
            wood.leaves().forEach(leaves -> flammable(leaves.get(), 30, 60));
        }
        flammable(MainWoods.PALM_CROWN.get(), 5, 20);
        MainWoods.WISTERIA_FLOWERS.forEach(flowers -> flammable(flowers.get(), 30, 60));
        MainWoods.fungalPlankSets().forEach(this::planks);
        flammable(MainWoods.VERTICAL_FUNGAL_PLANKS.get(), 5, 20);
        MainWoods.carvedPlanks().forEach(carved -> flammable(carved.get(), 5, 20));
        MainWoods.beams().forEach(beam -> flammable(beam.get(), 5, 5));
        MainWoods.VANILLA_VERTICAL_PLANKS.forEach(vertical -> flammable(vertical.vertical().get(), 5, 20));
        // Xenon had a stripped mycena stem but never registered the stripping; axes strip it here so it can be had.
        strippable(MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get(), MainSpecialBlocks.STRIPPED_MYCENA_MUSHROOM_STEM.get());
    }

    private void planks(PlankSet planks) {
        for (var block : List.of(planks.planks(), planks.stairs(), planks.slab(), planks.fence(), planks.fenceGate())) {
            flammable(block.get(), 5, 20);
        }
    }
}
