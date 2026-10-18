package com.apeira.main.datagen;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Block states and block models for {@link MainBlocks}, {@link MainWoods}, {@link MainStones}, {@link MainLanterns} and
 * {@link MainCrates}. Block items use their block's model, except buttons and fences, which use vanilla's inventory
 * models.
 *
 * <p>Textures are ORForged's or, for the blocks it had none for, the Origin Realms server pack's: {@code block/<name>},
 * with {@code block/<name>_top} for the ends of pillars, logs, beams, lanterns, chiseled blocks and slate. A wood's log
 * texture is also its wood's, and carved planks show their carving on every side.
 */
public final class MainBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        simpleBlock(MainBlocks.PLATINUM_BLOCK.get());
        simpleBlock(MainBlocks.PLATINUM_BRICKS.get());
        pillar(MainBlocks.PLATINUM_PILLAR.get());
        MainBlocks.ores().forEach(ore -> simpleBlock(ore.get()));
        simpleBlock(MainBlocks.RUBY_BLOCK.get());
        simpleBlock(MainBlocks.RUBY_BRICKS.get());
        pillar(MainBlocks.RUBY_PILLAR.get());
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            simpleBlock(mineral.brick().get());
            pillar(mineral.pillar().get());
        }

        for (MainWoods.Wood wood : MainWoods.woods()) {
            log(wood.log().get(), wood.wood().get());
            log(wood.strippedLog().get(), wood.strippedWood().get());
            family(wood.planks().family());
            simpleBlock(wood.verticalPlanks().get());
            wood.leaves().forEach(leaves -> leaves(leaves.get()));
        }
        simpleBlock(MainWoods.PALM_CROWN.get());
        MainWoods.WISTERIA_FLOWERS.forEach(flowers -> leaves(flowers.get()));
        MainWoods.fungalPlankSets().stream().map(PlankSet::family).forEach(this::family);
        simpleBlock(MainWoods.VERTICAL_FUNGAL_PLANKS.get());
        MainWoods.carvedPlanks().forEach(carved -> simpleBlock(carved.get()));
        MainWoods.beams().forEach(beam -> pillar(beam.get()));
        // The server pack's vertical planks: the vanilla planks texture, turned on its side by the hand-made parent.
        ModelTemplate verticalPlanks = new ModelTemplate(Optional.of(modLoc("block/vertical_planks")), Optional.empty(), TextureSlot.ALL);
        for (MainWoods.VanillaVerticalPlanks vertical : MainWoods.VANILLA_VERTICAL_PLANKS) {
            simpleBlock(vertical.vertical().get(), TexturedModel.createDefault(block -> TextureMapping.cube(vertical.planks()), verticalPlanks));
        }

        for (var stone : MainStones.all()) {
            if (MainStones.columns().contains(stone)) {
                simpleBlock(stone.get(), TexturedModel.COLUMN_ALT);
            } else {
                simpleBlock(stone.get());
            }
        }
        MainLanterns.all().forEach(lantern -> simpleBlock(lantern.block().get(), TexturedModel.COLUMN_ALT));

        xenon();

        // Crates are drawn by their block entity, whatever their rotation; the hand-made block model only gives the
        // breaking particles, and the item uses the crate's hand-made item model.
        for (var crate : MainCrates.all()) {
            Block block = crate.get();
            existingModelBlock(block);
            blockItemModel(block, modLoc("item/" + crate.getId().getPath()));
        }
    }

    /**
     * Xenon's blocks ({@link MainRocks}, {@link MainDyedBlocks}, {@link MainCrystals} and {@link MainBuildingBlocks}),
     * with Xenon's textures under Apeira's names: {@code block/<name>}, with {@code block/<name>_top} ends.
     */
    private void xenon() {
        Set<Block> shapes = new HashSet<>();
        MainRocks.shapes().forEach(set -> set.shapes().forEach(shape -> shapes.add(shape.get())));
        Set<Block> ownModels = Set.of(MainRocks.ASHEN_SAND.get(), MainRocks.ASHEN_SANDSTONE.get(), MainRocks.SMOOTH_ASHEN_SANDSTONE.get(),
                MainRocks.CHISELED_ASHEN_SANDSTONE.get(), MainRocks.CUT_ASHEN_SANDSTONE.get(), MainRocks.PERMAFROST.get());
        for (var rock : MainRocks.all()) {
            Block block = rock.get();
            if (MainRocks.pillars().contains(rock)) {
                pillar(block);
            } else if (MainRocks.columns().contains(rock)) {
                simpleBlock(block, TexturedModel.COLUMN_ALT);
            } else if (!shapes.contains(block) && !ownModels.contains(block)) {
                simpleBlock(block);
            }
        }
        simpleBlock(MainRocks.ASHEN_SAND.get());
        // Sandstone-like: ashen sandstone has its top texture on top; smooth, chiseled and cut ashen sandstone have it on
        // both ends, and smooth ashen sandstone all round, as in Xenon.
        Material ashenTop = blockTexture("ashen_sandstone_top");
        bottomTop(MainRocks.ASHEN_SANDSTONE.get(), blockTexture("ashen_sandstone"), ashenTop, blockTexture("ashen_sandstone"));
        simpleBlock(MainRocks.SMOOTH_ASHEN_SANDSTONE.get(), TexturedModel.createDefault(block -> TextureMapping.cube(ashenTop),
                ModelTemplates.CUBE_ALL));
        bottomTop(MainRocks.CHISELED_ASHEN_SANDSTONE.get(), blockTexture("chiseled_ashen_sandstone"), ashenTop, ashenTop);
        bottomTop(MainRocks.CUT_ASHEN_SANDSTONE.get(), blockTexture("cut_ashen_sandstone"), ashenTop, ashenTop);
        bottomTop(MainRocks.PERMAFROST.get(), blockTexture("permafrost"), blockTexture("permafrost_top"),
                TextureMapping.getBlockTexture(Blocks.DIRT));
        MainRocks.shapes().forEach(this::shapes);

        for (MainDyedBlocks.Dyed dyed : MainDyedBlocks.CONCRETE_BRICKS) {
            simpleBlock(dyed.block().get());
        }
        simpleBlock(MainDyedBlocks.TERRACOTTA_SHINGLES.get());
        MainDyedBlocks.TERRACOTTA_SHINGLES_COLOURS.forEach(dyed -> simpleBlock(dyed.block().get()));
        MainDyedBlocks.QUILTED_WOOL.forEach(dyed -> simpleBlock(dyed.block().get()));
        MainDyedBlocks.shapes().forEach(this::shapes);

        for (MainCrystals.Crystal crystal : MainCrystals.CRYSTALS) {
            Block cluster = crystal.cluster().get();
            generators().createAmethystCluster(cluster);
            generators().registerSimpleItemModel(cluster.asItem(), generators().createFlatItemModelWithBlockTexture(cluster.asItem(), cluster));
            simpleBlock(crystal.block().get(), TexturedModel.COLUMN_ALT);
            simpleBlock(crystal.bricks().get());
            simpleBlock(crystal.chiseledBricks().get(), TexturedModel.COLUMN_ALT);
            simpleBlock(crystal.lantern().get());
        }
        // The prism lantern blinks while lit.
        Block prism = MainCrystals.PRISM_LANTERN.get();
        Identifier unlit = ModelTemplates.CUBE_ALL.create(prism, TextureMapping.cube(prism), generators().modelOutput);
        Identifier lit = ModelTemplates.CUBE_ALL.createWithSuffix(prism, "_lit", TextureMapping.cube(TextureMapping.getBlockTexture(prism, "_lit")),
                generators().modelOutput);
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(prism)
                .with(BlockModelGenerators.createBooleanModelDispatch(BlockStateProperties.LIT, variant(lit), variant(unlit))));

        simpleBlock(MainBuildingBlocks.DAMP_MOSS_BLOCK.get(), TexturedModel.COLUMN_ALT);
        simpleBlock(MainBuildingBlocks.GRIMOSS_BLOCK.get(), TexturedModel.COLUMN_ALT);
        for (var grimweed : List.of(MainBuildingBlocks.GRIMWEED_BLOCK, MainBuildingBlocks.PALE_GRIMWEED_BLOCK)) {
            bottomTop(grimweed.get(), TextureMapping.getBlockTexture(grimweed.get()), TextureMapping.getBlockTexture(grimweed.get(), "_top"),
                    TextureMapping.getBlockTexture(MainRocks.LOAM.get()));
        }
        simpleBlock(MainBuildingBlocks.BLIGHTED_SHROOMLIGHT.get());
        // Carved blighted and scorched bone show another carving when lying down.
        for (var bone : MainBuildingBlocks.bones()) {
            Block block = bone.get();
            if (block == MainBuildingBlocks.CARVED_BLIGHTED_BONE_BLOCK.get() || block == MainBuildingBlocks.CARVED_SCORCHED_BONE_BLOCK.get()) {
                generators().createRotatedPillarWithHorizontalVariant(block, TexturedModel.COLUMN_ALT, TexturedModel.createDefault(
                        b -> TextureMapping.column(TextureMapping.getBlockTexture(b, "_horizontal"), TextureMapping.getBlockTexture(b, "_top")),
                        ModelTemplates.CUBE_COLUMN_HORIZONTAL));
            } else {
                pillar(block);
            }
        }
        MainBuildingBlocks.woodenCrates().forEach(crate -> simpleBlock(crate.get()));
        MainBuildingBlocks.cropCrates().forEach(crate -> simpleBlock(crate.get(), TexturedModel.COLUMN_ALT));
        MainBuildingBlocks.bundles().forEach(bundle -> pillar(bundle.get()));
        simpleBlock(MainBuildingBlocks.SHOJI.get(), TexturedModel.COLUMN_ALT);
        // Xenon's salvaging table has no facing: its front is always to the north.
        simpleBlock(MainBuildingBlocks.SALVAGING_TABLE.get(), TexturedModel.ORIENTABLE);
    }

    /** A cube with {@code side}, {@code top} and {@code bottom} textures, like sandstone. */
    private void bottomTop(Block block, Material side, Material top, Material bottom) {
        simpleBlock(block, TexturedModel.createDefault(b -> new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.TOP, top)
                .put(TextureSlot.BOTTOM, bottom), ModelTemplates.CUBE_BOTTOM_TOP));
    }
}
