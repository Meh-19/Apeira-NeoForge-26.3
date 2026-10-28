package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraRecipeProvider;
import com.apeira.core.registry.PlankSet;
import com.apeira.core.registry.ShapeSet;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainMaterials;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainRubies;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
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
 * says, Xenon's blocks as {@link #xenon()} says, and lanterns like vanilla's.
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
        shaped(RecipeCategory.DECORATIONS, MainRubies.RUBY_DEPOSIT)
                .define('G', Items.GOLD_INGOT).define('O', Blocks.OBSIDIAN).define('R', MainItems.RUBY)
                .pattern("GGG").pattern("ORO").pattern("GOG")
                .unlockedBy(getHasName(MainItems.RUBY), has(MainItems.RUBY))
                .save(output);

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
        xenon();
        turkey();
        for (MainLanterns.Lantern lantern : MainLanterns.all()) {
            lantern(lantern.block(), lantern.planks());
        }
    }

    /** Xenon's turkey recipes: raw turkey cooked in a furnace, a smoker or on a campfire. */
    private void turkey() {
        Ingredient raw = Ingredient.of(MainMaterials.RAW_TURKEY.get());
        String name = getItemName(MainMaterials.COOKED_TURKEY.get());
        save(SimpleCookingRecipeBuilder.smelting(raw, RecipeCategory.FOOD, CookingBookCategory.MISC, MainMaterials.COOKED_TURKEY.get(), 0.35F, 200)
                .group("turkey").unlockedBy(getHasName(MainMaterials.RAW_TURKEY.get()), has(MainMaterials.RAW_TURKEY.get())),
                name + "_from_smelting_raw_turkey");
        save(SimpleCookingRecipeBuilder.smoking(raw, RecipeCategory.FOOD, MainMaterials.COOKED_TURKEY.get(), 0.35F, 100)
                .group("turkey").unlockedBy(getHasName(MainMaterials.RAW_TURKEY.get()), has(MainMaterials.RAW_TURKEY.get())),
                name + "_from_smoking_raw_turkey");
        save(SimpleCookingRecipeBuilder.campfireCooking(raw, RecipeCategory.FOOD, MainMaterials.COOKED_TURKEY.get(), 0.35F, 600)
                .group("turkey").unlockedBy(getHasName(MainMaterials.RAW_TURKEY.get()), has(MainMaterials.RAW_TURKEY.get())),
                name + "_from_campfire_raw_turkey");
    }

    /**
     * Xenon's stone families, with Xenon's recipes: every block of a family is cut in a stonecutter from the family's
     * stone (two slabs from one), stairs, slabs and walls are also crafted from their block, bricks are cut from the
     * polished stone, cracked bricks are smelted bricks, and mossy blocks are their block with a vine. Xenon had no
     * recipes for its other blocks; like ORForged's stones, its concrete bricks, shingles, packed snow, packed ashen
     * sand and polished nether bricks are cut from the vanilla block they are made of, quilted wool is crafted from four
     * wool like bricks, and ashen sandstone is made like sandstone. Xenon's natural stones (rhyolite, kyanite, grimrock,
     * packed loam, strata, dolomite, gravestone, scoria, ashen sand, loam and permafrost) have none, and nor do its
     * crystals, crates, baskets, bundles, bone, moss and grimweed blocks, shoji and salvaging table.
     */
    private void xenon() {
        cut(MainStones.LIMESTONE.get(), MainRocks.LIMESTONE_SHAPES, MainRocks.POLISHED_LIMESTONE_SHAPES, MainRocks.LIMESTONE_BRICKS_SHAPES,
                MainRocks.SANDY_LIMESTONE_BRICKS_SHAPES);
        cut(MainRocks.RHYOLITE.get(), MainRocks.MOSSY_RHYOLITE, MainRocks.RHYOLITE_PILLAR, MainRocks.RHYOLITE_BRICKS, MainRocks.CHISELED_RHYOLITE,
                MainRocks.CRACKED_RHYOLITE_BRICKS, MainRocks.MOSSY_RHYOLITE_BRICKS);
        cut(MainRocks.RHYOLITE.get(), MainRocks.RHYOLITE_SHAPES, MainRocks.MOSSY_RHYOLITE_SHAPES, MainRocks.RHYOLITE_BRICKS_SHAPES,
                MainRocks.CRACKED_RHYOLITE_BRICKS_SHAPES, MainRocks.MOSSY_RHYOLITE_BRICKS_SHAPES);
        cracked(MainRocks.CRACKED_RHYOLITE_BRICKS, MainRocks.RHYOLITE_BRICKS);
        mossy(MainRocks.MOSSY_RHYOLITE_BRICKS, MainRocks.RHYOLITE_BRICKS);
        mossy(MainRocks.MOSSY_RHYOLITE, MainRocks.RHYOLITE);
        cut(MainRocks.KYANITE.get(), MainRocks.POLISHED_KYANITE, MainRocks.KYANITE_BRICKS, MainRocks.CHISELED_KYANITE);
        cut(MainRocks.KYANITE.get(), MainRocks.KYANITE_SHAPES, MainRocks.POLISHED_KYANITE_SHAPES, MainRocks.KYANITE_BRICKS_SHAPES);
        cut(MainRocks.POLISHED_KYANITE.get(), MainRocks.KYANITE_BRICKS);
        cut(MainRocks.GRIMROCK.get(), MainRocks.POLISHED_GRIMROCK, MainRocks.GRIMROCK_BRICKS, MainRocks.CHISELED_GRIMROCK,
                MainRocks.CRACKED_GRIMROCK_BRICKS, MainRocks.MOSSY_GRIMROCK_BRICKS, MainRocks.COBBLED_GRIMROCK);
        cut(MainRocks.GRIMROCK.get(), MainRocks.GRIMROCK_SHAPES, MainRocks.POLISHED_GRIMROCK_SHAPES, MainRocks.GRIMROCK_BRICKS_SHAPES,
                MainRocks.CRACKED_GRIMROCK_BRICKS_SHAPES, MainRocks.MOSSY_GRIMROCK_BRICKS_SHAPES, MainRocks.COBBLED_GRIMROCK_SHAPES);
        cut(MainRocks.POLISHED_GRIMROCK.get(), MainRocks.GRIMROCK_BRICKS);
        cracked(MainRocks.CRACKED_GRIMROCK_BRICKS, MainRocks.GRIMROCK_BRICKS);
        mossy(MainRocks.MOSSY_GRIMROCK_BRICKS, MainRocks.GRIMROCK_BRICKS);
        cut(MainRocks.PACKED_LOAM.get(), MainRocks.PACKED_LOAM_BRICKS, MainRocks.CHISELED_PACKED_LOAM, MainRocks.MOSSY_PACKED_LOAM_BRICKS);
        cut(MainRocks.PACKED_LOAM.get(), MainRocks.PACKED_LOAM_SHAPES, MainRocks.PACKED_LOAM_BRICKS_SHAPES, MainRocks.MOSSY_PACKED_LOAM_BRICKS_SHAPES);
        mossy(MainRocks.MOSSY_PACKED_LOAM_BRICKS, MainRocks.PACKED_LOAM_BRICKS);
        cut(MainStones.DRIED_SILT.get(), MainRocks.DRIED_SILT_SHAPES, MainRocks.POLISHED_SILT_SHAPES, MainRocks.SILT_BRICKS_SHAPES);
        cut(MainStones.SLATE.get(), MainRocks.SLATE_SHAPES, MainRocks.POLISHED_SLATE_SHAPES, MainRocks.SLATE_BRICKS_SHAPES);
        cut(MainRocks.STRATA.get(), MainRocks.POLISHED_STRATA, MainRocks.STRATA_BRICKS, MainRocks.CHISELED_STRATA);
        cut(MainRocks.STRATA.get(), MainRocks.STRATA_SHAPES, MainRocks.POLISHED_STRATA_SHAPES, MainRocks.STRATA_BRICKS_SHAPES);
        cut(MainRocks.POLISHED_STRATA.get(), MainRocks.STRATA_BRICKS);
        cut(MainRocks.DOLOMITE.get(), MainRocks.DOLOMITE_PILLAR, MainRocks.POLISHED_DOLOMITE, MainRocks.DOLOMITE_BRICKS, MainRocks.CHISELED_DOLOMITE,
                MainRocks.POROUS_DOLOMITE);
        cut(MainRocks.DOLOMITE.get(), MainRocks.DOLOMITE_SHAPES, MainRocks.POLISHED_DOLOMITE_SHAPES, MainRocks.DOLOMITE_BRICKS_SHAPES);
        cut(MainRocks.POLISHED_DOLOMITE.get(), MainRocks.DOLOMITE_BRICKS);
        cut(MainRocks.GRAVESTONE.get(), MainRocks.POLISHED_GRAVESTONE, MainRocks.GRAVESTONE_BRICKS, MainRocks.CHISELED_GRAVESTONE);
        cut(MainRocks.GRAVESTONE.get(), MainRocks.GRAVESTONE_SHAPES, MainRocks.POLISHED_GRAVESTONE_SHAPES, MainRocks.GRAVESTONE_BRICKS_SHAPES);
        cut(MainRocks.POLISHED_GRAVESTONE.get(), MainRocks.GRAVESTONE_BRICKS);
        cut(MainRocks.SCORIA.get(), MainRocks.SCORIA_BRICKS, MainRocks.CHISELED_SCORIA, MainRocks.MOLTEN_SCORIA, MainRocks.CHISELED_MOLTEN_SCORIA,
                MainRocks.MOLTEN_SCORIA_BRICKS);
        cut(MainRocks.SCORIA.get(), MainRocks.SCORIA_BRICKS_SHAPES, MainRocks.MOLTEN_SCORIA_BRICKS_SHAPES);
        cut(Blocks.DRIPSTONE_BLOCK, MainRocks.POLISHED_DRIPSTONE_SHAPES, MainRocks.DRIPSTONE_BRICKS_SHAPES);
        cut(Blocks.POLISHED_BASALT, MainRocks.POLISHED_BASALT_BRICKS_SHAPES, MainRocks.POLISHED_BASALT_TILES_SHAPES,
                MainRocks.CUT_POLISHED_BASALT_SHAPES);
        cut(MainStones.BLUE_NETHER_BRICKS.get(), MainRocks.BLUE_NETHER_BRICKS_SHAPES);
        cut(MainStones.TAINTED_NETHER_BRICKS.get(), MainRocks.TAINTED_NETHER_BRICKS_SHAPES);
        cut(Blocks.SOUL_SAND, MainRocks.POLISHED_SOUL_SAND_SHAPES, MainRocks.SOUL_BRICKS_SHAPES);
        cut(MainRocks.PACKED_SNOW.get(), MainRocks.SNOW_BRICKS, MainRocks.CHISELED_SNOW_BRICKS);
        cut(MainRocks.PACKED_SNOW.get(), MainRocks.PACKED_SNOW_SHAPES, MainRocks.SNOW_BRICKS_SHAPES);
        cut(Blocks.PACKED_ICE, MainRocks.POLISHED_PACKED_ICE, MainRocks.PACKED_ICE_BRICKS, MainRocks.CHISELED_PACKED_ICE_BRICKS);
        cut(Blocks.PACKED_ICE, MainRocks.POLISHED_PACKED_ICE_SHAPES, MainRocks.PACKED_ICE_BRICKS_SHAPES);
        cut(MainRocks.POLISHED_PACKED_ICE.get(), MainRocks.PACKED_ICE_BRICKS);
        cut(Blocks.PRISMARINE, MainRocks.PRISMARINE_PILLAR, MainRocks.POLISHED_PRISMARINE, MainRocks.PRISMARINE_TILES, MainRocks.CHISELED_PRISMARINE);
        cut(Blocks.PRISMARINE, MainRocks.POLISHED_PRISMARINE_SHAPES, MainRocks.PRISMARINE_TILES_SHAPES);
        MainRocks.shapes().forEach(shapes -> generateRecipes(shapes.family(), FeatureFlags.VANILLA_SET));

        // Xenon's blocks without recipes of their own, made like vanilla's.
        cut(Blocks.SNOW_BLOCK, MainRocks.PACKED_SNOW);
        cut(Blocks.NETHER_BRICKS, MainRocks.POLISHED_NETHER_BRICKS);
        cut(MainRocks.ASHEN_SAND.get(), MainRocks.PACKED_ASHEN_SAND);
        bricks(MainRocks.ASHEN_SANDSTONE.get(), MainRocks.ASHEN_SAND.get());
        smeltingResultFromBase(MainRocks.SMOOTH_ASHEN_SANDSTONE, MainRocks.ASHEN_SANDSTONE);
        cut(MainRocks.ASHEN_SANDSTONE.get(), MainRocks.CUT_ASHEN_SANDSTONE, MainRocks.CHISELED_ASHEN_SANDSTONE);
        for (MainDyedBlocks.Dyed dyed : MainDyedBlocks.CONCRETE_BRICKS) {
            Block concrete = vanillaBlock(dyed.colour().getSerializedName() + "_concrete");
            cut(concrete, dyed.block());
            cut(concrete, dyed.shapes());
        }
        cut(Blocks.TERRACOTTA, MainDyedBlocks.TERRACOTTA_SHINGLES);
        cut(Blocks.TERRACOTTA, MainDyedBlocks.TERRACOTTA_SHINGLES_SHAPES);
        for (MainDyedBlocks.Dyed dyed : MainDyedBlocks.TERRACOTTA_SHINGLES_COLOURS) {
            Block terracotta = vanillaBlock(dyed.colour().getSerializedName() + "_terracotta");
            cut(terracotta, dyed.block());
            cut(terracotta, dyed.shapes());
        }
        MainDyedBlocks.shapes().forEach(shapes -> generateRecipes(shapes.family(), FeatureFlags.VANILLA_SET));
        for (MainDyedBlocks.Dyed dyed : MainDyedBlocks.QUILTED_WOOL) {
            bricks(dyed.block().get(), vanillaBlock(dyed.colour().getSerializedName() + "_wool"));
        }
    }

    /** Cuts each of {@code blocks} from {@code input} in a stonecutter. */
    private void cut(ItemLike input, DeferredBlock<?>... blocks) {
        for (DeferredBlock<?> block : blocks) {
            stonecutting(RecipeCategory.BUILDING_BLOCKS, block, input, 1);
        }
    }

    /** Cuts the stairs, slab and wall of each of {@code sets} from {@code input}: two slabs from one block. */
    private void cut(ItemLike input, ShapeSet... sets) {
        for (ShapeSet set : sets) {
            for (DeferredBlock<? extends Block> shape : set.shapes()) {
                stonecutting(RecipeCategory.BUILDING_BLOCKS, shape, input, shape == set.slab() ? 2 : 1);
            }
        }
    }

    /** Cracked bricks are smelted bricks, as in vanilla. */
    private void cracked(ItemLike cracked, ItemLike bricks) {
        smeltingResultFromBase(cracked, bricks);
    }

    /** A mossy block is its block with a vine, as in vanilla. */
    private void mossy(ItemLike mossy, ItemLike block) {
        save(shapeless(RecipeCategory.BUILDING_BLOCKS, mossy).requires(block).requires(Blocks.VINE)
                .unlockedBy(getHasName(Blocks.VINE), has(Blocks.VINE)), getItemName(mossy) + "_from_vine");
    }

    private static Block vanillaBlock(String name) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(name));
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
