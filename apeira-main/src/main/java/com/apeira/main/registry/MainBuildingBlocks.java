package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's other building blocks, with its ids and properties ({@link MainBlockProperties}): moss and
 * grimweed blocks, the blighted shroomlight, bone blocks, wooden and crop crates, baskets, bundles, the shoji screen
 * and the salvaging table. Xenon gave none of them a behaviour of its own.
 *
 * <p>Textures are Xenon's, renamed to Apeira's convention: {@code block/<name>} sides and {@code block/<name>_top}
 * ends. Carved bone blocks show a different carving when lying down ({@code block/<name>_horizontal}); grimweed blocks
 * have a {@code _top}, a {@code block/loam} bottom and their own sides, and the salvaging table a {@code _front},
 * {@code _side}, {@code _top} and {@code _bottom}, facing north.
 */
public final class MainBuildingBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();

    public static final DeferredBlock<Block> DAMP_MOSS_BLOCK = block("damp_moss_block", Block::new, MainBlockProperties::dampMossBlock);
    public static final DeferredBlock<Block> GRIMOSS_BLOCK = block("grimoss_block", Block::new, MainBlockProperties::dampMossBlock);
    public static final DeferredBlock<Block> GRIMWEED_BLOCK = block("grimweed_block", Block::new, MainBlockProperties::grass);
    public static final DeferredBlock<Block> PALE_GRIMWEED_BLOCK = block("pale_grimweed_block", Block::new, MainBlockProperties::grass);
    public static final DeferredBlock<Block> BLIGHTED_SHROOMLIGHT = block("blighted_shroomlight", Block::new, MainBlockProperties::shroomlight);

    public static final DeferredBlock<RotatedPillarBlock> BRITTLE_BONE_BLOCK = bone("brittle_bone_block");
    public static final DeferredBlock<RotatedPillarBlock> CARVED_BRITTLE_BONE_BLOCK = bone("carved_brittle_bone_block");
    public static final DeferredBlock<RotatedPillarBlock> BLIGHTED_BONE_BLOCK = bone("blighted_bone_block");
    public static final DeferredBlock<RotatedPillarBlock> CARVED_BLIGHTED_BONE_BLOCK = bone("carved_blighted_bone_block");
    public static final DeferredBlock<RotatedPillarBlock> SCORCHED_BONE_BLOCK = bone("scorched_bone_block");
    public static final DeferredBlock<RotatedPillarBlock> CARVED_SCORCHED_BONE_BLOCK = bone("carved_scorched_bone_block");

    public static final DeferredBlock<Block> FRAMED_WOODEN_CRATE = block("framed_wooden_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> CLOSED_WOODEN_CRATE = block("closed_wooden_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> SLATTED_WOODEN_CRATE = block("slatted_wooden_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> ENFORCED_WOODEN_CRATE = block("enforced_wooden_crate", Block::new, MainBlockProperties::crates);
    /** Crop crates and baskets: a crate or basket side and its crop on top. */
    public static final DeferredBlock<Block> POTATO_CRATE = block("potato_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> CARROT_CRATE = block("carrot_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> BEET_CRATE = block("beet_crate", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> APPLE_BASKET = block("apple_basket", Block::new, MainBlockProperties::crates);
    public static final DeferredBlock<Block> BERRY_BASKET = block("berry_basket", Block::new, MainBlockProperties::crates);
    /** Bundles turn with their axis, like hay bales. */
    public static final DeferredBlock<RotatedPillarBlock> BAMBOO_BUNDLE = block("bamboo_bundle", RotatedPillarBlock::new,
            MainBlockProperties::bambooCrates);
    public static final DeferredBlock<RotatedPillarBlock> CACTUS_BUNDLE = block("cactus_bundle", RotatedPillarBlock::new,
            MainBlockProperties::cactusCrates);
    public static final DeferredBlock<RotatedPillarBlock> SUGAR_CANE_BUNDLE = block("sugar_cane_bundle", RotatedPillarBlock::new,
            MainBlockProperties::crates);
    public static final DeferredBlock<RotatedPillarBlock> STICK_BUNDLE = block("stick_bundle", RotatedPillarBlock::new,
            MainBlockProperties::crates);

    public static final DeferredBlock<Block> SHOJI = block("shoji", Block::new, MainBlockProperties::shoji);
    public static final DeferredBlock<Block> SALVAGING_TABLE = block("salvaging_table", Block::new, MainBlockProperties::table);

    private MainBuildingBlocks() {}

    /** Every block registered here, in registration order. */
    public static List<DeferredBlock<? extends Block>> all() {
        return List.copyOf(ALL);
    }

    public static List<DeferredBlock<RotatedPillarBlock>> bones() {
        return List.of(BRITTLE_BONE_BLOCK, CARVED_BRITTLE_BONE_BLOCK, BLIGHTED_BONE_BLOCK, CARVED_BLIGHTED_BONE_BLOCK, SCORCHED_BONE_BLOCK,
                CARVED_SCORCHED_BONE_BLOCK);
    }

    public static List<DeferredBlock<Block>> woodenCrates() {
        return List.of(FRAMED_WOODEN_CRATE, CLOSED_WOODEN_CRATE, SLATTED_WOODEN_CRATE, ENFORCED_WOODEN_CRATE);
    }

    public static List<DeferredBlock<Block>> cropCrates() {
        return List.of(POTATO_CRATE, CARROT_CRATE, BEET_CRATE, APPLE_BASKET, BERRY_BASKET);
    }

    public static List<DeferredBlock<RotatedPillarBlock>> bundles() {
        return List.of(BAMBOO_BUNDLE, CACTUS_BUNDLE, SUGAR_CANE_BUNDLE, STICK_BUNDLE);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blockWithItem(name, factory, properties, p -> p);
        ALL.add(block);
        return block;
    }

    private static DeferredBlock<RotatedPillarBlock> bone(String name) {
        return block(name, RotatedPillarBlock::new, MainBlockProperties::bone);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
