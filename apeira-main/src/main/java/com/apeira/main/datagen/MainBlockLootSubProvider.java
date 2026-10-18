package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.registry.MainBlocks;

import net.minecraft.data.loot.LootTableSubProvider;

/** Loot tables for {@link MainBlocks}. Every Apeira block needs one, or data generation fails. */
public final class MainBlockLootSubProvider extends ApeiraBlockLootProvider {
    public MainBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(MainBlocks.CARVED_APEIRA_STONE.get());
        dropSelf(MainBlocks.APEIRA_BRICK.get());
    }
}
