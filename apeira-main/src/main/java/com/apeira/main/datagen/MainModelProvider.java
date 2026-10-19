package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraItemModelProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.client.WornCosmetic;
import java.util.ArrayList;
import java.util.List;

import com.apeira.main.client.CapturedMobProperty;
import com.apeira.main.registry.MainCosmetics;
import com.apeira.main.registry.MainDataComponents;
import com.apeira.main.registry.MainGear;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainMaterials;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

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

        // Xenon's items, with its own item models.
        existingModel(MainMaterials.BLIGHT_ESSENCE);
        existingModel(MainMaterials.DOLOMITE_DUST);
        existingModel(MainMaterials.RAW_TURKEY);
        existingModel(MainMaterials.COOKED_TURKEY);
        existingModel(MainMaterials.CROCODILE_TOOTH);

        captureNet();

        MainCosmetics.all().forEach(this::cosmetic);
    }

    /** Xenon's capture net mobs: the vanilla mobs and Apeira's that Xenon drew in the net ({@code item/capture_net_mob/<mob>}). */
    private static final List<String> NET_MOBS = List.of("allay", "armadillo", "axolotl", "bat", "bee", "blaze", "camel", "cat", "cave_spider", "chicken", "cod", "copper_golem", "cow", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "enderman", "endermite", "evoker", "fox", "frog", "ghast", "glow_squid", "goat", "guardian", "hoglin", "horse", "husk", "illusioner", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager", "polar_bear", "pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "spider", "squid", "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator", "wandering_trader", "warden", "witch", "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin");
    private static final List<String> APEIRA_NET_MOBS = List.of("crocodile", "grizzly_bear", "turkey", "wraith");

    /**
     * The capture net: empty it is {@code item/capture_net}; holding a mob it shows that mob in the closed net
     * ({@code item/capture_net_mob/<mob>}) through the {@code apeira:captured_mob} property, or Xenon's generic filled net
     * ({@code item/capture_net_filled}) for a mob it has no picture of.
     */
    private void captureNet() {
        Item net = MainGear.CAPTURE_NET.get();
        ItemModel.Unbaked empty = ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(net, TextureMapping.layer0(net), generators().modelOutput));
        List<SelectItemModel.SwitchCase<ResourceKey<EntityType<?>>>> cases = new ArrayList<>();
        NET_MOBS.forEach(mob -> cases.add(netCase(Identifier.withDefaultNamespace(mob))));
        APEIRA_NET_MOBS.forEach(mob -> cases.add(netCase(modLoc(mob))));
        ItemModel.Unbaked filled = ItemModelUtils.select(new CapturedMobProperty(), ItemModelUtils.plainModel(modLoc("item/capture_net_filled")),
                cases);
        generators().itemModelOutput.accept(net, ItemModelUtils.conditional(ItemModelUtils.hasComponent(MainDataComponents.CAPTURED_MOB.get()),
                filled, empty));
    }

    private SelectItemModel.SwitchCase<ResourceKey<EntityType<?>>> netCase(Identifier mob) {
        return ItemModelUtils.when(ResourceKey.create(Registries.ENTITY_TYPE, mob), ItemModelUtils.plainModel(modLoc("item/capture_net_mob/" + mob.getPath())));
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
