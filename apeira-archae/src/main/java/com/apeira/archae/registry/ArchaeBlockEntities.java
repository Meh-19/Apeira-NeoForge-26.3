package com.apeira.archae.registry;

import java.util.Set;
import java.util.stream.Collectors;

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.block.AnimatedArtifactBlock;
import com.apeira.archae.block.AnimatedArtifactBlockEntity;
import com.apeira.archae.block.ArtifactBlockEntity;
import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block entities of the archae module, with Argon's ids. */
public final class ArchaeBlockEntities {
    private static final ApeiraRegistrar R = ApeiraArchae.REGISTRAR;

    /** {@code apeira:artifact_display}: an artifact's paint, drawn by {@code ArtifactRenderer}. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArtifactBlockEntity>> ARTIFACT_DISPLAY = R.blockEntityTypes()
            .register("artifact_display", () -> new BlockEntityType<>(
                    (pos, state) -> new ArtifactBlockEntity(ArchaeBlockEntities.ARTIFACT_DISPLAY.get(), pos, state), artifacts(false)));

    /** {@code apeira:generic_animated_furniture}: an animated artifact's paint and GeckoLib animation. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnimatedArtifactBlockEntity>> ANIMATED_ARTIFACT = R.blockEntityTypes()
            .register("generic_animated_furniture", () -> new BlockEntityType<>(
                    (pos, state) -> new AnimatedArtifactBlockEntity(ArchaeBlockEntities.ANIMATED_ARTIFACT.get(), pos, state), artifacts(true)));

    private ArchaeBlockEntities() {}

    private static Set<Block> artifacts(boolean animated) {
        return ArchaeArtifacts.all().stream().map(artifact -> (Block) artifact.block().get())
                .filter(block -> block instanceof AnimatedArtifactBlock == animated).collect(Collectors.toUnmodifiableSet());
    }

    public static void init(ApeiraRegistrar registrar) {}
}
