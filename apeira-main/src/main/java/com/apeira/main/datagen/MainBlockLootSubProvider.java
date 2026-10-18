package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockLootProvider;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;

import net.minecraft.data.loot.LootTableSubProvider;

/** Loot tables for {@link MainBlocks} and {@link MainCrates}. Every Apeira block needs one, or data generation fails. */
public final class MainBlockLootSubProvider extends ApeiraBlockLootProvider {
    public MainBlockLootSubProvider(LootTableSubProvider.Context context, ApeiraRegistrar registrar) {
        super(context, registrar);
    }

    @Override
    protected void generate() {
        dropSelf(MainBlocks.CARVED_APEIRA_STONE.get());
        dropSelf(MainBlocks.APEIRA_BRICK.get());
        // A closed crate drops itself; an open one cannot be broken.
        MainCrates.all().forEach(crate -> dropSelf(crate.get()));
    }
}
