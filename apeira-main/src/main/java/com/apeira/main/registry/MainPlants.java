package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.block.CattailBlock;
import com.apeira.main.block.CloverBlock;
import com.apeira.main.block.DecorationBlock;
import com.apeira.main.block.GroundCoverBlock;
import com.apeira.main.block.GroundedRotationBlock;
import com.apeira.main.block.HangingVineBlock;
import com.apeira.main.block.HangingVinePlantBlock;
import com.apeira.main.block.MossCoverBlock;
import com.apeira.main.block.TwigBlock;
import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.block.SmallLilyBlock;
import com.apeira.main.block.TallOrangeMycenaBlock;
import com.apeira.main.block.XenonFlowerBlock;
import com.apeira.main.block.XenonPlantBlock;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy Xenon mod's plants, with its ids and properties ({@link MainBlockProperties}): grasses and bushes, each
 * short one with a tall one that bone meal turns it into, brambles, flowers, cattails, clover, aubrieta, orange mycena,
 * leaf litter and shelf fungus, rocks and pebbles, the twig, ash and frost, the damp moss and grimoss covers,
 * the small lily and four hanging vines. Their models are Xenon's own (hand-made resources).
 */
public final class MainPlants {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();

    public static final DeferredBlock<XenonPlantBlock> AMARYLLIS = block("amaryllis", XenonPlantBlock::tall, MainBlockProperties::plants);
    public static final DeferredBlock<CattailBlock> CATTAIL = block("cattail", CattailBlock::new, MainBlockProperties::plants);
    public static final DeferredBlock<XenonFlowerBlock> OCULUS_ORCHID = block("oculus_orchid",
            p -> new XenonFlowerBlock(MobEffects.FIRE_RESISTANCE, 0.35F, p).onNetherSoil(), MainBlockProperties::plants);
    /** Placed on water like a lily pad. */
    public static final DeferredBlock<SmallLilyBlock> SMALL_LILY = R.blocks().registerBlock("small_lily", SmallLilyBlock::new,
            MainBlockProperties::smallLily);
    static {
        ALL.add(SMALL_LILY);
        R.items().registerItem("small_lily", p -> new PlaceOnWaterBlockItem(SMALL_LILY.get(), p.useBlockDescriptionPrefix()));
    }
    public static final DeferredBlock<XenonFlowerBlock> HIBISCUS = block("hibiscus",
            p -> new XenonFlowerBlock(MobEffects.ABSORPTION, 0.35F, p), MainBlockProperties::plants);

    public static final DeferredBlock<XenonPlantBlock> TALL_BEACH_GRASS = tall("tall_beach_grass");
    public static final DeferredBlock<XenonPlantBlock> BEACH_GRASS = growsInto("beach_grass", TALL_BEACH_GRASS);
    public static final DeferredBlock<XenonPlantBlock> TALL_BRAMBLE = block("tall_bramble",
            p -> new XenonPlantBlock(p, XenonPlantBlock.TALL, null, true), MainBlockProperties::bramble);
    public static final DeferredBlock<XenonPlantBlock> BRAMBLE = block("bramble",
            p -> new XenonPlantBlock(p, XenonPlantBlock.SHORT, TALL_BRAMBLE, true), MainBlockProperties::bramble);
    public static final DeferredBlock<XenonPlantBlock> TALL_DRY_GRASS = tall("tall_dry_grass");
    public static final DeferredBlock<XenonPlantBlock> DRY_GRASS = growsInto("dry_grass", TALL_DRY_GRASS);
    public static final DeferredBlock<XenonPlantBlock> TALL_DRY_BUSH = tall("tall_dry_bush");
    public static final DeferredBlock<XenonPlantBlock> DRY_BUSH = growsInto("dry_bush", TALL_DRY_BUSH);
    public static final DeferredBlock<XenonPlantBlock> TALL_FROSTED_GRASS = tall("tall_frosted_grass");
    public static final DeferredBlock<XenonPlantBlock> FROSTED_GRASS = growsInto("frosted_grass", TALL_FROSTED_GRASS);
    public static final DeferredBlock<XenonPlantBlock> TALL_GRIMWEED = tall("tall_grimweed");
    public static final DeferredBlock<XenonPlantBlock> GRIMWEED = growsInto("grimweed", TALL_GRIMWEED);
    public static final DeferredBlock<XenonPlantBlock> TALL_PALE_GRIMWEED = tall("tall_pale_grimweed");
    public static final DeferredBlock<XenonPlantBlock> PALE_GRIMWEED = growsInto("pale_grimweed", TALL_PALE_GRIMWEED);
    public static final DeferredBlock<XenonPlantBlock> TALL_HOGWEED = block("tall_hogweed", p -> XenonPlantBlock.tall(p).onNetherSoil(),
            MainBlockProperties::plants);
    public static final DeferredBlock<XenonPlantBlock> HOGWEED = block("hogweed", p -> XenonPlantBlock.growsInto(p, TALL_HOGWEED).onNetherSoil(),
            MainBlockProperties::plants);
    /** Bone meal on it grows one of Xenon's orange mycena mushrooms ({@code OrangeMycenaFeature}). */
    public static final DeferredBlock<TallOrangeMycenaBlock> TALL_ORANGE_MYCENA = block("tall_orange_mycena", TallOrangeMycenaBlock::new,
            MainBlockProperties::plants);
    public static final DeferredBlock<XenonPlantBlock> ORANGE_MYCENA = growsInto("orange_mycena", TALL_ORANGE_MYCENA);
    public static final DeferredBlock<XenonPlantBlock> TALL_SOUL_STALKS = block("tall_soul_stalks", p -> XenonPlantBlock.tall(p).onNetherSoil(),
            MainBlockProperties::plants);
    public static final DeferredBlock<XenonPlantBlock> SOUL_STALKS = block("soul_stalks",
            p -> XenonPlantBlock.growsInto(p, TALL_SOUL_STALKS).onNetherSoil(), MainBlockProperties::plants);
    public static final DeferredBlock<CloverBlock> CLOVER = block("clover", CloverBlock::new, MainBlockProperties::plants);

