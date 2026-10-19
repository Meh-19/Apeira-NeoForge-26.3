package com.apeira.main.registry;

import java.util.List;
import java.util.Map;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.item.CaptureNetItem;
import com.apeira.main.item.RocketBootsItem;
import com.apeira.main.item.ShadowArmorItem;
import com.apeira.main.item.ShadowSwordItem;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Xenon's tools and gear: the capture net, the shadow tools, sword and armour ({@link com.apeira.main.effect.ShadowModeManager
 * Shadow Mode}) and the rocket boots.
 */
public final class MainGear {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * Xenon's shadow tool material: 1600 uses, mining speed 8, 3 extra attack damage, enchantability 18. Its
     * {@code #apeira:incorrect_for_shadow_tool} is empty, so shadow tools harvest every block.
     */
    public static final ToolMaterial SHADOW_TOOL_MATERIAL = new ToolMaterial(MainTags.INCORRECT_FOR_SHADOW_TOOL, 1600, 8.0F, 3.0F, 18,
            MainTags.SHADOW_TOOL_MATERIALS);
    /** The shadow armour model, {@code assets/apeira/equipment/shadow.json}. */
    public static final ResourceKey<EquipmentAsset> SHADOW_EQUIPMENT = ResourceKey.create(EquipmentAssets.ROOT_ID, ApeiraIds.id("shadow"));
    /**
     * Xenon's shadow armour material, between platinum and netherite: durability multiplier 22, armour 3 (helmet),
     * 7 (chestplate), 5 (leggings) and 3 (boots), enchantability 18, netherite's equip sound, toughness 2.
     */
    public static final ArmorMaterial SHADOW_ARMOR_MATERIAL = new ArmorMaterial(22,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 3, ArmorType.BODY, 5), 18,
            SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.0F, MainTags.REPAIRS_SHADOW_ARMOR, SHADOW_EQUIPMENT);
    /** The rocket boots model, {@code assets/apeira/equipment/rocket.json}. */
    public static final ResourceKey<EquipmentAsset> ROCKET_EQUIPMENT = ResourceKey.create(EquipmentAssets.ROOT_ID, ApeiraIds.id("rocket"));
    /**
     * Xenon's rocket armour material: armour 1, enchantability 5, iron's equip sound, nothing repairs it. Its durability
     * multiplier 33 gives boots Xenon's 429 durability.
     */
    public static final ArmorMaterial ROCKET_ARMOR_MATERIAL = new ArmorMaterial(33,
            Map.of(ArmorType.HELMET, 0, ArmorType.CHESTPLATE, 0, ArmorType.LEGGINGS, 0, ArmorType.BOOTS, 1, ArmorType.BODY, 0), 5,
            SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, MainTags.REPAIRS_ROCKET_ARMOR, ROCKET_EQUIPMENT);

    public static final DeferredItem<CaptureNetItem> CAPTURE_NET = R.items().registerItem("capture_net", CaptureNetItem::new);

    // Shadow tools, with Xenon's attack damage and speed. They break blocks silently (mixin/SilentBlockBreakMixin).
    public static final DeferredItem<ShadowSwordItem> SHADOW_SWORD = R.items().registerItem("shadow_sword", ShadowSwordItem::new,
            p -> p.sword(SHADOW_TOOL_MATERIAL, 3.0F, -2.4F));
    public static final DeferredItem<Item> SHADOW_PICKAXE = R.items().registerSimpleItem("shadow_pickaxe",
            p -> p.pickaxe(SHADOW_TOOL_MATERIAL, 1.0F, -2.8F));
    public static final DeferredItem<Item> SHADOW_SHOVEL = R.items().registerSimpleItem("shadow_shovel",
            p -> p.shovel(SHADOW_TOOL_MATERIAL, 1.5F, -3.0F));
    public static final DeferredItem<Item> SHADOW_AXE = R.items().registerSimpleItem("shadow_axe",
            p -> p.axe(SHADOW_TOOL_MATERIAL, 6.0F, -3.2F));
    public static final DeferredItem<Item> SHADOW_HOE = R.items().registerSimpleItem("shadow_hoe",
            p -> p.hoe(SHADOW_TOOL_MATERIAL, 0.0F, -3.0F));

    // Shadow armour
    public static final DeferredItem<ShadowArmorItem> SHADOW_HELMET = R.items().registerItem("shadow_helmet", ShadowArmorItem::new,
            p -> p.humanoidArmor(SHADOW_ARMOR_MATERIAL, ArmorType.HELMET));
    public static final DeferredItem<ShadowArmorItem> SHADOW_CHESTPLATE = R.items().registerItem("shadow_chestplate", ShadowArmorItem::new,
            p -> p.humanoidArmor(SHADOW_ARMOR_MATERIAL, ArmorType.CHESTPLATE));
    public static final DeferredItem<ShadowArmorItem> SHADOW_LEGGINGS = R.items().registerItem("shadow_leggings", ShadowArmorItem::new,
            p -> p.humanoidArmor(SHADOW_ARMOR_MATERIAL, ArmorType.LEGGINGS));
    public static final DeferredItem<ShadowArmorItem> SHADOW_BOOTS = R.items().registerItem("shadow_boots", ShadowArmorItem::new,
            p -> p.humanoidArmor(SHADOW_ARMOR_MATERIAL, ArmorType.BOOTS));

    public static final DeferredItem<RocketBootsItem> ROCKET_BOOTS = R.items().registerItem("rocket_boots", RocketBootsItem::new,
            p -> p.humanoidArmor(ROCKET_ARMOR_MATERIAL, ArmorType.BOOTS));

    private static final List<DeferredItem<? extends Item>> SHADOW_TOOLS = List.of(SHADOW_SWORD, SHADOW_PICKAXE, SHADOW_SHOVEL,
            SHADOW_AXE, SHADOW_HOE);

    private MainGear() {}

    /** The shadow sword and tools, which break blocks silently. */
    public static List<DeferredItem<? extends Item>> shadowTools() {
        return SHADOW_TOOLS;
    }

    public static List<DeferredItem<ShadowArmorItem>> shadowArmor() {
        return List.of(SHADOW_HELMET, SHADOW_CHESTPLATE, SHADOW_LEGGINGS, SHADOW_BOOTS);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
