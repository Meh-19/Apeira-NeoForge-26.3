package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainItems;

import net.minecraft.data.PackOutput;

/** English names for Apeira's blocks and items. */
public final class MainLanguageProvider extends ApeiraLanguageProvider {
    public MainLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        block(MainBlocks.CARVED_APEIRA_STONE, "Carved Apeira Stone");
        block(MainBlocks.APEIRA_BRICK, "Apeira Brick");

        item(MainItems.APEIRA_INGOT, "Apeira Ingot");
        item(MainItems.APEIRA_GEM, "Apeira Gem");
        item(MainItems.APEIRA_RATION, "Apeira Ration");
        item(MainItems.EMBER_SHARD, "Ember Shard");
        item(MainItems.MOSS_CLUMP, "Moss Clump");
    }
}
