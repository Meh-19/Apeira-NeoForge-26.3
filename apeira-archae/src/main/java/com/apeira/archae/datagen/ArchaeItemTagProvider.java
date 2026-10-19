package com.apeira.archae.datagen;

import java.util.Objects;

import com.apeira.archae.registry.ArchaeArtifacts;
import com.apeira.archae.registry.ArchaeTags;
import com.apeira.core.datagen.ApeiraDataGenerators;
import com.apeira.core.datagen.ApeiraItemTagsProvider;

import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.registries.DeferredItem;

/** Item tags of the archae module: {@link ArchaeTags#FRAGMENTS}, every fragment an artifact is made from (Argon's tag). */
public final class ArchaeItemTagProvider extends ApeiraItemTagsProvider {
    public ArchaeItemTagProvider(ApeiraDataGenerators.Context context) {
        super(context);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        add(ArchaeTags.FRAGMENTS, ArchaeArtifacts.all().stream().map(ArchaeArtifacts.Artifact::fragment).filter(Objects::nonNull).distinct()
                .toArray(DeferredItem[]::new));
    }
}
