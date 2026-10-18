package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.registry.DecorBlocks;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.world.level.block.Block;

/**
 * Block states for {@link DecorBlocks}. Furniture models are hand-made (converted from the server pack) and live in
 * {@code src/main/resources/assets/apeira/models/block/}, facing north; block items use the block's model.
 */
public final class DecorBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        furniture(DecorBlocks.OAK_CHAIR.get());
        paintableItem(DecorBlocks.OAK_CHAIR.get());

        // Front, side, top and bottom textures, turned to face the player like a furnace.
        horizontalBlock(DecorBlocks.PAINTING_TABLE.get());
    }

    /** Gives a paintable block's item its block model, with tint index 0 showing the item's paint. */
    private void paintableItem(Block block) {
        generators().itemModelOutput.accept(block.asItem(), ItemModelUtils.tintedModel(modelOf(block), new PaintTintSource()));
    }
}
