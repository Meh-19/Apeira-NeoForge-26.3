package com.apeira.core.datagen;

import net.minecraft.tags.BlockItemTagId;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Tags that exist for blocks and their items alike, such as {@code minecraft:planks} or {@code minecraft:logs_that_burn}
 * ({@link BlockItemTagId}). A module writes them once, as a method taking this interface, and passes that method to
 * both its {@link ApeiraBlockTagsProvider} and its {@link ApeiraItemTagsProvider}, which write the block and the
 * item half of each tag. Blocks are added by their block item, which must share the block's name.
 */
public interface BlockAndItemTags {
    /** Adds {@code blocks} to {@code tag}, in order. */
    void add(BlockItemTagId tag, DeferredBlock<?>... blocks);

    /** Adds the tag {@code child} to {@code tag}. */
    void addTag(BlockItemTagId tag, BlockItemTagId child);
}
