package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.block.PaintingTableBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/** The painting table, registered with a plain block item of the same name. The furniture is {@link DecorFurniture}. */
public final class DecorBlocks {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** Opens the painting table menu, which paints furniture items and paintbrushes. */
    public static final DeferredBlock<PaintingTableBlock> PAINTING_TABLE = R.blockWithItem("painting_table", PaintingTableBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)
                    .ignitedByLava());

    private DecorBlocks() {}

    public static void init(ApeiraRegistrar registrar) {}
}
