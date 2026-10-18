package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.item.PaintbrushItem;

import net.neoforged.neoforge.registries.DeferredItem;

/** Items of the decor module that are not block items. */
public final class DecorItems {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** Paints paintable furniture with its paint; 300 uses, like CosmoLib's. */
    public static final DeferredItem<PaintbrushItem> PAINTBRUSH = R.items().registerItem("paintbrush", PaintbrushItem::new,
            properties -> properties.durability(300));

    private DecorItems() {}

    public static void init(ApeiraRegistrar registrar) {}
}
