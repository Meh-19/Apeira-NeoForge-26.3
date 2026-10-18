package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraLanguageProvider;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorEntities;

import net.minecraft.data.PackOutput;

/** English names for the decor module's blocks and entities. */
public final class DecorLanguageProvider extends ApeiraLanguageProvider {
    public DecorLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        block(DecorBlocks.OAK_CHAIR, "Oak Chair");

        addEntityType(DecorEntities.SEAT, "Seat");
    }
}
