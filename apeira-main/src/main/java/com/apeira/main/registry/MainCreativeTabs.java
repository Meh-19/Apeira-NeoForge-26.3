package com.apeira.main.registry;

import java.util.stream.Stream;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.cosmetic.CosmeticItem;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.main.Apeira;

import net.minecraft.core.Holder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Apeira's creative tabs. Tab titles are in {@link com.apeira.main.datagen.MainLanguageProvider}. */
public final class MainCreativeTabs {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    /**
     * "Apeira Content": every block and item of the main module but the cosmetics and crates, in registration order
     * (blocks first), followed by the decor module's furniture.
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENT = R.creativeTab("content", tab -> tab
            .icon(MainItems.APEIRA_GEM::toStack)
            .displayItems(Stream.concat(R.items().getEntries().stream(), ApeiraDecor.REGISTRAR.items().getEntries().stream())
                    .filter(item -> !(item.get() instanceof CosmeticItem)
                            && !(item.get() instanceof BlockItem block && block.getBlock() instanceof CrateBlock))
                    .toList()));

    /** "Cosmetics": the crates, then the crate cosmetics, grouped by crate as in {@link MainCosmetics}. */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COSMETICS = R.creativeTab("cosmetics", tab -> tab
            .icon(MainCosmetics.WESTERN_HAT::toStack)
            .withTabsBefore(CONTENT.getId())
            .displayItems(Stream.<Holder<? extends ItemLike>>concat(MainCrates.all().stream(),
                    MainCosmetics.all().stream().map(MainCosmetics.Cosmetic::item)).toList()));

    private MainCreativeTabs() {}

    public static void init(ApeiraRegistrar registrar) {}
}
