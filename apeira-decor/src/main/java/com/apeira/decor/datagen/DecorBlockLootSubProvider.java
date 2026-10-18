package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorDataComponents;
import com.apeira.decor.registry.DecorFurniture;

import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Loot tables for {@link DecorBlocks} and {@link DecorFurniture}; paintable furniture keeps its paint. Every decor block needs one, or data generation fails. */
public final class DecorBlockLootSubProvider extends ApeiraBlockLootProvider {
    public DecorBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(DecorBlocks.PAINTING_TABLE.get());
        for (DeferredBlock<FurnitureBlock> furniture : DecorFurniture.all()) {
            if (furniture.get().furniture().paintable()) {
                add(furniture.get(), this::dropSelfWithPaint);
            } else {
                dropSelf(furniture.get());
            }
        }
    }

    /** Drops a paintable block's item, carrying the block's paint. */
    private LootTable.Builder dropSelfWithPaint(Block block) {
        return LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ContextIntProviders.exactly(1))
                .add(LootItem.lootTableItem(block).apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                        .include(DecorDataComponents.PAINT.get())))));
    }
}
