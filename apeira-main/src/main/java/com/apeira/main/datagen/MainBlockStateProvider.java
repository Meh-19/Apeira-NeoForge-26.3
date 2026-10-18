package com.apeira.main.datagen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.block.DecorationBlock;
import com.apeira.main.block.PlacedStickBlock;
import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.block.SpikeTrapBlock;
import com.apeira.main.block.TurkeyMeatBlock;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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
        xenonPlants();

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

    /**
     * Xenon's plants ({@link MainPlants}) and special blocks ({@link MainSpecialBlocks}), drawn with Xenon's own models
     * (hand-made resources, {@code block/<name>}) and its block states; their items show Xenon's item models
     * ({@code item/<name>}).
     */
    private void xenonPlants() {
        List<Block> plain = new ArrayList<>();
        MainPlants.grasses().forEach(plant -> plain.add(plant.get()));
        plain.addAll(List.of(MainPlants.OCULUS_ORCHID.get(), MainPlants.HIBISCUS.get(), MainPlants.SMALL_LILY.get(), MainPlants.ORANGE_MYCENA.get(),
                MainPlants.TALL_ORANGE_MYCENA.get(), MainPlants.CLOVER.get(), MainPlants.ASH.get(), MainPlants.FROST.get(), MainPlants.DAMP_MOSS.get(),
                MainPlants.GRIMOSS.get(), MainSpecialBlocks.ECHO_GLASS.get(), MainSpecialBlocks.TURKEY_SKELETON.get()));
        for (Block block : plain) {
            existingModelBlock(block);
            flatXenonItem(block);
        }
        // Xenon's vine tips are drawn with its "_bottom"/"_end" models and the rest of the vine with the plain ones.
        vine(MainPlants.SCULK_VINES.get(), "sculk_vines_end", MainPlants.SCULK_VINES_END.get(), "sculk_vines");
        vine(MainPlants.WARPED_VEIL.get(), "warped_veil_bottom", MainPlants.WARPED_VEIL_BOTTOM.get(), "warped_veil");
        vine(MainPlants.GRIMOSS_HANG.get(), "grimoss_hang_bottom", MainPlants.GRIMOSS_HANG_BOTTOM.get(), "grimoss_hang");
        vine(MainPlants.DAMP_MOSS_HANG.get(), "damp_moss_hang_bottom", MainPlants.DAMP_MOSS_HANG_BOTTOM.get(), "damp_moss_hang");

        // The cattail's model is two blocks high, so its upper half only gives breaking particles.
        Block cattail = MainPlants.CATTAIL.get();
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(cattail).with(PropertyDispatch.initial(BlockStateProperties.DOUBLE_BLOCK_HALF)
                .select(DoubleBlockHalf.LOWER, variant(blockModel("cattail"))).select(DoubleBlockHalf.UPPER, variant(blockModel("cattail_top")))));
        flatXenonItem(cattail);

        List<Block> turned = new ArrayList<>();
        MainPlants.aubrietas().forEach(block -> turned.add(block.get()));
        MainPlants.litter().forEach(block -> turned.add(block.get()));
        for (Block block : turned) {
            generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block, variant(modelOf(block)))
                    .with(PropertyDispatch.modify(RandomRotationBlock.ROTATION).select(0, BlockModelGenerators.NOP)
                            .select(1, BlockModelGenerators.Y_ROT_90).select(2, BlockModelGenerators.Y_ROT_180).select(3, BlockModelGenerators.Y_ROT_270)));
            flatXenonItem(block);
        }

        // The placed stick: one of four models at random, turned to its facing.
        Block stick = MainPlants.STICK.get();
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(stick, BlockModelGenerators.variants(
                BlockModelGenerators.plainModel(blockModel("stick_0")), BlockModelGenerators.plainModel(blockModel("stick_1")),
                BlockModelGenerators.plainModel(blockModel("stick_2")), BlockModelGenerators.plainModel(blockModel("stick_3"))))
                .with(rotationFor(PlacedStickBlock.FACING)));
        flatXenonItem(stick);

        for (Block block : List.of(MainPlants.ROCK.get(), MainPlants.PEBBLES.get())) {
            String name = modelOf(block).getPath();
            generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(DecorationBlock.COUNT)
                    .generate(count -> variant(modLoc(name + "_" + count)))));
            flatXenonItem(block);
        }
        turkey(MainSpecialBlocks.RAW_TURKEY_BLOCK.get(), "raw_turkey");
        turkey(MainSpecialBlocks.COOKED_TURKEY_BLOCK.get(), "cooked_turkey");

        Block spikeTrap = MainSpecialBlocks.SPIKE_TRAP.get();
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(spikeTrap).with(BlockModelGenerators.createBooleanModelDispatch(
                SpikeTrapBlock.SPIKES_OUT, variant(blockModel("spike_trap_active")), variant(blockModel("spike_trap"))))
                .with(BlockModelGenerators.ROTATIONS_COLUMN_WITH_FACING));
        blockItemModel(spikeTrap, modLoc("item/spike_trap"));
        blockItemModel(MainSpecialBlocks.ECHO_GLASS.get(), modLoc("item/echo_glass"));

        simpleBlock(MainSpecialBlocks.AQUARIUM_GLASS.get());
        simpleBlock(MainSpecialBlocks.YELLOW_BIRCH_LEAVES.get());
        simpleBlock(MainSpecialBlocks.MYCENA_MUSHROOM_BLOCK.get());
        pillar(MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get());
        pillar(MainSpecialBlocks.STRIPPED_MYCENA_MUSHROOM_STEM.get());
    }

    /** A vine's tip and the rest of it, each with one model and Xenon's flat item. */
    private void vine(Block tip, String tipModel, Block body, String bodyModel) {
        simpleBlock(tip, blockModel(tipModel));
        simpleBlock(body, blockModel(bodyModel));
        flatXenonItem(tip);
        flatXenonItem(body);
    }

    /** A placed turkey, one model per bite: {@code block/<model>_<bites>}. */
    private void turkey(Block block, String model) {
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block).with(PropertyDispatch.initial(TurkeyMeatBlock.BITES)
                .generate(bites -> variant(blockModel(model + "_" + bites)))));
        flatXenonItem(block);
    }

    /** Gives a block's item Xenon's item model, {@code item/<name>}. */
    private void flatXenonItem(Block block) {
        if (block != MainSpecialBlocks.TURKEY_SKELETON.get() && block != MainSpecialBlocks.ECHO_GLASS.get()) {
            blockItemModel(block, modLoc("item/" + modelOf(block).getPath().substring("block/".length())));
        }
    }

    /** A cube with {@code side}, {@code top} and {@code bottom} textures, like sandstone. */
    private void bottomTop(Block block, Material side, Material top, Material bottom) {
        simpleBlock(block, TexturedModel.createDefault(b -> new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.TOP, top)
                .put(TextureSlot.BOTTOM, bottom), ModelTemplates.CUBE_BOTTOM_TOP));
    }
}
