package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Apeira's own tags. */
public final class MainTags {
    /** What repairs platinum tools: platinum. */
    public static final TagKey<Item> PLATINUM_TOOL_MATERIALS = item("platinum_tool_materials");
    /** What repairs platinum armour: platinum. */
    public static final TagKey<Item> REPAIRS_PLATINUM_ARMOR = item("repairs_platinum_armor");

    /**
     * Where Xenon's cattails and clover grow: dirt, sand, red sand, gravel, clay, mud, muddy mangrove roots, moss, farmland,
     * soul sand and soul soil.
     */
    public static final TagKey<Block> WETLAND_PLANT_SOIL = block("wetland_plant_soil");

    /** What keeps a coral next to it alive out of water, as Xenon's dolomite did: dolomite and its shapes. */
    public static final TagKey<Block> KEEPS_CORAL_ALIVE = block("keeps_coral_alive");

    private MainTags() {}

    /** The logs and woods of one tree, like vanilla's {@code oak_logs}: {@code apeira:<wood>_logs}. */
    public static BlockItemTagId logs(String wood) {
        return BlockItemTagId.create(ApeiraIds.id(wood + "_logs"), ApeiraIds.id(wood + "_logs"));
    }

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, ApeiraIds.id(name));
    }

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, ApeiraIds.id(name));
    }
}
