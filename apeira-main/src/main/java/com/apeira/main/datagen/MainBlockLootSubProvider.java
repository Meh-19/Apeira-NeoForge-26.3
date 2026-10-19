package com.apeira.main.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.PlankSet;
import com.apeira.main.block.DecorationBlock;
import com.apeira.main.block.HangingVineBlock;
import com.apeira.main.block.HangingVinePlantBlock;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainBuildingBlocks;
import com.apeira.main.registry.MainCrates;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainDyedBlocks;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainLanterns;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainSpecialBlocks;
import com.apeira.main.registry.MainStones;
import com.apeira.main.registry.MainWoods;

import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Loot tables for {@link MainBlocks}, {@link MainWoods}, {@link MainStones}, {@link MainLanterns}, Xenon's blocks
 * ({@link MainRocks}, {@link MainDyedBlocks}, {@link MainCrystals}, {@link MainBuildingBlocks}, {@link MainPlants},
 * {@link MainSpecialBlocks}) and {@link MainCrates}.
 * Every Apeira block needs one, or data generation fails.
 */
public final class MainBlockLootSubProvider extends ApeiraBlockLootProvider {
    public MainBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(MainBlocks.PLATINUM_BLOCK.get());
        dropSelf(MainBlocks.PLATINUM_BRICKS.get());
        dropSelf(MainBlocks.PLATINUM_PILLAR.get());
        dropSelf(MainBlocks.RUBY_BLOCK.get());
        dropSelf(MainBlocks.RUBY_BRICKS.get());
        dropSelf(MainBlocks.RUBY_PILLAR.get());
        for (MainBlocks.Mineral mineral : MainBlocks.vanillaMinerals()) {
            dropSelf(mineral.brick().get());
            dropSelf(mineral.pillar().get());
        }
        // Ores drop their material, more with Fortune, or themselves with Silk Touch.
        add(MainBlocks.PLATINUM_ORE.get(), createOreDrop(MainBlocks.PLATINUM_ORE.get(), MainItems.PLATINUM.get()));
        add(MainBlocks.DEEPSLATE_PLATINUM_ORE.get(), createOreDrop(MainBlocks.DEEPSLATE_PLATINUM_ORE.get(), MainItems.PLATINUM.get()));
        add(MainBlocks.RUBY_ORE.get(), createOreDrop(MainBlocks.RUBY_ORE.get(), MainItems.RUBY.get()));
        add(MainBlocks.DEEPSLATE_RUBY_ORE.get(), createOreDrop(MainBlocks.DEEPSLATE_RUBY_ORE.get(), MainItems.RUBY.get()));

        for (MainWoods.Wood wood : MainWoods.woods()) {
            wood.logBlocks().forEach(log -> dropSelf(log.get()));
            planks(wood.planks());
            dropSelf(wood.verticalPlanks().get());
        }
        // Maple and fir leaves drop Xenon's saplings, at Xenon's flat 5% (Fortune does not raise it). ORForged's other
        // trees have no saplings: their leaves and wisteria's flowers drop sticks, or themselves with shears.
        Map<DeferredBlock<?>, SaplingBlock> saplings = new HashMap<>();
        MainWoods.MAPLE.leaves().forEach(leaves -> saplings.put(leaves, MainWoods.MAPLE_SAPLING.get()));
        MainWoods.FIR.leaves().forEach(leaves -> saplings.put(leaves, MainWoods.FIR_SAPLING.get()));
        for (DeferredBlock<LeavesBlock> leaves : MainWoods.allLeaves()) {
            SaplingBlock sapling = saplings.get(leaves);
            add(leaves.get(), sapling != null ? createLeavesDrops(leaves.get(), sapling, 0.05F) : createLeavesDropsWithoutSapling(leaves.get()));
        }
        MainWoods.saplings().forEach(sapling -> dropSelf(sapling.get()));
        dropSelf(MainWoods.PALM_CROWN.get());
        MainWoods.fungalPlankSets().forEach(this::planks);
        dropSelf(MainWoods.VERTICAL_FUNGAL_PLANKS.get());
        MainWoods.carvedPlanks().forEach(carved -> dropSelf(carved.get()));
        MainWoods.beams().forEach(beam -> dropSelf(beam.get()));
        MainWoods.VANILLA_VERTICAL_PLANKS.forEach(vertical -> dropSelf(vertical.vertical().get()));
        MainStones.all().forEach(stone -> dropSelf(stone.get()));
        MainLanterns.all().forEach(lantern -> dropSelf(lantern.block().get()));

