package com.apeira.core.datagen;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * Base for one module's item models.
 *
 * <p>NeoForge 26.x no longer ships its own {@code ItemModelProvider}; item models and the client item definitions in
 * {@code assets/<namespace>/items/} are written by vanilla's {@link net.minecraft.client.data.models.ModelProvider},
 * through {@link ItemModelGenerators}. Register subclasses with {@link ApeiraDataGenerators#itemModels}, which runs
 * them inside one model provider for the module, together with its {@link ApeiraBlockStateProvider}s. Every item of
 * the module's registrar must get a model, or the data run fails; plain block items get their block's model unless
 * given another one here.
 *
 * <p>Client-only: only reference subclasses from data generation code.
 */
public abstract class ApeiraItemModelProvider {
    private @Nullable ItemModelGenerators generators;
    private @Nullable String namespace;

    /** Adds this module's item models. */
    protected abstract void registerModels();

    final void generate(String namespace, ItemModelGenerators generators) {
        this.namespace = namespace;
        this.generators = generators;
        try {
            registerModels();
        } finally {
            this.generators = null;
        }
    }

    /** Vanilla's item model generators, for anything the helpers below do not cover. */
    protected ItemModelGenerators generators() {
        return Objects.requireNonNull(generators, "Item model generators are only available inside registerModels");
    }

    protected String namespace() {
        return Objects.requireNonNull(namespace, "namespace");
    }

    /** Returns {@code <namespace>:<path>}. */
    protected Identifier modLoc(String path) {
        return ApeiraIds.id(namespace(), path);
    }

    /** Returns the texture {@code <namespace>:item/<path>}. */
    protected Material itemTexture(String path) {
        return new Material(modLoc("item/" + path));
    }

    /** A flat item drawn from the texture {@code item/<name>}. */
    protected void simpleItem(ItemLike item) {
        generators().generateFlatItem(item.asItem(), ModelTemplates.FLAT_ITEM);
    }

    /** A flat item drawn from {@code texture}. */
    protected void simpleItem(ItemLike item, Material texture) {
        generate(item, ModelTemplates.FLAT_ITEM, TextureMapping.layer0(texture));
    }

    /** A tool or weapon held like a sword, drawn from {@code item/<name>}. */
    protected void toolItem(ItemLike item) {
        generators().generateFlatItem(item.asItem(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }

    /** A tool held like a fishing rod or a stick, drawn from {@code item/<name>}. */
    protected void rodToolItem(ItemLike item) {
        generators().generateFlatItem(item.asItem(), ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
    }

    /**
     * A piece of armour drawn from {@code item/<name>} that shows armour trims, using NeoForge's dynamic trim model, so
     * no model per trim material is generated. {@code slotTrimPrefix} is vanilla's trim texture prefix for its slot,
     * such as {@link ItemModelGenerators#TRIM_PREFIX_HELMET}.
     */
    protected void trimmableArmor(ItemLike item, Identifier slotTrimPrefix) {
        Item value = item.asItem();
        ModelTemplates.FLAT_ITEM.create(value, TextureMapping.layer0(value), generators().modelOutput);
        generators().generateDynamicTrimmableItem(value, slotTrimPrefix, null);
    }

    /** A block's item rendered with the block's model at {@code block/<name>}. */
    protected void blockItem(Block block) {
        blockItem(block, ModelLocationUtils.getModelLocation(block));
    }

    /** A block's item rendered with {@code model}. */
    protected void blockItem(Block block, Identifier model) {
        existingModel(block.asItem(), model);
    }

    /** A block's item drawn as a flat sprite from the block's texture {@code block/<name>}. */
    protected void flatBlockItem(Block block) {
        generate(block.asItem(), ModelTemplates.FLAT_ITEM, TextureMapping.layer0(block));
    }

    /** An item rendered with its hand-made model at {@code item/<name>}, such as a Blockbench model. */
    protected void existingModel(ItemLike item) {
        existingModel(item, ModelLocationUtils.getModelLocation(item.asItem()));
    }

    /** An item rendered with {@code model}, which is not generated here. */
    protected void existingModel(ItemLike item, Identifier model) {
        generators().itemModelOutput.accept(item.asItem(), ItemModelUtils.plainModel(model));
    }

    /** Writes the model {@code item/<name>} from {@code template} and uses it as the item's model. */
    protected Identifier generate(ItemLike item, ModelTemplate template, TextureMapping textures) {
        Item value = item.asItem();
        Identifier model = template.create(value, textures, generators().modelOutput);
        generators().itemModelOutput.accept(value, ItemModelUtils.plainModel(model));
        return model;
    }
}
