package com.apeira.core.registry;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import net.minecraft.data.BlockFamily;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Planks and the wooden building blocks made from them, as vanilla has for each wood: {@code <name>_planks},
 * {@code _stairs}, {@code _slab}, {@code _button}, {@code _pressure_plate}, {@code _fence} and {@code _fence_gate},
 * each with a block item that burns in a furnace like vanilla's. The shapes copy oak's properties and sounds;
 * {@link #family()} gives the vanilla {@link BlockFamily} that datagen uses for their models and recipes.
 */
public record PlankSet(DeferredBlock<Block> planks, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
        DeferredBlock<ButtonBlock> button, DeferredBlock<PressurePlateBlock> pressurePlate, DeferredBlock<FenceBlock> fence,
        DeferredBlock<FenceGateBlock> fenceGate) {
    private static final UnaryOperator<Item.Properties> WOOD_FUEL = p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_BLOCKS);
    private static final UnaryOperator<Item.Properties> SLAB_FUEL = p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_SLABS);
    private static final UnaryOperator<Item.Properties> BUTTON_FUEL =
            p -> p.cookingFuel(ContextIntProviders.COOKING_TIME_WOOD_ITEMS_EXTRA_SMALL);

    /**
     * Registers {@code <name>_planks} with {@code planks} properties and its shapes. Buttons stay pressed for
     * {@code buttonTicks} (oak's is 30); the button, plate and gate sound and behave like {@code setType} and
     * {@code woodType}.
     */
    public static PlankSet register(ApeiraRegistrar registrar, String name, Supplier<BlockBehaviour.Properties> planks,
            BlockSetType setType, WoodType woodType, int buttonTicks) {
        DeferredBlock<Block> base = registrar.blockWithItem(name + "_planks", Block::new, planks, WOOD_FUEL);
        return new PlankSet(base,
                registrar.blockWithItem(name + "_stairs", p -> new StairBlock(base.get().defaultBlockState(), p),
                        () -> BlockBehaviour.Properties.ofLegacyCopy(base.get()), WOOD_FUEL),
                registrar.blockWithItem(name + "_slab", SlabBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(base.get()), SLAB_FUEL),
                registrar.blockWithItem(name + "_button", p -> new ButtonBlock(setType, buttonTicks, p),
                        () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_BUTTON), BUTTON_FUEL),
                registrar.blockWithItem(name + "_pressure_plate", p -> new PressurePlateBlock(setType, p),
                        () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(base.get().defaultMapColor()),
                        WOOD_FUEL),
                registrar.blockWithItem(name + "_fence", FenceBlock::new,
                        () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_FENCE).mapColor(base.get().defaultMapColor()), WOOD_FUEL),
                registrar.blockWithItem(name + "_fence_gate", p -> new FenceGateBlock(woodType, p),
                        () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_FENCE_GATE).mapColor(base.get().defaultMapColor()),
                        WOOD_FUEL));
    }

    /** Every block of the set, planks first. */
    public List<DeferredBlock<? extends Block>> all() {
        return List.of(planks, stairs, slab, button, pressurePlate, fence, fenceGate);
    }

    /** The shapes, without the planks. */
    public List<DeferredBlock<? extends Block>> shapes() {
        return List.of(stairs, slab, button, pressurePlate, fence, fenceGate);
    }

    /** The vanilla block family of the set, for model and recipe generation. Only call once blocks are registered. */
    public BlockFamily family() {
        return new BlockFamily.Builder(planks.get())
                .stairs(stairs.get())
                .slab(slab.get())
                .button(button.get())
                .pressurePlate(pressurePlate.get())
                .fence(fence.get())
                .fenceGate(fenceGate.get())
                .recipeGroupPrefix("wooden")
                .recipeUnlockedBy("has_planks")
                .getFamily();
    }
}
