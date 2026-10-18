package com.apeira.core.datagen;

import java.util.Set;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.core.registry.ShapeSet;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Base for one module's block loot tables, written to {@code data/<namespace>/loot_table/blocks/<block>.json}.
 *
 * <p>Loot tables are datapack registry entries in Minecraft 26.x, like recipes: a loot sub-provider is run by a
 * {@code LootTableProvider} bootstrap from {@code GatherDataRegistryEntriesEvent}. Register subclasses with
 * {@link ApeiraDataGenerators#blockLoot}, which passes the sub-provider context and the module's registrar, so a
 * constructor reference works:
 *
 * <pre>{@code
 * public final class DecorBlockLoot extends ApeiraBlockLootProvider {
 *     public DecorBlockLoot(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
 *         super(context, registrar);
 *     }
 *
 *     @Override
 *     protected void generate() {
 *         dropSelf(DecorBlocks.OAK_CHAIR.get());
 *     }
 * }
 * }</pre>
 *
 * <p>Only the registrar's blocks are checked, so every one of them needs a table (use {@link #noDrop()} for blocks
 * that drop nothing), and other modules' blocks are left to their own providers.
 */
public abstract class ApeiraBlockLootProvider extends BlockLootSubProvider {
    /** Vanilla's chance of sticks from leaves at each Fortune level. */
    private static final float[] LEAVES_STICK_CHANCES = {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

    private final ApeiraRegistrar registrar;

    protected ApeiraBlockLootProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        // No block is explosion resistant: every drop must survive the explosion that broke the block.
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
        this.registrar = registrar;
    }

    /**
     * Leaves of a tree with no sapling: themselves with shears or Silk Touch, otherwise sometimes one or two sticks,
     * with vanilla's chances.
     */
    protected LootTable.Builder createLeavesDropsWithoutSapling(Block leaves) {
        return createShearsOrSilkTouchOnlyDrop(leaves).withPool(LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .when(new AnyOfCondition.Builder().or(hasShears()).or(hasSilkTouch()).invert())
                .add(applyExplosionDecay(leaves, LootItem.lootTableItem(Items.STICK)
                        .apply(SetItemCountFunction.setCount(ContextIntProviders.between(1, 2))))
                        .when(BonusLevelTableCondition.bonusLevelFlatChance(enchantments.getOrThrow(Enchantments.FORTUNE),
                                LEAVES_STICK_CHANCES))));
    }

    /** The shapes of a {@link ShapeSet} drop themselves; a double slab drops two slabs. */
    protected void dropShapes(ShapeSet shapes) {
        if (shapes.stairs() != null) {
            dropSelf(shapes.stairs().get());
        }
        if (shapes.slab() != null) {
            add(shapes.slab().get(), createSlabItemTable(shapes.slab().get()));
        }
        if (shapes.wall() != null) {
            dropSelf(shapes.wall().get());
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return registrar.blocks().getEntries().stream().<Block>map(DeferredHolder::value).toList();
    }
}
