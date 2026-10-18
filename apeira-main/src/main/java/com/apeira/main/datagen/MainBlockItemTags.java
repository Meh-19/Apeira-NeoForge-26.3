package com.apeira.main.datagen;

import com.apeira.core.datagen.BlockAndItemTags;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainWoods;

import net.minecraft.tags.BlockItemTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The wood tags shared by Apeira's blocks and their items, written by both {@link MainBlockTagProvider} and
 * {@link MainItemTagProvider}. Vanilla's tags make the woods behave like vanilla wood: axes mine them, logs keep
 * leaves from decaying, fences connect, and recipes asking for any planks or logs accept them.
 *
 * <p>As in ORForged, beams and vertical and carved planks count as planks, and the palm crown as a log.
 */
final class MainBlockItemTags {
    private MainBlockItemTags() {}

    static void addTo(BlockAndItemTags tags) {
        for (MainWoods.Wood wood : MainWoods.woods()) {
            tags.add(wood.logs(), wood.logBlocks().toArray(DeferredBlock<?>[]::new));
            tags.addTag(BlockItemTags.LOGS_THAT_BURN, wood.logs());
            tags.add(BlockItemTags.PLANKS, wood.planks().planks(), wood.verticalPlanks());
            tags.add(BlockItemTags.LEAVES, wood.leaves().toArray(DeferredBlock<?>[]::new));
            shapes(tags, wood.planks());
        }
        tags.add(BlockItemTags.LOGS_THAT_BURN, MainWoods.PALM_CROWN);
        tags.add(BlockItemTags.LEAVES, MainWoods.WISTERIA_FLOWERS.toArray(DeferredBlock<?>[]::new));
        for (PlankSet planks : MainWoods.fungalPlankSets()) {
            tags.add(BlockItemTags.PLANKS, planks.planks());
            shapes(tags, planks);
        }
        tags.add(BlockItemTags.PLANKS, MainWoods.VERTICAL_FUNGAL_PLANKS);
        tags.add(BlockItemTags.PLANKS, MainWoods.carvedPlanks().toArray(DeferredBlock<?>[]::new));
        tags.add(BlockItemTags.PLANKS, MainWoods.beams().toArray(DeferredBlock<?>[]::new));
    }

    private static void shapes(BlockAndItemTags tags, PlankSet planks) {
        tags.add(BlockItemTags.WOODEN_STAIRS, planks.stairs());
        tags.add(BlockItemTags.WOODEN_SLABS, planks.slab());
        tags.add(BlockItemTags.WOODEN_BUTTONS, planks.button());
        tags.add(BlockItemTags.WOODEN_PRESSURE_PLATES, planks.pressurePlate());
        tags.add(BlockItemTags.WOODEN_FENCES, planks.fence());
        tags.add(BlockItemTags.FENCE_GATES, planks.fenceGate());
    }
}
