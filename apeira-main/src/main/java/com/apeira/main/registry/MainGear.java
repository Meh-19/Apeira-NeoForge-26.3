package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.main.item.CaptureNetItem;

import net.neoforged.neoforge.registries.DeferredItem;

/** Xenon's tools and gear: the capture net. */
public final class MainGear {
    public static final DeferredItem<CaptureNetItem> CAPTURE_NET = Apeira.REGISTRAR.items().registerItem("capture_net", CaptureNetItem::new);

    private MainGear() {}

    public static void init(ApeiraRegistrar registrar) {}
}
