package com.apeira.main.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.Apeira;
import com.apeira.main.world.MainConfiguredFeatures;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy ORForged mod's woods: seven trees with their building blocks, fungal planks in every dye colour, and the
 * beams, vertical planks and carved planks it added for vanilla woods.
 *
 * <p>Everything here is plain vanilla behaviour: logs and beams are {@link RotatedPillarBlock}s, leaves are vanilla
 * {@link LeavesBlock}s (decay by their {@code distance} state, no ticking of their own), and stripping and burning
 * are the {@code neoforge:transformables} and {@code neoforge:flammable} data maps. Maple and fir have Xenon's
 * saplings, which their leaves drop; ORForged's other woods have none, so their leaves drop sticks rather than a sapling.
 * Wooden items burn in furnaces like vanilla's.
 */
public final class MainWoods {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;
    /** ORForged's buttons stay pressed for 10 ticks, a third of oak's. */
    private static final int BUTTON_TICKS = 10;
    private static final UnaryOperator<Item.Properties> WOOD_FUEL = p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
    /** Every block registered here, in registration order. */
    private static final List<DeferredBlock<? extends Block>> ALL = new ArrayList<>();
    private static final List<Wood> WOODS = new ArrayList<>();
    private static final List<DeferredBlock<RotatedPillarBlock>> BEAMS = new ArrayList<>();
    private static final List<DeferredBlock<Block>> CARVED_PLANKS = new ArrayList<>();

