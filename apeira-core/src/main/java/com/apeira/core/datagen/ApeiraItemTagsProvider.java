package com.apeira.core.datagen;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Base for one module's item tags, written to {@code data/<tag namespace>/tags/item/<tag>.json}. Vanilla tags are
 * extended rather than replaced. Register subclasses with {@link ApeiraDataGenerators#itemTags}.
 *
 * <p>Tags shared by blocks and their items, such as {@code minecraft:planks}, are best written once as
 * {@link BlockAndItemTags} and passed to both {@link #blockItemTags} and the block tags provider's.
 */
public abstract class ApeiraItemTagsProvider extends ItemTagsProvider {
    protected ApeiraItemTagsProvider(ApeiraDataGenerators.Context context) {
        super(context.output(), context.lookup(), context.namespace());
    }

    /** Adds {@code items} to {@code tag}, in order. */
    @SafeVarargs
    protected final void add(TagKey<Item> tag, DeferredHolder<Item, ? extends Item>... items) {
        var appender = tag(tag);
        for (DeferredHolder<Item, ? extends Item> item : items) {
            appender.add(item.key());
        }
    }

    /** Adds the items of {@code blocks} to {@code tag}, in order. */
    protected void add(TagKey<Item> tag, DeferredBlock<?>... blocks) {
        var appender = tag(tag);
        for (DeferredBlock<?> block : blocks) {
            appender.add(ResourceKey.create(Registries.ITEM, block.getId()));
        }
    }

    /** Writes the item half of the module's {@link BlockAndItemTags}. */
    protected void blockItemTags(Consumer<BlockAndItemTags> tags) {
        tags.accept(new BlockAndItemTags() {
            @Override
            public void add(BlockItemTagId tag, DeferredBlock<?>... blocks) {
                ApeiraItemTagsProvider.this.add(tag.item(), blocks);
            }

            @Override
            public void addTag(BlockItemTagId tag, BlockItemTagId child) {
                tag(tag.item()).addTag(child.item());
            }
        });
    }
}
