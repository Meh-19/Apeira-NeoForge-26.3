package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.item.DolomiteDustItem;
import com.apeira.main.item.TurkeyItem;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The legacy Xenon mod's items that are not blocks or tools: raw and cooked turkey (placed as a turkey to eat in bites;
 * cooked turkey is raw turkey cooked in a furnace, smoker or campfire), blight essence (no use in Xenon), dolomite dust and
 * the crocodile tooth.
 */
public final class MainMaterials {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredItem<Item> BLIGHT_ESSENCE = R.items().registerSimpleItem("blight_essence");
    public static final DeferredItem<DolomiteDustItem> DOLOMITE_DUST = R.items().registerItem("dolomite_dust", DolomiteDustItem::new);
    public static final DeferredItem<TurkeyItem> RAW_TURKEY = R.items().registerItem("raw_turkey",
            p -> new TurkeyItem(p, MainSpecialBlocks.RAW_TURKEY_BLOCK));
    public static final DeferredItem<TurkeyItem> COOKED_TURKEY = R.items().registerItem("cooked_turkey",
            p -> new TurkeyItem(p, MainSpecialBlocks.COOKED_TURKEY_BLOCK));
    /** Dropped by crocodiles killed by a player; Xenon gave it no other use. */
    public static final DeferredItem<Item> CROCODILE_TOOTH = R.items().registerSimpleItem("crocodile_tooth");

    private MainMaterials() {}

    public static void init(ApeiraRegistrar registrar) {}
}
