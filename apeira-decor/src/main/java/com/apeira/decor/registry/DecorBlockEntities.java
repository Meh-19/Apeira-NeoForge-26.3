package com.apeira.decor.registry;

import java.util.HashSet;
import java.util.Set;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.decor.crate.CrateBlockEntity;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block entities of the decor module. */
public final class DecorBlockEntities {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /** {@code apeira:paint}: the paint of a paintable block. Add every new {@code PaintableBlock} here. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintBlockEntity>> PAINT = R.blockEntityType("paint",
            PaintBlockEntity::new, DecorBlocks.OAK_CHAIR);

    /**
     * {@code apeira:crate}: a crate's opening. Valid for every {@link CrateBlock}, of any module: the blocks are
     * collected from the block registry, which is filled before block entity types are registered.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE = R.blockEntityTypes().register(
            "crate", () -> new BlockEntityType<>(CrateBlockEntity::new, crateBlocks()));

    private DecorBlockEntities() {}

    private static Set<Block> crateBlocks() {
        Set<Block> blocks = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof CrateBlock) {
                blocks.add(block);
            }
        }
        return Set.copyOf(blocks);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
