package com.apeira.decor.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.decor.client.PaintTintSource;
import com.apeira.decor.furniture.Furniture;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorFurniture;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Block states for {@link DecorBlocks} and {@link DecorFurniture}. Furniture models are hand-made (converted from the
 * legacy mods) and live in {@code src/main/resources/assets/apeira/models/{block,item}/furniture/}, facing north.
 */
public final class DecorBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        for (DeferredBlock<FurnitureBlock> block : DecorFurniture.all()) {
            furniture(block.get());
        }

        // Front, side, top and bottom textures, turned to face the player like a furnace.
        horizontalBlock(DecorBlocks.PAINTING_TABLE.get());
    }

    /**
     * A furniture piece's block state, one for every rotation, since its block model only gives the breaking particles,
     * and its item definition: {@code item/furniture/<name>}, and in the {@code none} display context, which
     * {@code FurnitureRenderer} draws, {@code block/furniture/<name>}, or for a large piece
     * {@code item/furniture/<name>_placed}. Paintable pieces show their paint on tint index 0.
     */
    private void furniture(FurnitureBlock block) {
        Furniture furniture = block.furniture();
        Identifier id = modelOf(block);
        Identifier blockModel = id.withPath(path -> "block/furniture/" + furniture.name());
        simpleBlock(block, blockModel);
        Identifier placed = furniture.large() != null ? id.withPath(path -> "item/furniture/" + furniture.name() + "_placed") : blockModel;
        ItemModel.Unbaked item = ItemModelUtils.select(new DisplayContext(), model(id.withPath(path -> "item/furniture/" + furniture.name()),
                furniture), ItemModelUtils.when(ItemDisplayContext.NONE, model(placed, furniture)));
        generators().itemModelOutput.accept(block.asItem(), item);
    }

    private static ItemModel.Unbaked model(Identifier model, Furniture furniture) {
        return furniture.paintable() ? ItemModelUtils.tintedModel(model, new PaintTintSource()) : ItemModelUtils.plainModel(model);
    }
}
