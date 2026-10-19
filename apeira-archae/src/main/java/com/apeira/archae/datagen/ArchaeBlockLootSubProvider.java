package com.apeira.archae.datagen;

import com.apeira.archae.block.ArtifactBlock;
import com.apeira.archae.registry.ArchaeArtifacts;
import com.apeira.archae.registry.ArchaeBlocks;
import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Loot tables for {@link ArchaeBlocks} and {@link ArchaeArtifacts}. An artifact drops its own item, or a wall, ceiling
 * or water variant its base artifact's, carrying the block's paint, as Argon's {@code afterBreak} dropped it.
 */
public final class ArchaeBlockLootSubProvider extends ApeiraBlockLootProvider {
    public ArchaeBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(ArchaeBlocks.ASSEMBLY_TABLE.get());
        for (ArchaeArtifacts.Artifact artifact : ArchaeArtifacts.all()) {
            ArtifactBlock block = artifact.block().get();
            add(block, LootTable.lootTable().withPool(applyExplosionCondition(block.dropBlock(), LootPool.lootPool()
                    .setRolls(ContextIntProviders.exactly(1))
                    .add(LootItem.lootTableItem(block.dropBlock()).apply(CopyComponentsFunction
                            .copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DecorDataComponents.PAINT.get()))))));
        }
    }
}
