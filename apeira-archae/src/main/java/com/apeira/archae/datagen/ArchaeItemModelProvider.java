package com.apeira.archae.datagen;

import com.apeira.archae.item.FragmentItem;
import com.apeira.archae.registry.ArchaeItems;
import com.apeira.core.datagen.ApeiraItemModelProvider;

import net.neoforged.neoforge.registries.DeferredItem;

/** Item models for {@link ArchaeItems}: flat sprites from {@code textures/item/archaeology/<name>.png}. */
public final class ArchaeItemModelProvider extends ApeiraItemModelProvider {
    @Override
    protected void registerModels() {
        for (DeferredItem<FragmentItem> fragment : ArchaeItems.all()) {
            simpleItem(fragment.get(), itemTexture("archaeology/" + fragment.getId().getPath()));
        }
    }
}
