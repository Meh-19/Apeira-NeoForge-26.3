package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorDataComponents;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Loot tables for {@link DecorBlocks}. Every decor block needs one, or data generation fails. */
public final class DecorBlockLootSubProvider extends ApeiraBlockLootProvider {
    public DecorBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        add(DecorBlocks.OAK_CHAIR.get(), this::dropSelfWithPaint);
        dropSelf(DecorBlocks.PAINTING_TABLE.get());
    }

    /** Drops a paintable block's item, carrying the block's paint. */
    private LootTable.Builder dropSelfWithPaint(Block block) {
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(block).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(DecorDataComponents.PAINT.get())))));
    }
}
