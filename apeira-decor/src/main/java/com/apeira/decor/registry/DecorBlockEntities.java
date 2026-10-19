package com.apeira.decor.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.decor.ApeiraDecor;
import com.apeira.decor.crate.CrateBlock;
import com.apeira.decor.crate.CrateBlockEntity;
import com.apeira.decor.furniture.FurnitureBlockEntity;
import com.apeira.decor.paint.PaintBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Block entities of the decor module. */
public final class DecorBlockEntities {
    private static final ApeiraRegistrar R = ApeiraDecor.REGISTRAR;

    /**
     * {@code apeira:paint}: the paint of a paintable block that has no block entity of its own. Add every such
     * {@code PaintableBlock} here; there are none yet, since furniture and artifacts keep their paint in their own
     * block entities.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PaintBlockEntity>> PAINT = R.blockEntityTypes().register(
            "paint", () -> new BlockEntityType<>(PaintBlockEntity::new, Set.of()));

    /** {@code apeira:furniture}: every furniture piece, drawn by {@code FurnitureRenderer}, and its paint. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnitureBlockEntity>> FURNITURE = R.blockEntityTypes().register(
            "furniture", () -> new BlockEntityType<>(FurnitureBlockEntity::new, blocks(DecorFurniture.all())));

    /**
     * {@code apeira:crate}: a crate's opening. Valid for every {@link CrateBlock}, of any module: the blocks are
     * collected from the block registry, which is filled before block entity types are registered.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE = R.blockEntityTypes().register(
            "crate", () -> new BlockEntityType<>(CrateBlockEntity::new, crateBlocks()));

    private DecorBlockEntities() {}

    private static Set<Block> blocks(List<? extends Supplier<? extends Block>> blocks) {
        return blocks.stream().map(Supplier::get).collect(Collectors.toUnmodifiableSet());
    }

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
