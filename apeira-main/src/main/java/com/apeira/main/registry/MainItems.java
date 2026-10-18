package com.apeira.main.registry;

import java.util.List;
import java.util.Map;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Apeira's items, from the legacy ORForged mod: its two materials, platinum and ruby, and platinum's tools and armour.
 * Block items are registered with their blocks in {@link MainBlocks} and {@link MainWoods}.
 */
public final class MainItems {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * ORForged's platinum tool material: diamond tier, 500 uses, mining speed 7, 2.2 extra attack damage,
     * enchantability 12.
     */
    public static final ToolMaterial PLATINUM_TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.2F, 12,
            MainTags.PLATINUM_TOOL_MATERIALS);
    /** The armour model and its textures, {@code assets/apeira/equipment/platinum.json}. */
    public static final ResourceKey<EquipmentAsset> PLATINUM_EQUIPMENT = ResourceKey.create(EquipmentAssets.ROOT_ID, ApeiraIds.id("platinum"));
    /**
     * ORForged's platinum armour material: durability multiplier 25, armour 2 (helmet), 6 (chestplate), 7 (leggings)
     * and 3 (boots), enchantability 12, iron's equip sound, no toughness or knockback resistance.
     */
    public static final ArmorMaterial PLATINUM_ARMOR_MATERIAL = new ArmorMaterial(25,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 3, ArmorType.BODY, 0), 12,
            SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, MainTags.REPAIRS_PLATINUM_ARMOR, PLATINUM_EQUIPMENT);

    // Materials
    public static final DeferredItem<Item> PLATINUM = R.items().registerSimpleItem("platinum");
    public static final DeferredItem<Item> RUBY = R.items().registerSimpleItem("ruby");

    // Platinum tools. ORForged gave every tool 3 attack damage and attack speed 2 on top of the material's.
    public static final DeferredItem<Item> PLATINUM_SWORD = R.items().registerSimpleItem("platinum_sword",
            p -> p.sword(PLATINUM_TOOL_MATERIAL, 3.0F, 2.0F));
    public static final DeferredItem<Item> PLATINUM_PICKAXE = R.items().registerSimpleItem("platinum_pickaxe",
            p -> p.pickaxe(PLATINUM_TOOL_MATERIAL, 3.0F, 2.0F));
    public static final DeferredItem<Item> PLATINUM_AXE = R.items().registerSimpleItem("platinum_axe",
            p -> p.axe(PLATINUM_TOOL_MATERIAL, 3.0F, 2.0F));
    public static final DeferredItem<Item> PLATINUM_SHOVEL = R.items().registerSimpleItem("platinum_shovel",
            p -> p.shovel(PLATINUM_TOOL_MATERIAL, 3.0F, 2.0F));
    public static final DeferredItem<Item> PLATINUM_HOE = R.items().registerSimpleItem("platinum_hoe",
            p -> p.hoe(PLATINUM_TOOL_MATERIAL, 3.0F, 2.0F));

    // Platinum armour
    public static final DeferredItem<Item> PLATINUM_HELMET = R.items().registerSimpleItem("platinum_helmet",
            p -> p.humanoidArmor(PLATINUM_ARMOR_MATERIAL, ArmorType.HELMET));
    public static final DeferredItem<Item> PLATINUM_CHESTPLATE = R.items().registerSimpleItem("platinum_chestplate",
            p -> p.humanoidArmor(PLATINUM_ARMOR_MATERIAL, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> PLATINUM_LEGGINGS = R.items().registerSimpleItem("platinum_leggings",
            p -> p.humanoidArmor(PLATINUM_ARMOR_MATERIAL, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> PLATINUM_BOOTS = R.items().registerSimpleItem("platinum_boots",
            p -> p.humanoidArmor(PLATINUM_ARMOR_MATERIAL, ArmorType.BOOTS));

    private MainItems() {}

    public static List<DeferredItem<Item>> tools() {
        return List.of(PLATINUM_SWORD, PLATINUM_PICKAXE, PLATINUM_AXE, PLATINUM_SHOVEL, PLATINUM_HOE);
    }

    public static List<DeferredItem<Item>> armor() {
        return List.of(PLATINUM_HELMET, PLATINUM_CHESTPLATE, PLATINUM_LEGGINGS, PLATINUM_BOOTS);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
