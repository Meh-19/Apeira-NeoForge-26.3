package com.apeira.core.datagen;

import java.util.Set;

import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
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
    private final ApeiraRegistrar registrar;

    protected ApeiraBlockLootProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        // No block is explosion resistant: every drop must survive the explosion that broke the block.
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), context);
        this.registrar = registrar;
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return registrar.blocks().getEntries().stream().<Block>map(DeferredHolder::value).toList();
    }
}
