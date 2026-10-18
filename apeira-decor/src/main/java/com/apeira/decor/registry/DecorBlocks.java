package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.block.OakChairBlock;
import com.apeira.decor.block.PaintingTableBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Apeira's furniture and the painting table. Each is registered with a plain block item of the same name. */
public final class DecorBlocks {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** A chair whose seat cushion can be painted. */
    public static final DeferredBlock<OakChairBlock> OAK_CHAIR = R.blockWithItem("oak_chair", OakChairBlock::new, DecorBlocks::wood);
    /** Opens the painting table menu, which paints furniture items and paintbrushes. */
    public static final DeferredBlock<PaintingTableBlock> PAINTING_TABLE = R.blockWithItem("painting_table", PaintingTableBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)
                    .ignitedByLava());

    private DecorBlocks() {}

    /** Wooden furniture: oak planks' hardness, sound and flammability. Not a full cube, so it hides no faces. */
    private static BlockBehaviour.Properties wood(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.WOOD)
                .ignitedByLava().noOcclusion();
    }

    public static void init(ApeiraRegistrar registrar) {}
}
