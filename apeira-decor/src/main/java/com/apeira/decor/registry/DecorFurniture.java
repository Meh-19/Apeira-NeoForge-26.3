package com.apeira.decor.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.furniture.Furniture;
import com.apeira.decor.furniture.FurnitureBlock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The furniture of {@link Furniture#all()}, each registered as a {@link FurnitureBlock} with a block item of the same
 * name. A piece exclusive to a crate names it in gold under the item's name ({@code tooltip.apeira.exclusive.<crate>}),
 * as the Crates mod did.
 */
public final class DecorFurniture {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;
    /** The Crates mod's gold. */
    private static final int EXCLUSIVE_COLOR = 0xFFAA00;

    private static final List<DeferredBlock<FurnitureBlock>> ALL = register();

    private DecorFurniture() {}

    /** Every furniture block, in catalogue order. */
    public static List<DeferredBlock<FurnitureBlock>> all() {
        return ALL;
    }

    /** The furniture blocks whose piece matches {@code filter}. */
    public static List<DeferredBlock<FurnitureBlock>> matching(Predicate<Furniture> filter) {
        return ALL.stream().filter(block -> filter.test(block.get().furniture())).toList();
    }

    private static List<DeferredBlock<FurnitureBlock>> register() {
        List<DeferredBlock<FurnitureBlock>> blocks = new ArrayList<>();
        for (Furniture furniture : Furniture.all()) {
            DeferredBlock<FurnitureBlock> block = R.blocks().registerBlock(furniture.name(),
                    properties -> FurnitureBlock.create(properties, furniture), properties -> properties(properties, furniture));
            R.items().registerSimpleBlockItem(block, properties -> itemProperties(properties, furniture));
            blocks.add(block);
        }
        return List.copyOf(blocks);
    }

    /** The Crates mod's furniture properties (breaks quickly with anything), with wood's sound. */
    private static BlockBehaviour.Properties properties(BlockBehaviour.Properties properties, Furniture furniture) {
        properties.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion();
        return furniture.collision() ? properties : properties.noCollision();
    }

    private static Item.Properties itemProperties(Item.Properties properties, Furniture furniture) {
        if (furniture.defaultPaint() != null) {
            properties.component(DecorDataComponents.PAINT.get(), furniture.defaultPaint());
        }
        if (furniture.crate() != null) {
            properties.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.apeira.exclusive." + furniture.crate())
                    .withStyle(Style.EMPTY.withColor(EXCLUSIVE_COLOR).withItalic(false)))));
        }
        return properties;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
