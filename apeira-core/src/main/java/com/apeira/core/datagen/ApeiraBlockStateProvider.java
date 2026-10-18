package com.apeira.core.datagen;

import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Base for one module's block state definitions and block models.
 *
 * <p>NeoForge 26.x no longer ships its own {@code BlockStateProvider}; block states, block models and item models are
 * all written by vanilla's {@link net.minecraft.client.data.models.ModelProvider} in one pass, through
 * {@link BlockModelGenerators}. Subclasses are therefore not data providers themselves: register them with
 * {@link ApeiraDataGenerators#blockStates}, which runs them inside one model provider for the module. Every block of
 * the module's registrar must get a definition, or the data run fails.
 *
 * <p>A block whose item is a plain {@link net.minecraft.world.item.BlockItem} gets an item model pointing at
 * {@code <namespace>:block/<name>} automatically, unless an {@link ApeiraItemModelProvider} or
 * {@link #blockItemModel} gives it another one.
 *
 * <p>Client-only: only reference subclasses from data generation code.
 */
public abstract class ApeiraBlockStateProvider {
    private @Nullable BlockModelGenerators generators;
    private @Nullable String namespace;

    /** Adds this module's block state definitions and models. */
    protected abstract void registerStatesAndModels();

    final void generate(String namespace, BlockModelGenerators generators) {
        this.namespace = namespace;
        this.generators = generators;
        try {
            registerStatesAndModels();
        } finally {
            this.generators = null;
        }
    }

    /** Vanilla's block model generators, for anything the helpers below do not cover. */
    protected BlockModelGenerators generators() {
        return Objects.requireNonNull(generators, "Block state generators are only available inside registerStatesAndModels");
    }

    protected String namespace() {
        return Objects.requireNonNull(namespace, "namespace");
    }

    /** Returns {@code <namespace>:<path>}. */
    protected Identifier modLoc(String path) {
        return ApeiraIds.id(namespace(), path);
    }

    /** Returns the model {@code <namespace>:block/<path>}. */
    protected Identifier blockModel(String path) {
        return modLoc("block/" + path);
    }

    /** Returns the texture {@code <namespace>:block/<path>}. */
    protected Material blockTexture(String path) {
        return new Material(modLoc("block/" + path));
    }

    /** Returns {@code <namespace>:block/<name>}, the default model of a block. */
    protected static Identifier modelOf(Block block) {
        return ModelLocationUtils.getModelLocation(block);
    }

    // Simple blocks

    /** A full cube using the texture {@code block/<name>} on every side. */
    protected void simpleBlock(Block block) {
        generators().createTrivialCube(block);
    }

    /** A block with one state, rendered with a generated model. */
    protected void simpleBlock(Block block, TexturedModel.Provider model) {
        generators().createTrivialBlock(block, model);
    }

    /** A block with one state, rendered with {@code model}, which is not generated here (for example a Blockbench model). */
    protected void simpleBlock(Block block, Identifier model) {
        generators().blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, BlockModelGenerators.plainVariant(model)));
    }

    /** A block with one state, rendered with its hand-made model at {@code block/<name>}. */
    protected void existingModelBlock(Block block) {
        simpleBlock(block, modelOf(block));
    }

    // Directional blocks

    /**
     * A block rotated by {@link BlockStateProperties#HORIZONTAL_FACING}, with a front, side, top and bottom texture
     * ({@code block/<name>_front}, {@code _side}, {@code _top} and {@code _bottom}), like a furnace.
     */
    protected void horizontalBlock(Block block) {
        generators().createHorizontallyRotatedBlock(block, TexturedModel.ORIENTABLE);
    }

    /** A block rotated by {@link BlockStateProperties#HORIZONTAL_FACING}. {@code model} faces north. */
    protected void horizontalBlock(Block block, Identifier model) {
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    /** A block rotated by {@link BlockStateProperties#FACING} in all six directions. {@code model} faces up. */
    protected void directionalBlock(Block block, Identifier model) {
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                .with(BlockModelGenerators.ROTATION_FACING));
    }

    /** A pillar along {@link BlockStateProperties#AXIS}, with {@code block/<name>} sides and {@code block/<name>_top} ends. */
    protected void axisBlock(Block block) {
        generators().createAxisAlignedPillarBlock(block, TexturedModel.COLUMN);
    }

    // Furniture

    /**
     * Furniture rendered with its hand-made model at {@code block/<name>}, rotated by
     * {@link BlockStateProperties#HORIZONTAL_FACING}. The model is authored facing north.
     */
    protected void furniture(Block block) {
        furniture(block, modelOf(block));
    }

    /** Furniture rendered with {@code model}, authored facing north, rotated by {@code HORIZONTAL_FACING}. */
    protected void furniture(Block block, Identifier model) {
        furniture(block, BlockStateProperties.HORIZONTAL_FACING, model);
    }

    /**
     * Furniture rendered with {@code model}, authored facing north, rotated around the vertical axis by a block's own
     * facing property. Vertical facings keep the model unrotated.
     */
    protected void furniture(Block block, EnumProperty<Direction> facing, Identifier model) {
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block, BlockModelGenerators.plainVariant(model))
                .with(rotationFor(facing)));
    }

    /**
     * Furniture whose model depends on another property as well as its facing, such as a sofa's left, middle and right
     * pieces or a table's connected legs. Every model is authored facing north.
     */
    protected <T extends Comparable<T>> void furniture(Block block, Property<T> shape, Function<T, Identifier> modelForShape) {
        generators().blockStateOutput.accept(MultiVariantGenerator.dispatch(block)
                .with(PropertyDispatch.initial(shape).generate(value -> BlockModelGenerators.plainVariant(modelForShape.apply(value))))
                .with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
    }

    /** Dispatches a north-facing model's rotation on any horizontal facing property. */
    protected static PropertyDispatch<VariantMutator> rotationFor(EnumProperty<Direction> facing) {
        return PropertyDispatch.modify(facing).generate(ApeiraBlockStateProvider::yRotation);
    }

    /** Returns the rotation that turns a north-facing model to face {@code direction}. */
    protected static VariantMutator yRotation(Direction direction) {
        return switch (direction) {
            case EAST -> BlockModelGenerators.Y_ROT_90;
            case SOUTH -> BlockModelGenerators.Y_ROT_180;
            case WEST -> BlockModelGenerators.Y_ROT_270;
            case NORTH, UP, DOWN -> BlockModelGenerators.NOP;
        };
    }

    /** Returns {@code model} as a single block state variant. */
    protected static MultiVariant variant(Identifier model) {
        return BlockModelGenerators.plainVariant(model);
    }

    // Items of blocks

    /** Gives the block's item {@code model} instead of the block's default model. */
    protected void blockItemModel(Block block, Identifier model) {
        generators().registerSimpleItemModel(block, model);
    }

    /** Gives the block's item a flat sprite from the texture {@code item/<name>}, as vanilla does for doors. */
    protected void flatBlockItem(Block block) {
        generators().registerSimpleFlatItemModel(block.asItem());
    }
}
