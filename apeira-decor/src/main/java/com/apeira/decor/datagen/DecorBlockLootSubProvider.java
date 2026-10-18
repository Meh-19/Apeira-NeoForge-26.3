package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.registry.DecorBlocks;

import net.minecraft.data.loot.LootTableSubProvider;

/** Loot tables for {@link DecorBlocks}. Every decor block needs one, or data generation fails. */
public final class DecorBlockLootSubProvider extends ApeiraBlockLootProvider {
    public DecorBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(DecorBlocks.OAK_CHAIR.get());
    }
}
