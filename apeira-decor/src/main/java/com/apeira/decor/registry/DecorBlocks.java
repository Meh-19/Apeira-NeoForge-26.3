package com.apeira.decor.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.block.PaintingTableBlock;
import com.apeira.decor.multiblock.MultiblockPartBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The painting table, registered with a plain block item of the same name, and the multiblock part. The furniture is
 * {@link DecorFurniture}.
 */
public final class DecorBlocks {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** Opens the painting table menu, which paints furniture items and paintbrushes. */
    public static final DeferredBlock<PaintingTableBlock> PAINTING_TABLE = R.blockWithItem("painting_table", PaintingTableBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)
                    .ignitedByLava());

    /**
     * The other blocks of a multiblock piece ({@link com.apeira.decor.multiblock.MultiblockController}): no item and no
     * loot (the controller drops the piece), a shape that depends on the controller's state, never moved by pistons.
     */
    public static final DeferredBlock<MultiblockPartBlock> MULTIBLOCK_PART = R.blocks().registerBlock("multiblock_part",
            MultiblockPartBlock::new, () -> BlockBehaviour.Properties.of().strength(0.5F).noOcclusion().noLootTable().dynamicShape()
                    .pushReaction(PushReaction.IMMOVEABLE));

    private DecorBlocks() {}

    public static void init(ApeiraRegistrar registrar) {}
}