        // Xenon's blocks drop themselves, double slabs two slabs; permafrost drops dirt without Silk Touch, as in Xenon.
        Set<Block> shapes = new HashSet<>();
        MainRocks.shapes().forEach(set -> set.shapes().forEach(shape -> shapes.add(shape.get())));
        for (var rock : MainRocks.all()) {
            if (rock == MainRocks.PERMAFROST) {
                add(rock.get(), createSingleItemTableWithSilkTouch(rock.get(), Blocks.DIRT));
            } else if (!shapes.contains(rock.get())) {
                dropSelf(rock.get());
            }
        }
        MainRocks.shapes().forEach(this::dropShapes);
        MainDyedBlocks.CONCRETE_BRICKS.forEach(dyed -> dropSelf(dyed.block().get()));
        dropSelf(MainDyedBlocks.TERRACOTTA_SHINGLES.get());
        MainDyedBlocks.TERRACOTTA_SHINGLES_COLOURS.forEach(dyed -> dropSelf(dyed.block().get()));
        MainDyedBlocks.QUILTED_WOOL.forEach(dyed -> dropSelf(dyed.block().get()));
        MainDyedBlocks.shapes().forEach(this::dropShapes);
        MainCrystals.all().forEach(crystal -> dropSelf(crystal.get()));
        MainBuildingBlocks.all().forEach(block -> dropSelf(block.get()));

        xenonPlants();

        // A closed crate drops itself; an open one cannot be broken.
        MainCrates.all().forEach(crate -> dropSelf(crate.get()));
    }

    /**
     * Xenon's plants drop only to shears (Xenon's intent: its {@code dropsWithShears} tables were never added), vines
     * their one item ({@link HangingVineBlock#item()}; the moss hangs drop the moss cover without shears, like the cover), the twig a stick, rocks and pebbles
     * one item per stone, and the covers themselves, except frost, which drops nothing as in Xenon. Placed turkeys and
     * their skeleton drop nothing; echo glass needs Silk Touch, like glass.
     */
    private void xenonPlants() {
        List<Block> shearsOnly = new ArrayList<>();
        MainPlants.grasses().forEach(plant -> shearsOnly.add(plant.get()));
        MainPlants.litter().forEach(litter -> shearsOnly.add(litter.get()));
        shearsOnly.addAll(List.of(MainPlants.OCULUS_ORCHID.get(), MainPlants.HIBISCUS.get(), MainPlants.CLOVER.get(), MainPlants.SMALL_LILY.get(),
                MainPlants.ORANGE_MYCENA.get(), MainPlants.TALL_ORANGE_MYCENA.get()));
        shearsOnly.forEach(block -> add(block, createShearsOnlyDrop(block)));
        for (var vine : MainPlants.vines()) {
            ItemLike item = vine.get() instanceof HangingVineBlock tip ? tip.item() : ((HangingVinePlantBlock) vine.get()).item();
            boolean moss = item == MainPlants.GRIMOSS.get() || item == MainPlants.DAMP_MOSS.get();
            add(vine.get(), moss ? createSingleItemTable(item) : createShearsOnlyDrop(item));
        }
        add(MainPlants.CATTAIL.get(), createDoublePlantShearsDrop(MainPlants.CATTAIL.get()));
        MainPlants.aubrietas().forEach(aubrieta -> dropSelf(aubrieta.get()));
        add(MainPlants.TWIG.get(), createSingleItemTable(Items.STICK));
        add(MainPlants.ROCK.get(), createCountDrops(MainPlants.ROCK.get()));
        add(MainPlants.PEBBLES.get(), createCountDrops(MainPlants.PEBBLES.get()));
        dropSelf(MainPlants.ASH.get());
        dropSelf(MainPlants.DAMP_MOSS.get());
        dropSelf(MainPlants.GRIMOSS.get());
        add(MainPlants.FROST.get(), noDrop());

        dropSelf(MainSpecialBlocks.SPIKE_TRAP.get());
        add(MainSpecialBlocks.YELLOW_BIRCH_LEAVES.get(), createLeavesDrops(MainSpecialBlocks.YELLOW_BIRCH_LEAVES.get(), Blocks.BIRCH_SAPLING,
                NORMAL_LEAVES_SAPLING_CHANCES));
        add(MainSpecialBlocks.ECHO_GLASS.get(), createSilkTouchOnlyTable(MainSpecialBlocks.ECHO_GLASS.get()));
        dropSelf(MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get());
        dropSelf(MainSpecialBlocks.STRIPPED_MYCENA_MUSHROOM_STEM.get());
        dropSelf(MainSpecialBlocks.MYCENA_MUSHROOM_BLOCK.get());
        add(MainSpecialBlocks.RAW_TURKEY_BLOCK.get(), noDrop());
        add(MainSpecialBlocks.COOKED_TURKEY_BLOCK.get(), noDrop());
        add(MainSpecialBlocks.TURKEY_SKELETON.get(), noDrop());
    }

    /** One item for each of a {@link DecorationBlock}'s stones, like candles. */
    private LootTable.Builder createCountDrops(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ContextIntProviders.exactly(1))
                .add((LootPoolEntryContainer.Builder<?>) applyExplosionDecay(block, LootItem.lootTableItem(block).apply(List.of(2, 3),
                        count -> SetItemCountFunction.setCount(ContextIntProviders.exactly(count)).when(MatchBlock.blockMatches(blocks, block,
                                StatePropertiesPredicate.Builder.properties().hasProperty(DecorationBlock.COUNT, count.intValue())))))));
    }

    /** Planks and their shapes drop themselves; a double slab drops two slabs. */
    private void planks(PlankSet planks) {
        planks.all().forEach(block -> {
            if (block == planks.slab()) {
                add(block.get(), createSlabItemTable(block.get()));
            } else {
                dropSelf(block.get());
            }
        });
    }
}
