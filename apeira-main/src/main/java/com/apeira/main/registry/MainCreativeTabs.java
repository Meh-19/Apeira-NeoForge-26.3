package com.apeira.main.registry;

import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.main.Apeira;

import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Apeira's creative tabs. Tab titles are in {@link com.apeira.main.datagen.MainLanguageProvider}. */
public final class MainCreativeTabs {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * "Apeira Content": every block and item of the main module, in registration order (blocks first), followed by the
     * decor module's furniture.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENT = R.creativeTab("content", tab -> tab
            .icon(MainItems.APEIRA_GEM::toStack)
            .displayItems(Stream.concat(R.items().getEntries().stream(), ApeiraDecor.REGISTRAR.items().getEntries().stream()).toList()));

    private MainCreativeTabs() {}

    public static void init(ApeiraRegistrar registrar) {}
}