    public static final DeferredBlock<GroundedRotationBlock> BLUE_AUBRIETA = aubrieta("blue_aubrieta");
    public static final DeferredBlock<GroundedRotationBlock> PINK_AUBRIETA = aubrieta("pink_aubrieta");
    public static final DeferredBlock<GroundedRotationBlock> PURPLE_AUBRIETA = aubrieta("purple_aubrieta");
    public static final DeferredBlock<GroundedRotationBlock> WHITE_AUBRIETA = aubrieta("white_aubrieta");
    public static final DeferredBlock<GroundedRotationBlock> RED_AUBRIETA = aubrieta("red_aubrieta");

    public static final DeferredBlock<TwigBlock> TWIG = block("twig", TwigBlock::new, MainBlockProperties::stickPlaceable);

    public static final DeferredBlock<HangingVineBlock> SCULK_VINES = block("sculk_vines",
            p -> new HangingVineBlock(p, MainPlants.SCULK_VINES_END), MainBlockProperties::vines);
    /** The rest of the sculk vines; the tip's item places both. */
    public static final DeferredBlock<HangingVinePlantBlock> SCULK_VINES_END = vineBody("sculk_vines_end", SCULK_VINES,
            MainBlockProperties::vines);
    public static final DeferredBlock<HangingVineBlock> WARPED_VEIL = block("warped_veil",
            p -> new HangingVineBlock(p, MainPlants.WARPED_VEIL_BOTTOM), MainBlockProperties::vines);
    public static final DeferredBlock<HangingVinePlantBlock> WARPED_VEIL_BOTTOM = vineBody("warped_veil_bottom", WARPED_VEIL,
            MainBlockProperties::vines);

    public static final DeferredBlock<GroundCoverBlock> ASH = block("ash", GroundCoverBlock::new, MainBlockProperties::ash);
    public static final DeferredBlock<GroundCoverBlock> FROST = block("frost", GroundCoverBlock::new, MainBlockProperties::frost);
    public static final DeferredBlock<RandomRotationBlock> DRIED_LEAF_LITTER = block("dried_leaf_litter", RandomRotationBlock::new,
            MainBlockProperties::natural);
    public static final DeferredBlock<RandomRotationBlock> COLD_LEAF_LITTER = block("cold_leaf_litter", RandomRotationBlock::new,
            MainBlockProperties::natural);
    public static final DeferredBlock<RandomRotationBlock> LEAF_LITTER = block("leaf_litter", RandomRotationBlock::new,
            MainBlockProperties::natural);
    public static final DeferredBlock<RandomRotationBlock> SHELF_FUNGUS = block("shelf_fungus", RandomRotationBlock::new,
            MainBlockProperties::naturalMushroom);

