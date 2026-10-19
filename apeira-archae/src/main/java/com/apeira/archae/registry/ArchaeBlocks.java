package com.apeira.archae.registry;

import com.apeira.archae.ApeiraArchae;
import com.apeira.archae.assembly.AssemblyTableBlock;
import com.apeira.core.registry.ApeiraRegistrar;

import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The archae module's blocks besides the {@link ArchaeArtifacts}. */
public final class ArchaeBlocks {
    /** Argon's assembly table: wood, strength 2/3, lava sets it alight (Argon's {@code BlockBuilders#assemblyTable}). */
    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE = ApeiraArchae.REGISTRAR.blockWithItem("assembly_table",
            AssemblyTableBlock::new, properties -> properties.ignitedByLava().strength(2.0F, 3.0F).sound(SoundType.WOOD));

    private ArchaeBlocks() {}

    public static void init(ApeiraRegistrar registrar) {}
}
