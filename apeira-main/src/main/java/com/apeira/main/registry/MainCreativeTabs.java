package com.apeira.main.registry;

import java.util.List;
import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.furniture.FurnitureBlock;

import net.minecraft.core.Holder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Apeira's creative tabs. Tab titles are in {@link com.apeira.main.datagen.MainLanguageProvider}. */
public final class MainCreativeTabs {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * "Apeira Content": every block and item of the main module but the crates, in registration order (items, then
     * mineral blocks, then woods), followed by the decor module's items.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENT = R.creativeTab("content", tab -> tab
            .icon(MainItems.RUBY::toStack)
            .displayItems(Stream.concat(R.items().getEntries().stream(), ApeiraDecor.REGISTRAR.items().getEntries().stream())
                    .filter(item -> !item.getId().equals(MainBadgerCrate.BADGER_CRATE.getId()))
                    // Furniture wall and ceiling variants are placed through their base piece.
                    .filter(item -> !(item.get() instanceof BlockItem block && block.getBlock() instanceof FurnitureBlock furniture
                            && furniture.dropBlock() != furniture))
                    .toList()));

    /** "Crates": the Badger Crate. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRATES = R.creativeTab("crates", tab -> tab
            .icon(MainBadgerCrate.BADGER_CRATE::toStack)
            .withTabsBefore(CONTENT.getId())
            .displayItems(List.<Holder<? extends ItemLike>>of(MainBadgerCrate.BADGER_CRATE)));

    private MainCreativeTabs() {}

    public static void init(ApeiraRegistrar registrar) {}
}
