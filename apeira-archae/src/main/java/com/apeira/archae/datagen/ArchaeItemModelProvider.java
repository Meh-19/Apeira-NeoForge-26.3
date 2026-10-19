package com.apeira.archae.datagen;

import com.apeira.archae.item.FragmentItem;
import com.apeira.archae.registry.ArchaeCosmetics;
import com.apeira.archae.registry.ArchaeItems;
import com.apeira.core.datagen.ApeiraItemModelProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.client.WornCosmetic;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Item models for {@link ArchaeItems}: flat sprites from {@code textures/item/archaeology/<name>.png}; and item
 * definitions for {@link ArchaeCosmetics}.
 */
public final class ArchaeItemModelProvider extends ApeiraItemModelProvider {
    @Override
    protected void registerModels() {
        for (DeferredItem<FragmentItem> fragment : ArchaeItems.all()) {
            simpleItem(fragment.get(), itemTexture("archaeology/" + fragment.getId().getPath()));
        }
        ArchaeCosmetics.all().forEach(this::cosmetic);
    }

    /**
     * A cosmetic shows its rarity token ({@code item/<rarity>_<slot>_token}, apeira-decor's) everywhere except when
     * worn, where the {@code apeira:worn_cosmetic} condition shows Argon's 3-D model ({@code item/cosmetic/<name>}).
     * Tint index 1 of the model is the paint, as in Argon; tint index 0 keeps its texture.
     */
    private void cosmetic(ArchaeCosmetics.Cosmetic cosmetic) {
        ItemModel.Unbaked token = ItemModelUtils.plainModel(modLoc("item/" + cosmetic.rarity().id() + "_"
                + cosmetic.slot().getSerializedName() + "_token"));
        ItemModel.Unbaked worn = ItemModelUtils.tintedModel(modLoc("item/cosmetic/" + cosmetic.item().getId().getPath()),
                ItemModelUtils.constantTint(-1), new PaintTintSource());
        generators().itemModelOutput.accept(cosmetic.item().get(), ItemModelUtils.conditional(new WornCosmetic(), worn, token));
    }
}
