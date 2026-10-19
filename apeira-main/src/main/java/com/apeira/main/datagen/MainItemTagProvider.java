package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.datagen.ApeiraItemTagsProvider;
import com.apeira.main.registry.MainGear;
import com.apeira.main.registry.MainItems;
import com.apeira.main.registry.MainTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;

/**
 * Item tags: platinum repairs platinum tools and armour, the tool and armour tags give them and the shadow gear vanilla's
 * enchantments, the armour takes trims, and the wood tags of {@link MainBlockItemTags}.
 */
public final class MainItemTagProvider extends ApeiraItemTagsProvider {
    public MainItemTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        add(MainTags.PLATINUM_TOOL_MATERIALS, MainItems.PLATINUM);
        add(MainTags.REPAIRS_PLATINUM_ARMOR, MainItems.PLATINUM);
        add(ItemTags.SWORDS, MainItems.PLATINUM_SWORD);
        add(ItemTags.PICKAXES, MainItems.PLATINUM_PICKAXE);
        add(ItemTags.AXES, MainItems.PLATINUM_AXE);
        add(ItemTags.SHOVELS, MainItems.PLATINUM_SHOVEL);
        add(ItemTags.HOES, MainItems.PLATINUM_HOE);
        add(ItemTags.HEAD_ARMOR, MainItems.PLATINUM_HELMET);
        add(ItemTags.CHEST_ARMOR, MainItems.PLATINUM_CHESTPLATE);
        add(ItemTags.LEG_ARMOR, MainItems.PLATINUM_LEGGINGS);
        add(ItemTags.FOOT_ARMOR, MainItems.PLATINUM_BOOTS);
        add(ItemTags.TRIMMABLE_ARMOR, MainItems.PLATINUM_HELMET, MainItems.PLATINUM_CHESTPLATE, MainItems.PLATINUM_LEGGINGS,
                MainItems.PLATINUM_BOOTS);
        // Xenon's shadow gear, tagged as Xenon tagged it. Its repair item, the wraith rib, is not ported, so the repair tags
        // are empty. The rocket boots had no tags in Xenon: they take no enchantments or trims.
        tag(MainTags.SHADOW_TOOL_MATERIALS);
        tag(MainTags.REPAIRS_SHADOW_ARMOR);
        tag(MainTags.REPAIRS_ROCKET_ARMOR);
        add(ItemTags.SWORDS, MainGear.SHADOW_SWORD);
        add(ItemTags.PICKAXES, MainGear.SHADOW_PICKAXE);
        add(ItemTags.AXES, MainGear.SHADOW_AXE);
        add(ItemTags.SHOVELS, MainGear.SHADOW_SHOVEL);
        add(ItemTags.HOES, MainGear.SHADOW_HOE);
        add(ItemTags.HEAD_ARMOR, MainGear.SHADOW_HELMET);
        add(ItemTags.CHEST_ARMOR, MainGear.SHADOW_CHESTPLATE);
        add(ItemTags.LEG_ARMOR, MainGear.SHADOW_LEGGINGS);
        add(ItemTags.FOOT_ARMOR, MainGear.SHADOW_BOOTS);
        add(ItemTags.TRIMMABLE_ARMOR, MainGear.SHADOW_HELMET, MainGear.SHADOW_CHESTPLATE, MainGear.SHADOW_LEGGINGS, MainGear.SHADOW_BOOTS);
        blockItemTags(MainBlockItemTags::addTo);
    }
}
