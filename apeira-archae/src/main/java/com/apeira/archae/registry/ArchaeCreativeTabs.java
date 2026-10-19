package com.apeira.archae.registry;

import java.util.ArrayList;
import java.util.List;

import com.apeira.archae.ApeiraArchae;
import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.registry.DecorBlocks;
import com.apeira.decor.registry.DecorItems;

import net.minecraft.core.Holder;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The archae module's creative tab. Its title is in {@link com.apeira.archae.datagen.ArchaeLanguageProvider}. */
public final class ArchaeCreativeTabs {
    /**
     * "Argon Archaeology" ({@code apeira:archaeology}): Argon's two tabs of that name in one, in their order. The
     * assembly table, the decor module's painting table and paintbrush (Argon's own copies of them are not ported), the
     * fragments, the artifacts without their wall, ceiling and water variants, then the cosmetics (Argon's "Argon
     * Cosmetics" tab).
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCHAEOLOGY = ApeiraArchae.REGISTRAR.creativeTab("archaeology",
            tab -> tab.icon(ArchaeBlocks.ASSEMBLY_TABLE::toStack).displayItems(contents()));

    private ArchaeCreativeTabs() {}

    private static List<Holder<? extends ItemLike>> contents() {
        List<Holder<? extends ItemLike>> items = new ArrayList<>();
        items.add(ArchaeBlocks.ASSEMBLY_TABLE);
        items.add(DecorBlocks.PAINTING_TABLE);
        items.add(DecorItems.PAINTBRUSH);
        items.addAll(ArchaeItems.all());
        ArchaeArtifacts.all().stream().filter(artifact -> !artifact.variant()).forEach(artifact -> items.add(artifact.item()));
        ArchaeCosmetics.all().forEach(cosmetic -> items.add(cosmetic.item()));
        return items;
    }

    public static void init(ApeiraRegistrar registrar) {}
}
