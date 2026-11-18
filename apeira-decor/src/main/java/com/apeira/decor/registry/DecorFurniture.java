package com.apeira.decor.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.furniture.AnimatedFurnitureBlock;
import com.apeira.decor.furniture.FacingFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock.Form;
import com.apeira.decor.furniture.FurnitureBlockItem;
import com.apeira.decor.furniture.FurnitureFootprints;
import com.apeira.decor.furniture.SeatFurnitureBlock;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Decor's furniture pieces, on the one furniture system ({@link FurnitureBlock}, the same as archaeology's artifacts):
 * each a block with a {@link FurnitureBlockItem} of the same name, drawn from {@code models/item/furniture/<name>.json}
 * by {@code FurnitureRenderer} as its {@link Form}'s display mode lays it in, with its footprint from
 * {@code apeira_decor/furniture_footprints.txt}. A piece exclusive to a crate names it in gold under its name
 * ({@code tooltip.apeira.exclusive.<crate>}).
 *
 * <p>The pieces are the Badger Crate's, the Origin Realms server pack's {@code models/custom/furniture/badger/} models
 * (names from Ren's uicapture of the {@code /catalog} "Reward Furniture" pages). Its {@code fixed} display picks the
 * mode: rotation x 90 is drawn on the top face ({@link Form#FURNITURE}), -90 on the floor ({@link Form#FLOOR}), as
 * Argon's models are. A piece takes paint when its model has tint index 1 faces (all but the wooden cart); the
 * windmill's GeckoLib model paints its {@code _tintable} bones (sails and tail fin).
 */
public final class DecorFurniture {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;
    private static final List<DeferredBlock<? extends FurnitureBlock>> ALL = new ArrayList<>();

    public static final DeferredBlock<SeatFurnitureBlock> OUTDOOR_ROCKING_CHAIR = piece("outdoor_rocking_chair", "badger_crate",
            p -> new SeatFurnitureBlock(p, true, 8));
    /** Offsets are Ren's furniture wand exports. */
    public static final DeferredBlock<FurnitureBlock> WEDGE_TENT = piece("wedge_tent", "badger_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE).renderOffset(0, 16, 0));
    /** On the floor, or against a wall as {@link #SASH_BANNER_WALL} (the pack's {@code sash_banner_wall} model). */
    public static final DeferredBlock<FurnitureBlock> SASH_BANNER = piece("sash_banner", "badger_crate",
            p -> new FurnitureBlock(p, true, Form.FLOOR, () -> DecorFurniture.SASH_BANNER_WALL.get(), null, null).renderOffset(0, 32, 0));
    public static final DeferredBlock<FacingFurnitureBlock> SASH_BANNER_WALL = piece("sash_banner_wall", "badger_crate",
            p -> new FacingFurnitureBlock(p, true, () -> DecorFurniture.SASH_BANNER.get()));
    public static final DeferredBlock<FurnitureBlock> WOODEN_CART = piece("wooden_cart", "badger_crate",
            p -> new FurnitureBlock(p, false, Form.FURNITURE));
    public static final DeferredBlock<FurnitureBlock> STALL_CART = piece("stall_cart", "badger_crate",
            p -> new FurnitureBlock(p, true, Form.FURNITURE));
    /** Animated: Ren's GeckoLib model, {@code geckolib/models/furniture/wooden_windmill.geo.json}, its sails turning. */
    public static final DeferredBlock<AnimatedFurnitureBlock> WOODEN_WINDMILL = piece("wooden_windmill", "badger_crate",
            p -> new AnimatedFurnitureBlock(p, "furniture", true, "wooden_windmill", null));

    private DecorFurniture() {}

    /** Every decor furniture block, in registration order. */
    public static List<DeferredBlock<? extends FurnitureBlock>> all() {
        return List.copyOf(ALL);
    }

    /**
     * Registers a piece and its item: wood, breaking quickly with anything (the Crates mod's furniture), with the
     * crate's gold exclusive line when it has one.
     */
    private static <B extends FurnitureBlock> DeferredBlock<B> piece(String name, @Nullable String crate,
            Function<BlockBehaviour.Properties, ? extends B> factory) {
        DeferredBlock<B> block = R.blocks().registerBlock(name, factory, () -> FurnitureFootprints.properties(name,
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
        R.items().registerItem(name, properties -> new FurnitureBlockItem(block.get(), properties), properties -> {
            properties.useBlockDescriptionPrefix();
            if (crate != null) {
                properties.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.apeira.exclusive." + crate)
                        .withStyle(Style.EMPTY.withColor(CosmeticItem.EXCLUSIVE_COLOR).withItalic(false)))));
            }
            return properties;
        });
        ALL.add(block);
        return block;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
