package com.apeira.main.datagen;

import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.main.registry.MainBlocks;
import com.apeira.main.registry.MainCrates;

import net.minecraft.world.level.block.Block;

/** Block states and block models for {@link MainBlocks} (block items use their block's model) and {@link MainCrates}. */
public final class MainBlockStateProvider extends ApeiraBlockStateProvider {
    @Override
    protected void registerStatesAndModels() {
        simpleBlock(MainBlocks.CARVED_APEIRA_STONE.get());
        simpleBlock(MainBlocks.APEIRA_BRICK.get());

        // Crates are drawn by their block entity, whatever their rotation; the hand-made block model only gives the
        // breaking particles, and the item uses the crate's hand-made item model.
        for (var crate : MainCrates.all()) {
            Block block = crate.get();
            existingModelBlock(block);
            blockItemModel(block, modLoc("item/" + crate.getId().getPath()));
        }
    }
}
