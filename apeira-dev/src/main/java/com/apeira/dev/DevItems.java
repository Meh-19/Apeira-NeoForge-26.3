package com.apeira.dev;

import com.apeira.core.registry.ApeiraRegistrar;

import net.neoforged.neoforge.registries.DeferredItem;

/** The dev module's items. */
public final class DevItems {
    public static final DeferredItem<FurnitureDevWandItem> FURNITURE_DEV_WAND = ApeiraDev.REGISTRAR.items()
            .registerItem("furniture_dev_wand", FurnitureDevWandItem::new, properties -> properties.stacksTo(1));

    private DevItems() {}

    public static void init(ApeiraRegistrar registrar) {}
}
