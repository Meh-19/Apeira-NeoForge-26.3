package com.apeira.archae.datagen;

import java.util.Locale;

import com.apeira.archae.item.FragmentItem;
import com.apeira.archae.registry.ArchaeArtifacts;
import com.apeira.archae.registry.ArchaeBlocks;
import com.apeira.archae.registry.ArchaeItems;
import com.apeira.core.datagen.ApeiraLanguageProvider;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * English names and texts of the archae module, Argon's wording. Argon named only a few of its blocks and items, all
 * as their id in title case, so every name here is the id in title case.
 */
public final class ArchaeLanguageProvider extends ApeiraLanguageProvider {
    public ArchaeLanguageProvider(PackOutput output, String namespace) {
        super(output, namespace);
    }

    @Override
    protected void addTranslations() {
        block(ArchaeBlocks.ASSEMBLY_TABLE, "Assembly Table");
        for (DeferredItem<FragmentItem> fragment : ArchaeItems.all()) {
            item(fragment, title(fragment.getId().getPath()));
        }
        for (ArchaeArtifacts.Artifact artifact : ArchaeArtifacts.all()) {
            block(artifact.block(), title(artifact.block().getId().getPath()));
        }

        creativeTab("archaeology", "Argon Archaeology");
        add("tag.item." + namespace() + ".fragments", "Fragments");
        tooltip("assemble_instruction_line1", "Bring to an Assembly Table");
        tooltip("assemble_instruction_line2", "to assemble Artifacts");
        tooltip("exclusive.archaeology", "Archeology Exclusive");

        container("assembly_table", "Assembly Table");
        container("assembly_table.info1", "Insert fragments to assemble artifacts.");
        container("assembly_table.info2", "Fragment cost varies by item.");
        container("assembly_table.cost", "Cost: %s");
    }

    /** {@code coin_pile} -> {@code Coin Pile}. */
    private static String title(String id) {
        StringBuilder title = new StringBuilder();
        for (String word : id.split("_")) {
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return title.toString();
    }
}
