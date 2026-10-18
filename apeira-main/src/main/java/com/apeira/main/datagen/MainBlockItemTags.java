package com.apeira.main.datagen;

import java.util.stream.Stream;

import com.apeira.core.datagen.BlockAndItemTags;
import com.apeira.core.registry.PlankSet;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainWoods;

import net.minecraft.tags.BlockItemTags;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The tags shared by Apeira's blocks and their items (the wood tags, and Xenon's in {@link #addXenon}), written by both {@link MainBlockTagProvider} and
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
        tags.add(BlockItemTags.PLANKS, MainWoods.VANILLA_VERTICAL_PLANKS.stream().map(MainWoods.VanillaVerticalPlanks::vertical)
                .toArray(DeferredBlock<?>[]::new));
        addXenon(tags);
    }

    /**
     * Xenon's tags: stairs, slabs and walls in vanilla's shape tags, quilted wool as wool, and grimweed blocks as dirt,
     * on which plants grow.
     */
    static void addXenon(BlockAndItemTags tags) {
        for (ShapeSet shapes : Stream.concat(MainRocks.shapes().stream(), MainDyedBlocks.shapes().stream()).toList()) {
            if (shapes.stairs() != null) {
                tags.add(BlockItemTags.STAIRS, shapes.stairs());
            }
            if (shapes.slab() != null) {
                tags.add(BlockItemTags.SLABS, shapes.slab());
            }
            if (shapes.wall() != null) {
                tags.add(BlockItemTags.WALLS, shapes.wall());
            }
        }
        tags.add(BlockItemTags.WOOL, MainDyedBlocks.QUILTED_WOOL.stream().map(MainDyedBlocks.Dyed::block).toArray(DeferredBlock<?>[]::new));
        tags.add(BlockItemTags.DIRT, MainBuildingBlocks.GRIMWEED_BLOCK, MainBuildingBlocks.PALE_GRIMWEED_BLOCK);
        addXenonPlants(tags);
    }

    /**
     * Xenon's flower tags (it called most of its grasses flowers, so bees visit them), yellow birch leaves as leaves and
     * the mycena stems as logs that burn. Xenon also had the stick in its flower tag; it is left out.
     */
    private static void addXenonPlants(BlockAndItemTags tags) {
        tags.add(BlockItemTags.FLOWERS, Stream.concat(MainPlants.grasses().stream(), Stream.of(MainPlants.CATTAIL, MainPlants.OCULUS_ORCHID,
                MainPlants.HIBISCUS, MainPlants.CLOVER, MainPlants.SMALL_LILY, MainPlants.ORANGE_MYCENA)).toArray(DeferredBlock<?>[]::new));
        tags.add(BlockItemTags.SMALL_FLOWERS, MainPlants.HOGWEED, MainPlants.FROSTED_GRASS, MainPlants.DRY_GRASS, MainPlants.HIBISCUS,
                MainPlants.OCULUS_ORCHID, MainPlants.CLOVER, MainPlants.GRIMWEED, MainPlants.DRY_BUSH, MainPlants.SOUL_STALKS,
                MainPlants.BEACH_GRASS, MainPlants.BRAMBLE);
        tags.add(BlockItemTags.LEAVES, MainSpecialBlocks.YELLOW_BIRCH_LEAVES);
        tags.add(BlockItemTags.LOGS_THAT_BURN, MainSpecialBlocks.MYCENA_MUSHROOM_STEM, MainSpecialBlocks.STRIPPED_MYCENA_MUSHROOM_STEM);
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