    public static final Wood MAPLE = wood("maple", "orange_maple_leaves", "red_maple_leaves");
    /** Xenon's maple sapling, which grows its maple tree ({@code apeira:maple_tree}). */
    public static final DeferredBlock<SaplingBlock> MAPLE_SAPLING = sapling("maple", MainConfiguredFeatures.MAPLE_TREE);
    public static final Wood BLEAK = wood("bleak", "bleak_leaves");
    public static final Wood FIR = wood("fir", "green_fir_leaves", "orange_fir_leaves");
    /** Xenon's fir sapling, which grows its fir tree ({@code apeira:fir_tree}). */
    public static final DeferredBlock<SaplingBlock> FIR_SAPLING = sapling("fir", MainConfiguredFeatures.FIR_TREE);
    public static final Wood PALM = wood("palm", "palm_leaves");
    /** The block at the top of a palm trunk that its leaves grow from. Counts as a log, so palm leaves next to it stay. */
    public static final DeferredBlock<Block> PALM_CROWN = track(R.blockWithItem("palm_crown", Block::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS), WOOD_FUEL));
    public static final Wood BAYOU = wood("bayou", "bayou_leaves");
    public static final Wood WISTERIA = wood("wisteria", "wisteria_leaves");
    /** Wisteria's hanging flowers, leaves in four colours. */
    public static final List<DeferredBlock<LeavesBlock>> WISTERIA_FLOWERS = List.of(leaves("wisteria_flowers_blue"),
            leaves("wisteria_flowers_pink"), leaves("wisteria_flowers_purple"), leaves("wisteria_flowers_white"));
    public static final Wood BAOBAB = wood("baobab", "baobab_leaves");

    /** Fungal planks, made from mushroom stems, with vertical and carved variants. */
    public static final PlankSet FUNGAL = planks("fungal");
    public static final DeferredBlock<Block> VERTICAL_FUNGAL_PLANKS = plankVariant("vertical_fungal_planks");
    public static final DeferredBlock<Block> FUNGAL_PLANKS_CARVED = carvedPlanks("fungal");
    /** Fungal planks dyed in each of the 16 colours, in the creative inventory's order. */
    public static final List<DyedPlanks> DYED_FUNGAL = dyedFungal();

    /** ORForged's beams for vanilla woods, made from their logs. */
    public static final DeferredBlock<RotatedPillarBlock> OAK_BEAM = beam("oak", Blocks.OAK_LOG);
    public static final DeferredBlock<RotatedPillarBlock> SPRUCE_BEAM = beam("spruce", Blocks.SPRUCE_LOG);
    public static final DeferredBlock<RotatedPillarBlock> BIRCH_BEAM = beam("birch", Blocks.BIRCH_LOG);
    public static final DeferredBlock<RotatedPillarBlock> JUNGLE_BEAM = beam("jungle", Blocks.JUNGLE_LOG);
    public static final DeferredBlock<RotatedPillarBlock> ACACIA_BEAM = beam("acacia", Blocks.ACACIA_LOG);
    public static final DeferredBlock<RotatedPillarBlock> DARK_OAK_BEAM = beam("dark_oak", Blocks.DARK_OAK_LOG);
    public static final DeferredBlock<RotatedPillarBlock> MANGROVE_BEAM = beam("mangrove", Blocks.MANGROVE_LOG);
    public static final DeferredBlock<RotatedPillarBlock> CHERRY_BEAM = beam("cherry", Blocks.CHERRY_LOG);
    public static final DeferredBlock<RotatedPillarBlock> CRIMSON_BEAM = beam("crimson", Blocks.CRIMSON_STEM);
    public static final DeferredBlock<RotatedPillarBlock> WARPED_BEAM = beam("warped", Blocks.WARPED_STEM);

    /** ORForged's carved planks for vanilla woods. */
    public static final List<VanillaCarvedPlanks> VANILLA_CARVED_PLANKS = List.of(
            vanillaCarved("oak", Blocks.OAK_PLANKS), vanillaCarved("spruce", Blocks.SPRUCE_PLANKS),
            vanillaCarved("birch", Blocks.BIRCH_PLANKS), vanillaCarved("jungle", Blocks.JUNGLE_PLANKS),
            vanillaCarved("acacia", Blocks.ACACIA_PLANKS), vanillaCarved("dark_oak", Blocks.DARK_OAK_PLANKS),
            vanillaCarved("mangrove", Blocks.MANGROVE_PLANKS), vanillaCarved("cherry", Blocks.CHERRY_PLANKS),
            vanillaCarved("crimson", Blocks.CRIMSON_PLANKS), vanillaCarved("warped", Blocks.WARPED_PLANKS));

    /**
     * ORForged's vertical planks for vanilla woods. ORForged had no texture for them; the Origin Realms server pack draws
     * its vertical planks with the wood's own planks texture turned on its side ({@code block/vertical_planks}).
     */
    public static final List<VanillaVerticalPlanks> VANILLA_VERTICAL_PLANKS = List.of(
            vanillaVertical("oak", Blocks.OAK_PLANKS), vanillaVertical("spruce", Blocks.SPRUCE_PLANKS),
            vanillaVertical("birch", Blocks.BIRCH_PLANKS), vanillaVertical("jungle", Blocks.JUNGLE_PLANKS),
            vanillaVertical("acacia", Blocks.ACACIA_PLANKS), vanillaVertical("dark_oak", Blocks.DARK_OAK_PLANKS),
            vanillaVertical("mangrove", Blocks.MANGROVE_PLANKS), vanillaVertical("cherry", Blocks.CHERRY_PLANKS),
            vanillaVertical("crimson", Blocks.CRIMSON_PLANKS), vanillaVertical("warped", Blocks.WARPED_PLANKS),
            vanillaVertical("bamboo", Blocks.BAMBOO_PLANKS));

    private MainWoods() {}

    /** Every block registered here, in registration order. */
    public static List<DeferredBlock<? extends Block>> all() {
        return List.copyOf(ALL);
    }

    /** The seven trees, in the order above. */
    public static List<Wood> woods() {
        return List.copyOf(WOODS);
    }

    /** Every beam: the trees' first, then the vanilla woods'. */
    public static List<DeferredBlock<RotatedPillarBlock>> beams() {
        return List.copyOf(BEAMS);
    }

    /** Xenon's saplings: maple and fir. */
    public static List<DeferredBlock<SaplingBlock>> saplings() {
        return List.of(MAPLE_SAPLING, FIR_SAPLING);
    }

    /** Every carved planks block: the trees', fungal and the vanilla woods'. */
    public static List<DeferredBlock<Block>> carvedPlanks() {
        return List.copyOf(CARVED_PLANKS);
    }

    /** Every leaves block: the trees' leaves and wisteria's flowers. */
    public static List<DeferredBlock<LeavesBlock>> allLeaves() {
        List<DeferredBlock<LeavesBlock>> leaves = new ArrayList<>();
        WOODS.forEach(wood -> leaves.addAll(wood.leaves()));
        leaves.addAll(WISTERIA_FLOWERS);
        return leaves;
    }

    /** Every fungal plank set: plain, then the 16 dyed ones. */
    public static List<PlankSet> fungalPlankSets() {
        List<PlankSet> sets = new ArrayList<>();
        sets.add(FUNGAL);
        DYED_FUNGAL.forEach(dyed -> sets.add(dyed.planks()));
        return sets;
    }

    private static Wood wood(String name, String... leaves) {
        var log = log(name + "_log", Blocks.OAK_LOG);
        var wood = log(name + "_wood", Blocks.OAK_WOOD);
        var strippedLog = log("stripped_" + name + "_log", Blocks.STRIPPED_OAK_LOG);
        var strippedWood = log("stripped_" + name + "_wood", Blocks.STRIPPED_OAK_WOOD);
        PlankSet planks = planks(name);
        var verticalPlanks = plankVariant("vertical_" + name + "_planks");
        var carvedPlanks = carvedPlanks(name);
        List<DeferredBlock<LeavesBlock>> leafBlocks = new ArrayList<>();
        for (String leaf : leaves) {
            leafBlocks.add(leaves(leaf));
        }
        var beam = beam(name, log);
        Wood result = new Wood(name, log, wood, strippedLog, strippedWood, planks, verticalPlanks, carvedPlanks,
                List.copyOf(leafBlocks), beam, MainTags.logs(name));
        WOODS.add(result);
        return result;
    }

    /** A log or wood: ORForged's are twice as hard as oak's. */
    private static DeferredBlock<RotatedPillarBlock> log(String name, Block like) {
        return track(R.blockWithItem(name, RotatedPillarBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(like).strength(4.0F),
                WOOD_FUEL));
    }

    private static PlankSet planks(String name) {
        PlankSet planks = PlankSet.register(R, name, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS), BlockSetType.OAK,
                WoodType.OAK, BUTTON_TICKS);
        ALL.addAll(planks.all());
        return planks;
    }

    /** Planks laid another way, with oak planks' properties. */
    private static DeferredBlock<Block> plankVariant(String name) {
        return track(R.blockWithItem(name, Block::new, () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS), WOOD_FUEL));
    }

    private static DeferredBlock<Block> carvedPlanks(String wood) {
        DeferredBlock<Block> carved = plankVariant(wood + "_planks_carved");
        CARVED_PLANKS.add(carved);
        return carved;
    }

    private static DeferredBlock<LeavesBlock> leaves(String name) {
        return track(R.blockWithItem(name, p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
                () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_LEAVES), UnaryOperator.identity()));
    }

    /** A sapling with oak's properties that grows {@code tree}, composting and burning like vanilla's. */
    private static DeferredBlock<SaplingBlock> sapling(String wood, ResourceKey<Feature> tree) {
        TreeGrower grower = new TreeGrower(R.id(wood).toString(), WeightedList.of(tree), WeightedList.of(), WeightedList.of(), null);
        return track(R.blockWithItem(wood + "_sapling", p -> new SaplingBlock(grower, p),
                () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_SAPLING),
                p -> p.compostable(ContextIntProviders.COMPOSTABLE_LOW).cookingFuel(ContextIntProviders.COOKING_TIME_DRY_PLANTS)));
    }

    /** A beam of the wood whose log is {@code log}: its properties, but as soft as planks. */
    private static DeferredBlock<RotatedPillarBlock> beam(String wood, DeferredBlock<RotatedPillarBlock> log) {
        DeferredBlock<RotatedPillarBlock> beam = R.blockWithItem(wood + "_beam", RotatedPillarBlock::new,
                () -> BlockBehaviour.Properties.ofLegacyCopy(log.get()).strength(2.0F, 3.0F), WOOD_FUEL);
        BEAMS.add(beam);
        return track(beam);
    }

    private static DeferredBlock<RotatedPillarBlock> beam(String wood, Block log) {
        DeferredBlock<RotatedPillarBlock> beam = R.blockWithItem(wood + "_beam", RotatedPillarBlock::new,
                () -> BlockBehaviour.Properties.ofLegacyCopy(log).strength(2.0F, 3.0F), WOOD_FUEL);
        BEAMS.add(beam);
        return track(beam);
    }

    private static <B extends Block> DeferredBlock<B> track(DeferredBlock<B> block) {
        ALL.add(block);
        return block;
    }

    private static VanillaCarvedPlanks vanillaCarved(String wood, Block planks) {
        return new VanillaCarvedPlanks(carvedPlanks(wood), planks);
    }

    private static VanillaVerticalPlanks vanillaVertical(String wood, Block planks) {
        return new VanillaVerticalPlanks(plankVariant("vertical_" + wood + "_planks"), planks);
    }

    private static List<DyedPlanks> dyedFungal() {
        List<DyedPlanks> dyed = new ArrayList<>();
        for (DyeColor color : List.of(DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.BROWN, DyeColor.RED,
                DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.BLUE,
                DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK)) {
            dyed.add(new DyedPlanks(color, planks(color.getSerializedName() + "_fungal")));
        }
        return List.copyOf(dyed);
    }

    public static void init(ApeiraRegistrar registrar) {}

    /**
     * One of ORForged's trees: logs, woods and their stripped forms, planks and their shapes, vertical and carved
     * planks, one or two colours of leaves and a beam. {@code logs} is its {@code apeira:<name>_logs} tag.
     */
    public record Wood(String name, DeferredBlock<RotatedPillarBlock> log, DeferredBlock<RotatedPillarBlock> wood,
            DeferredBlock<RotatedPillarBlock> strippedLog, DeferredBlock<RotatedPillarBlock> strippedWood, PlankSet planks,
            DeferredBlock<Block> verticalPlanks, DeferredBlock<Block> carvedPlanks, List<DeferredBlock<LeavesBlock>> leaves,
            DeferredBlock<RotatedPillarBlock> beam, BlockItemTagId logs) {
        /** The log, wood, stripped log and stripped wood. */
        public List<DeferredBlock<RotatedPillarBlock>> logBlocks() {
            return List.of(log, wood, strippedLog, strippedWood);
        }
    }

    /** Fungal planks dyed {@code color}, with their shapes. */
    public record DyedPlanks(DyeColor color, PlankSet planks) {}

    /** Carved planks of a vanilla wood. */
    public record VanillaCarvedPlanks(DeferredBlock<Block> carved, Block planks) {}

    /** Vertical planks of a vanilla wood. */
    public record VanillaVerticalPlanks(DeferredBlock<Block> vertical, Block planks) {}
}
