package com.apeira.core.datagen;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Base for one module's block tags, written to {@code data/<tag namespace>/tags/block/<tag>.json}. Vanilla tags such
 * as {@code minecraft:mineable/pickaxe} are extended rather than replaced, so they keep vanilla's entries.
 *
 * <p>Register subclasses with {@link ApeiraDataGenerators#blockTags}:
 *
 * <pre>{@code
 * public final class DecorBlockTags extends ApeiraBlockTagsProvider {
 *     public DecorBlockTags(ApeiraDataGenerators.Context context) {
 *         super(context);
 *     }
 *
 *     @Override
 *     protected void addTags(HolderLookup.Provider registries) {
 *         add(BlockTags.MINEABLE_WITH_AXE, DecorBlocks.OAK_CHAIR);
 *     }
 * }
 * }</pre>
 *
 * <p>In 26.x a tag appender takes registry keys; {@code tag(tag).add(block.key())} works too.
 */
public abstract class ApeiraBlockTagsProvider extends BlockTagsProvider {
    protected ApeiraBlockTagsProvider(ApeiraDataGenerators.Context context) {
        super(context.output(), context.lookup(), context.namespace());
    }

    /** Adds {@code blocks} to {@code tag}, in order. */
    protected void add(TagKey<Block> tag, DeferredBlock<?>... blocks) {
        var appender = tag(tag);
        for (DeferredBlock<?> block : blocks) {
            appender.add(block.key());
        }
    }
}
