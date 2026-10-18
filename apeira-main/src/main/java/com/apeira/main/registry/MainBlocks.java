package com.apeira.main.registry;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Apeira's blocks. Each is registered with a plain block item of the same name. */
public final class MainBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredBlock<Block> CARVED_APEIRA_STONE = R.blockWithItem("carved_apeira_stone", Block::new, MainBlocks::stone);
    public static final DeferredBlock<Block> APEIRA_BRICK = R.blockWithItem("apeira_brick", Block::new, MainBlocks::stone);

    private MainBlocks() {}

    /** Stone bricks' hardness, blast resistance and sound. */
    private static BlockBehaviour.Properties stone(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
