package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraItemModelProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.client.WornCosmetic;
import com.apeira.main.registry.MainCosmetics;
import com.apeira.main.registry.MainItems;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;

/**
 * Item models for {@link MainItems}, each a flat sprite from {@code item/<name>} (tools held like a sword, armour
 * showing its trims), and for the cosmetics.
 *
 * <p>Core's {@code ApeiraModelProvider} is the final vanilla model provider that runs every module's model providers,
 * so module item models extend {@link ApeiraItemModelProvider} instead.
 */
public final class MainModelProvider extends ApeiraItemModelProvider {
    @Override
    protected void registerModels() {
        simpleItem(MainItems.PLATINUM);
        simpleItem(MainItems.RUBY);
        MainItems.tools().forEach(this::toolItem);
        trimmableArmor(MainItems.PLATINUM_HELMET, ItemModelGenerators.TRIM_PREFIX_HELMET);
        trimmableArmor(MainItems.PLATINUM_CHESTPLATE, ItemModelGenerators.TRIM_PREFIX_CHESTPLATE);
        trimmableArmor(MainItems.PLATINUM_LEGGINGS, ItemModelGenerators.TRIM_PREFIX_LEGGINGS);
        trimmableArmor(MainItems.PLATINUM_BOOTS, ItemModelGenerators.TRIM_PREFIX_BOOTS);

        MainCosmetics.all().forEach(this::cosmetic);
    }

    /**
     * A cosmetic shows its rarity token ({@code item/<rarity>_<slot>_token}, CosmoLib's) everywhere (inventories, hands,
     * item frames, the ground) except when worn, where the {@code apeira:worn_cosmetic} condition shows its 3-D model
     * ({@code item/cosmetic/<name>}) instead. Tint index 1 of the model is the paint, as in CosmoLib; tint index 0 keeps its texture.
     */
    private void cosmetic(MainCosmetics.Cosmetic cosmetic) {
        ItemModel.Unbaked token = ItemModelUtils.plainModel(modLoc("item/" + cosmetic.rarity().id() + "_"
                + cosmetic.slot().getSerializedName() + "_token"));
        if (!cosmetic.modelled()) {
            generators().itemModelOutput.accept(cosmetic.item().get(), token);
            return;
        }
        ItemModel.Unbaked worn = ItemModelUtils.tintedModel(modLoc("item/cosmetic/" + cosmetic.item().getId().getPath()),
                ItemModelUtils.constantTint(-1), new PaintTintSource());
        generators().itemModelOutput.accept(cosmetic.item().get(), ItemModelUtils.conditional(new WornCosmetic(), worn, token));
    }
}