    /**
     * Xenon's {@code grimoss_hang_bottom} is the vine above the tip, {@code grimoss_hang} the tip. Neither has an item:
     * the grimoss cover's item places the cover on a floor and the hang under a block.
     */
    public static final DeferredBlock<HangingVinePlantBlock> GRIMOSS_HANG_BOTTOM = vineBody("grimoss_hang_bottom", () -> MainPlants.GRIMOSS_HANG.get(),
            MainBlockProperties::mossHang);
    public static final DeferredBlock<HangingVineBlock> GRIMOSS_HANG = blockOnly("grimoss_hang",
            p -> new HangingVineBlock(p, GRIMOSS_HANG_BOTTOM, MainPlants.GRIMOSS), MainBlockProperties::mossHang);
    public static final DeferredBlock<MossCoverBlock> GRIMOSS = block("grimoss", p -> new MossCoverBlock(p, GRIMOSS_HANG, GRIMOSS_HANG_BOTTOM),
            MainBlockProperties::mossCover);
    public static final DeferredBlock<HangingVinePlantBlock> DAMP_MOSS_HANG_BOTTOM = vineBody("damp_moss_hang_bottom",
            () -> MainPlants.DAMP_MOSS_HANG.get(), MainBlockProperties::mossHang);
    public static final DeferredBlock<HangingVineBlock> DAMP_MOSS_HANG = blockOnly("damp_moss_hang",
            p -> new HangingVineBlock(p, DAMP_MOSS_HANG_BOTTOM, MainPlants.DAMP_MOSS), MainBlockProperties::mossHang);
    public static final DeferredBlock<MossCoverBlock> DAMP_MOSS = block("damp_moss",
            p -> new MossCoverBlock(p, DAMP_MOSS_HANG, DAMP_MOSS_HANG_BOTTOM), MainBlockProperties::mossCover);

    public static final DeferredBlock<DecorationBlock> ROCK = block("rock", DecorationBlock::new, MainBlockProperties::rockItem);
    public static final DeferredBlock<DecorationBlock> PEBBLES = block("pebbles", DecorationBlock::new, MainBlockProperties::rockItem);

    private MainPlants() {}

    /** Every block registered here. */
    public static List<DeferredBlock<? extends Block>> all() {
        return List.copyOf(ALL);
    }

    /** Short plants and the tall ones they grow into. */
    public static List<DeferredBlock<XenonPlantBlock>> grasses() {
        return List.of(BEACH_GRASS, TALL_BEACH_GRASS, BRAMBLE, TALL_BRAMBLE, DRY_GRASS, TALL_DRY_GRASS, DRY_BUSH, TALL_DRY_BUSH, FROSTED_GRASS,
                TALL_FROSTED_GRASS, GRIMWEED, TALL_GRIMWEED, PALE_GRIMWEED, TALL_PALE_GRIMWEED, HOGWEED, TALL_HOGWEED, SOUL_STALKS,
                TALL_SOUL_STALKS, AMARYLLIS);
    }

    public static List<DeferredBlock<GroundedRotationBlock>> aubrietas() {
        return List.of(BLUE_AUBRIETA, PINK_AUBRIETA, PURPLE_AUBRIETA, WHITE_AUBRIETA, RED_AUBRIETA);
    }

    public static List<DeferredBlock<RandomRotationBlock>> litter() {
        return List.of(DRIED_LEAF_LITTER, COLD_LEAF_LITTER, LEAF_LITTER, SHELF_FUNGUS);
    }

    /** Every vine block, tips and the rest: sculk vines, warped veil, grimoss and damp moss. */
    public static List<DeferredBlock<? extends Block>> vines() {
        return List.of(SCULK_VINES, SCULK_VINES_END, WARPED_VEIL, WARPED_VEIL_BOTTOM, GRIMOSS_HANG, GRIMOSS_HANG_BOTTOM, DAMP_MOSS_HANG,
                DAMP_MOSS_HANG_BOTTOM);
    }

    private static <B extends Block> DeferredBlock<B> block(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blockWithItem(name, factory, properties, p -> p);
        ALL.add(block);
        return block;
    }

    private static DeferredBlock<XenonPlantBlock> tall(String name) {
        return block(name, XenonPlantBlock::tall, MainBlockProperties::plants);
    }

    private static DeferredBlock<XenonPlantBlock> growsInto(String name, DeferredBlock<? extends XenonPlantBlock> tall) {
        return block(name, p -> XenonPlantBlock.growsInto(p, tall), MainBlockProperties::plants);
    }

    private static DeferredBlock<GroundedRotationBlock> aubrieta(String name) {
        return block(name, GroundedRotationBlock::new, MainBlockProperties::plantsNoOffset);
    }

    /** The rest of a vine, above its tip {@code head}; it has no item of its own ({@link HangingVineBlock#item()}). */
    private static DeferredBlock<HangingVinePlantBlock> vineBody(String name, Supplier<HangingVineBlock> head,
            Supplier<BlockBehaviour.Properties> properties) {
        return blockOnly(name, p -> new HangingVinePlantBlock(p, head), properties);
    }

    /** A block without an item. */
    private static <B extends Block> DeferredBlock<B> blockOnly(String name, Function<BlockBehaviour.Properties, ? extends B> factory,
            Supplier<BlockBehaviour.Properties> properties) {
        DeferredBlock<B> block = R.blocks().registerBlock(name, factory, properties);
        ALL.add(block);
        return block;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
