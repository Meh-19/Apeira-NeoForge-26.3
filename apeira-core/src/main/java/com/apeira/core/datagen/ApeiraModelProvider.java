package com.apeira.core.datagen;

import java.util.List;
import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Runs one module's {@link ApeiraBlockStateProvider}s and {@link ApeiraItemModelProvider}s as a single vanilla
 * {@link ModelProvider}. Created by {@link ApeiraDataGenerators}.
 *
 * <p>Validation is limited to the registrar's own blocks and items, so modules sharing a namespace do not need each
 * other's models.
 */
public final class ApeiraModelProvider extends ModelProvider {
    private final ApeiraRegistrar registrar;
    private final List<? extends ApeiraBlockStateProvider> blockStates;
    private final List<? extends ApeiraItemModelProvider> itemModels;

    public ApeiraModelProvider(PackOutput output, ApeiraRegistrar registrar, List<? extends ApeiraBlockStateProvider> blockStates,
            List<? extends ApeiraItemModelProvider> itemModels) {
        super(output, registrar.namespace());
        this.registrar = registrar;
        this.blockStates = List.copyOf(blockStates);
        this.itemModels = List.copyOf(itemModels);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        blockStates.forEach(provider -> provider.generate(modId, blockModels));
        this.itemModels.forEach(provider -> provider.generate(modId, itemModels));
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return registrar.blocks().getEntries().stream();
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return registrar.items().getEntries().stream();
    }
}
