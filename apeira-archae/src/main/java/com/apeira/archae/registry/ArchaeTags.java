package com.apeira.archae.registry;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The archae module's tags. */
public final class ArchaeTags {
    /** {@code #apeira:fragments}: what the assembly table's input slot takes; as in Argon, the fragments artifacts are made from. */
    public static final TagKey<Item> FRAGMENTS = TagKey.create(Registries.ITEM, ApeiraIds.id("fragments"));

    private ArchaeTags() {}
}
