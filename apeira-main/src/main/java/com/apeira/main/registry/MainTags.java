package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Apeira's own tags. */
public final class MainTags {
    /** What repairs platinum tools: platinum. */
    public static final TagKey<Item> PLATINUM_TOOL_MATERIALS = item("platinum_tool_materials");
    /** What repairs platinum armour: platinum. */
    public static final TagKey<Item> REPAIRS_PLATINUM_ARMOR = item("repairs_platinum_armor");
    /**
     * Blocks shadow tools can't get drops from. Empty, as Xenon's {@code incorrect_for_shadow_tool} was, so shadow tools
     * harvest everything.
     */
    public static final TagKey<Block> INCORRECT_FOR_SHADOW_TOOL = block("incorrect_for_shadow_tool");
    /** What repairs shadow tools. Xenon's was the wraith rib, which is not ported (it has no texture), so it is empty. */
    public static final TagKey<Item> SHADOW_TOOL_MATERIALS = item("shadow_tool_materials");
    /** What repairs shadow armour: empty, like {@link #SHADOW_TOOL_MATERIALS}. */
    public static final TagKey<Item> REPAIRS_SHADOW_ARMOR = item("repairs_shadow_armor");
    /** What repairs rocket boots: nothing, as in Xenon. */
    public static final TagKey<Item> REPAIRS_ROCKET_ARMOR = item("repairs_rocket_armor");

    /**
     * Where Xenon's cattails and clover grow: dirt, sand, red sand, gravel, clay, mud, muddy mangrove roots, moss, farmland,
     * soul sand and soul soil.
     */
    public static final TagKey<Block> WETLAND_PLANT_SOIL = block("wetland_plant_soil");
    /** Nether floors that hogweed, soul stalks and the oculus orchid grow on too: netherrack, soul sand and soil, nylium. */
    public static final TagKey<Block> NETHER_PLANT_SOIL = block("nether_plant_soil");

    /** What keeps a coral next to it alive out of water, as Xenon's dolomite did: dolomite and its shapes. */
    public static final TagKey<Block> KEEPS_CORAL_ALIVE = block("keeps_coral_alive");

    private MainTags() {}

    /** The logs and woods of one tree, like vanilla's {@code oak_logs}: {@code apeira:<wood>_logs}. */
    public static BlockItemTagId logs(String wood) {
        return BlockItemTagId.create(ApeiraIds.id(wood + "_logs"), ApeiraIds.id(wood + "_logs"));
    }

    /** Mobs a capture net catches (Xenon's {@code xenon:capturable}). */
    public static final TagKey<EntityType<?>> CAPTURABLE = entity("capturable");
    /** Mobs a capture net catches only while they have Weakness. */
    public static final TagKey<EntityType<?>> CAPTURABLE_WHEN_WEAKENED = entity("capturable_when_weakened");

    private static TagKey<EntityType<?>> entity(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ApeiraIds.id(name));
    }

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, ApeiraIds.id(name));
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, ApeiraIds.id(name));
    }
}
