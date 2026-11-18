package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorFurniture;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Block states for {@link DecorBlocks} and {@link DecorFurniture}. Furniture models are hand-made (the server pack's)
 * and live in {@code src/main/resources/assets/apeira/models/{block,item}/furniture/}.
 */
public final class DecorBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        for (DeferredBlock<? extends FurnitureBlock> block : DecorFurniture.all()) {
            furniture(block.get());
        }

        // Front, side, top and bottom textures, turned to face the player like a furnace.
        horizontalBlock(DecorBlocks.PAINTING_TABLE.get());

        // Parts render nothing; the hand-made model only gives a particle texture.
        existingModelBlock(DecorBlocks.MULTIBLOCK_PART.get());
    }

    /**
     * A furniture piece's block state, one model for every state, since its block model ({@code block/furniture/<name>})
     * only gives the breaking particles, and its item definition: {@code item/furniture/<name>}, the piece's whole model,
     * which {@code FurnitureRenderer} also draws placed, with the paint on tint index 1, as artifacts have it.
     */
    private void furniture(FurnitureBlock block) {
        Identifier id = modelOf(block);
        String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        simpleBlock(block, id.withPath("block/furniture/" + name));
        generators().itemModelOutput.accept(block.asItem(), ItemModelUtils.tintedModel(id.withPath("item/furniture/" + name),
                ItemModelUtils.constantTint(-1), new PaintTintSource()));
    }
}
