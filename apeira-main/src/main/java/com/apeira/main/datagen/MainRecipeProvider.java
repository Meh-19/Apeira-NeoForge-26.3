package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraRecipeProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Recipes for Apeira's items and blocks.
 *
 * <p>From ORForged: fungal planks from mushroom stems and dyed with eight planks around a dye, vertical planks from
 * three planks in a column, beams from two logs side by side, and beams back into four planks. ORForged had no other
 * recipes, so the rest are vanilla's for the same kind of block: wood families (planks from logs, wood from logs,
 * stairs, slabs, ...), storage blocks, ore smelting, tools and armour. Bricks and pillars are made like vanilla's
 * from four and two blocks, carved planks like chiseled blocks, from two slabs, stone variants as {@link #stones()}
 * says, and lanterns like vanilla's.
 */
public final class MainRecipeProvider extends ApeiraRecipeProvider {
    public MainRecipeProvider(BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements, String namespace) {
        super(recipes, advancements, namespace);
    }

    @Override
    protected void buildRecipes() {
        minerals(MainItems.PLATINUM.get(), MainBlocks.PLATINUM_BLOCK.get(), MainBlocks.PLATINUM_BRICKS.get(), MainBlocks.PLATINUM_PILLAR.get(),
                MainBlocks.PLATINUM_ORE.get(), MainBlocks.DEEPSLATE_PLATINUM_ORE.get());
        minerals(MainItems.RUBY.get(), MainBlocks.RUBY_BLOCK.get(), MainBlocks.RUBY_BRICKS.get(), MainBlocks.RUBY_PILLAR.get(),
                MainBlocks.RUBY_ORE.get(), MainBlocks.DEEPSLATE_RUBY_ORE.get());
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            bricks(mineral.brick().get(), mineral.block());
            pillar(mineral.pillar().get(), mineral.block());
        }
        tools();
        armor();

        for (MainWoods.Wood wood : MainWoods.woods()) {
            planksFromLog(wood.planks().planks(), wood.logs().item(), 4);
            woodFromLogs(wood.wood(), wood.log());
            woodFromLogs(wood.strippedWood(), wood.strippedLog());
            planks(wood.planks());
            verticalPlanks(wood.verticalPlanks(), wood.planks().planks());
            carvedPlanks(wood.carvedPlanks(), wood.planks().slab());
            beam(wood.beam(), wood.log(), wood.planks().planks());
        }

        shapeless(RecipeCategory.BUILDING_BLOCKS, MainWoods.FUNGAL.planks(), 4).requires(Blocks.MUSHROOM_STEM)
                .unlockedBy(getHasName(Blocks.MUSHROOM_STEM), has(Blocks.MUSHROOM_STEM))
                .save(output);
        planks(MainWoods.FUNGAL);
        verticalPlanks(MainWoods.VERTICAL_FUNGAL_PLANKS, MainWoods.FUNGAL.planks());
        carvedPlanks(MainWoods.FUNGAL_PLANKS_CARVED, MainWoods.FUNGAL.slab());
        for (MainWoods.DyedPlanks dyed : MainWoods.DYED_FUNGAL) {
            Item dye = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dyed.color().getSerializedName() + "_dye"));
            shaped(RecipeCategory.BUILDING_BLOCKS, dyed.planks().planks(), 8)
                    .pattern("###").pattern("#D#").pattern("###")
                    .define('#', MainWoods.FUNGAL.planks()).define('D', dye)
                    .group("dyed_fungal_planks")
                    .unlockedBy(getHasName(MainWoods.FUNGAL.planks()), has(MainWoods.FUNGAL.planks()))
                    .save(output);
            planks(dyed.planks());
        }

        beam(MainWoods.OAK_BEAM.get(), Blocks.OAK_LOG, Blocks.OAK_PLANKS);
        beam(MainWoods.SPRUCE_BEAM.get(), Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS);
        beam(MainWoods.BIRCH_BEAM.get(), Blocks.BIRCH_LOG, Blocks.BIRCH_PLANKS);
        beam(MainWoods.JUNGLE_BEAM.get(), Blocks.JUNGLE_LOG, Blocks.JUNGLE_PLANKS);
        beam(MainWoods.ACACIA_BEAM.get(), Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS);
        beam(MainWoods.DARK_OAK_BEAM.get(), Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS);
        beam(MainWoods.MANGROVE_BEAM.get(), Blocks.MANGROVE_LOG, Blocks.MANGROVE_PLANKS);
        beam(MainWoods.CHERRY_BEAM.get(), Blocks.CHERRY_LOG, Blocks.CHERRY_PLANKS);
        beam(MainWoods.CRIMSON_BEAM.get(), Blocks.CRIMSON_STEM, Blocks.CRIMSON_PLANKS);
        beam(MainWoods.WARPED_BEAM.get(), Blocks.WARPED_STEM, Blocks.WARPED_PLANKS);
        for (MainWoods.VanillaCarvedPlanks carved : MainWoods.VANILLA_CARVED_PLANKS) {
            carvedPlanks(carved.carved(), slabOf(carved.planks()));
        }
        for (MainWoods.VanillaVerticalPlanks vertical : MainWoods.VANILLA_VERTICAL_PLANKS) {
            verticalPlanks(vertical.vertical(), vertical.planks());
        }

        stones();
        for (MainLanterns.Lantern lantern : MainLanterns.all()) {
            lantern(lantern.block(), lantern.planks());
        }
    }

    /**
     * ORForged's stone variants and stained stone. ORForged had no recipes for them, so each is cut in a stonecutter
     * from the block it is a variant of (crafting four of it would clash with vanilla's polished stones and sandstone,
     * and with the bricks and polished block of the same stone). Dried silt is smelted silt, and blue nether bricks are
     * red nether bricks' recipe with warped fungus for the nether wart. Slate, limestone and silt are natural stones.
     */
    private void stones() {
        stoneVariants(Blocks.ANDESITE, MainStones.ANDESITE_BRICKS, MainStones.CHISELED_ANDESITE);
        stoneVariants(Blocks.DIORITE, MainStones.DIORITE_BRICKS, MainStones.CHISELED_DIORITE);
        stoneVariants(Blocks.GRANITE, MainStones.GRANITE_BRICKS, MainStones.CHISELED_GRANITE);
        stoneVariants(Blocks.STONE, MainStones.CHISELED_STONE);
        stoneVariants(Blocks.DRIPSTONE_BLOCK, MainStones.DRIPSTONE_BRICKS, MainStones.CHISELED_DRIPSTONE, MainStones.POLISHED_DRIPSTONE);
        stoneVariants(Blocks.MUD_BRICKS, MainStones.CHISELED_MUD, MainStones.POLISHED_MUD);
        stoneVariants(MainStones.SLATE.get(), MainStones.SLATE_BRICKS, MainStones.CHISELED_SLATE, MainStones.POLISHED_SLATE);
        stoneVariants(MainStones.LIMESTONE.get(), MainStones.LIMESTONE_BRICKS, MainStones.SANDY_LIMESTONE_BRICKS, MainStones.CHISELED_LIMESTONE,
                MainStones.POLISHED_LIMESTONE);
        stoneVariants(Blocks.DIRT, MainStones.PACKED_DIRT);
        stoneVariants(Blocks.SAND, MainStones.PACKED_SAND);
        stoneVariants(Blocks.RED_SAND, MainStones.PACKED_RED_SAND);
        smeltingResultFromBase(MainStones.DRIED_SILT, MainStones.SILT);
        stoneVariants(MainStones.DRIED_SILT.get(), MainStones.SILT_BRICKS, MainStones.CHISELED_SILT_BRICKS, MainStones.POLISHED_SILT);
        shaped(RecipeCategory.BUILDING_BLOCKS, MainStones.BLUE_NETHER_BRICKS)
                .define('N', Items.NETHER_BRICK).define('W', Items.WARPED_FUNGUS)
                .pattern("NW").pattern("WN")
                .unlockedBy(getHasName(Items.WARPED_FUNGUS), has(Items.WARPED_FUNGUS))
                .save(output);
        stoneVariants(MainStones.BLUE_NETHER_BRICKS.get(), MainStones.CHISELED_BLUE_NETHER_BRICKS, MainStones.POLISHED_BLUE_NETHER_BRICKS);
        stoneVariants(Blocks.NETHER_BRICKS, MainStones.TAINTED_NETHER_BRICKS);
        stoneVariants(Blocks.RED_NETHER_BRICKS, MainStones.CHISELED_RED_NETHER_BRICKS, MainStones.POLISHED_RED_NETHER_BRICKS);
        stoneVariants(Blocks.SOUL_SAND, MainStones.SOUL_BRICKS, MainStones.POLISHED_SOUL_SAND, MainStones.CHISELED_SOUL_BRICKS,
                MainStones.POSSESSED_CHISELED_SOUL_BRICKS);
        stoneVariants(Blocks.POLISHED_BASALT, MainStones.POLISHED_BASALT_BRICKS, MainStones.POLISHED_BASALT_TILES,
                MainStones.CUT_POLISHED_BASALT, MainStones.CHISELED_POLISHED_BASALT);
        MainStones.STAINED_STONE.forEach(stained -> stonecutting(RecipeCategory.BUILDING_BLOCKS, stained, Blocks.STONE, 1));
    }

    /** Variants of {@code base}, each cut from one of it in a stonecutter. */
    private void stoneVariants(Block base, DeferredBlock<?>... variants) {
        for (DeferredBlock<?> variant : variants) {
            stonecutting(RecipeCategory.BUILDING_BLOCKS, variant, base, 1);
        }
    }

    /** A pillar from two blocks in a column, like vanilla's quartz pillar. */
    private void pillar(Block pillar, Block block) {
        shaped(RecipeCategory.BUILDING_BLOCKS, pillar, 2)
                .pattern("#").pattern("#")
                .define('#', block)
                .unlockedBy(getHasName(block), has(block))
                .save(output);
    }

    /** A lantern from four paper around a torch with planks in the corners, like vanilla's lantern. */
    private void lantern(ItemLike lantern, ItemLike planks) {
        shaped(RecipeCategory.DECORATIONS, lantern)
                .pattern("#P#").pattern("PTP").pattern("#P#")
                .define('#', planks).define('P', Items.PAPER).define('T', Items.TORCH)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(output);
    }

    /** A material's storage block, bricks, pillar and the smelting of its ores. */
    private void minerals(Item material, Block block, Block bricks, Block pillar, Block ore, Block deepslateOre) {
        storageBlock(RecipeCategory.MISC, material, RecipeCategory.BUILDING_BLOCKS, block);
        bricks(bricks, block);
        pillar(pillar, block);
        smeltingAndBlasting(ore, RecipeCategory.MISC, material, 1.0F, 200);
        smeltingAndBlasting(deepslateOre, RecipeCategory.MISC, material, 1.0F, 200);
    }

    /** Four bricks from four blocks, like vanilla's stone bricks. */
    private void bricks(Block bricks, Block block) {
        shaped(RecipeCategory.BUILDING_BLOCKS, bricks, 4)
                .pattern("##").pattern("##")
                .define('#', block)
                .unlockedBy(getHasName(block), has(block))
                .save(output);
    }

    private void tools() {
        Item platinum = MainItems.PLATINUM.get();
        tool(MainItems.PLATINUM_SWORD.get(), platinum, "#", "#", "/");
        tool(MainItems.PLATINUM_PICKAXE.get(), platinum, "###", " / ", " / ");
        tool(MainItems.PLATINUM_AXE.get(), platinum, "##", "#/", " /");
        tool(MainItems.PLATINUM_SHOVEL.get(), platinum, "#", "/", "/");
        tool(MainItems.PLATINUM_HOE.get(), platinum, "##", " /", " /");
    }

    private void tool(Item tool, Item material, String... pattern) {
        var builder = shaped(tool == MainItems.PLATINUM_SWORD.get() ? RecipeCategory.COMBAT : RecipeCategory.TOOLS, tool);
        for (String row : pattern) {
            builder.pattern(row);
        }
        builder.define('#', material).define('/', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(output);
    }

    private void armor() {
        Item platinum = MainItems.PLATINUM.get();
        armor(MainItems.PLATINUM_HELMET.get(), platinum, "###", "# #");
        armor(MainItems.PLATINUM_CHESTPLATE.get(), platinum, "# #", "###", "###");
        armor(MainItems.PLATINUM_LEGGINGS.get(), platinum, "###", "# #", "# #");
        armor(MainItems.PLATINUM_BOOTS.get(), platinum, "# #", "# #");
    }

    private void armor(Item armor, Item material, String... pattern) {
        var builder = shaped(RecipeCategory.COMBAT, armor);
        for (String row : pattern) {
            builder.pattern(row);
        }
        builder.define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(output);
    }

    /** Stairs, slab, button, pressure plate, fence and gate from planks, as vanilla makes them. */
    private void planks(PlankSet planks) {
        generateRecipes(planks.family(), FeatureFlags.VANILLA_SET);
    }

    /** ORForged's vertical planks: three planks in a column make three. */
    private void verticalPlanks(ItemLike vertical, ItemLike planks) {
        shaped(RecipeCategory.BUILDING_BLOCKS, vertical, 3)
                .pattern("#").pattern("#").pattern("#")
                .define('#', planks)
                .unlockedBy(getHasName(planks), has(planks))
                .save(output);
    }

    /** Carved planks from two slabs, as vanilla makes chiseled blocks. */
    private void carvedPlanks(ItemLike carved, ItemLike slab) {
        chiseled(RecipeCategory.BUILDING_BLOCKS, carved, slab);
    }

    /** ORForged's beams: two logs side by side make two beams, and a beam turns back into four planks. */
    private void beam(ItemLike beam, ItemLike log, ItemLike planks) {
        shaped(RecipeCategory.BUILDING_BLOCKS, beam, 2)
                .pattern("##")
                .define('#', log)
                .unlockedBy(getHasName(log), has(log))
                .save(output);
        save(shapeless(RecipeCategory.BUILDING_BLOCKS, planks, 4).requires(beam)
                .unlockedBy(getHasName(beam), has(beam)), getItemName(planks) + "_from_" + getItemName(beam));
    }

    private static Block slabOf(Block planks) {
        return BlockFamilies.getAllFamilies()
                .filter(family -> family.getBaseBlock() == planks)
                .findFirst()
                .map(family -> family.get(BlockFamily.Variant.SLAB))
                .orElseThrow(() -> new IllegalStateException("No vanilla slab for " + planks));
    }
}
