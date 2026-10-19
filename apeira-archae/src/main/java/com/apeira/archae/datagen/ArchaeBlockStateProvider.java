package com.apeira.archae.datagen;

import com.apeira.archae.registry.ArchaeArtifacts;
import com.apeira.archae.registry.ArchaeBlocks;
import com.apeira.core.datagen.ApeiraBlockStateProvider;
import com.apeira.decor.client.PaintTintSource;

import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Block states and item definitions for {@link ArchaeBlocks} and {@link ArchaeArtifacts}. The models are Argon's,
 * hand-made resources in {@code src/main/resources/assets/apeira/models/block/archaeology/}.
 *
 * <p>An artifact has one block state for all its rotations, since its block model only gives the breaking particles
 * ({@code ArtifactRenderer} draws the item). Its item shows the same model, with the paint on tint index 1 (Argon's).
 */
public final class ArchaeBlockStateProvider extends ApeiraBlockStateProvider {
    /** Argon's pyramid relic has no model of its own: its block model is particles only and its item shows another statue. */
    private static final String PYRAMID_RELIC = "pyramid_relic";

    @Override
    protected void registerStatesAndModels() {
        // Argon's orientable_with_bottom model, one state.
        simpleBlock(ArchaeBlocks.ASSEMBLY_TABLE.get(), modelOf(ArchaeBlocks.ASSEMBLY_TABLE.get()));
        blockItemModel(ArchaeBlocks.ASSEMBLY_TABLE.get(), modelOf(ArchaeBlocks.ASSEMBLY_TABLE.get()));

        for (ArchaeArtifacts.Artifact artifact : ArchaeArtifacts.all()) {
            Block block = artifact.block().get();
            String name = artifact.block().getId().getPath();
            Identifier model = modLoc("block/archaeology/" + name);
            simpleBlock(block, model);
            Identifier item = name.equals(PYRAMID_RELIC) ? modLoc("item/archaeology/" + name) : model;
            generators().itemModelOutput.accept(block.asItem(),
                    ItemModelUtils.tintedModel(item, ItemModelUtils.constantTint(-1), new PaintTintSource()));
        }
    }
}
