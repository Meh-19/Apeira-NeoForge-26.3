package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraItemModelProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.registry.DecorItems;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;

/** Item models for {@link DecorItems}. */
public final class DecorItemModelProvider extends ApeiraItemModelProvider {
    @Override
    protected void registerModels() {
        // CosmoLib's Blockbench paintbrush (models/item/paintbrush.json); its paint drip is tint index 0.
        generators().itemModelOutput.accept(DecorItems.PAINTBRUSH.get(),
                ItemModelUtils.tintedModel(ModelLocationUtils.getModelLocation(DecorItems.PAINTBRUSH.get()), new PaintTintSource()));
    }
}
