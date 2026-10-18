package com.apeira.main.registry;

import java.util.List;

import com.apeira.core.registry.ApeiraRegistrar;
import com.apeira.main.Apeira;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The legacy ORForged mod's mineral blocks: platinum and ruby blocks, bricks, pillars and ores, and diamond bricks.
 * Each is registered with a plain block item of the same name. Its woods are in {@link MainWoods}.
 */
public final class MainBlocks {
    private static final ApeiraRegistrar R = Apeira.REGISTRAR;

    public static final DeferredBlock<Block> PLATINUM_BLOCK = R.blockWithItem("platinum_block", Block::new, MainBlocks::metal, p -> p);
    public static final DeferredBlock<Block> PLATINUM_BRICKS = R.blockWithItem("platinum_bricks", Block::new, MainBlocks::metal, p -> p);
    public static final DeferredBlock<RotatedPillarBlock> PLATINUM_PILLAR = R.blockWithItem("platinum_pillar", RotatedPillarBlock::new,
            MainBlocks::metal, p -> p);
    public static final DeferredBlock<DropExperienceBlock> PLATINUM_ORE = ore("platinum_ore");
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_PLATINUM_ORE = ore("deepslate_platinum_ore");

    public static final DeferredBlock<Block> RUBY_BLOCK = R.blockWithItem("ruby_block", Block::new, MainBlocks::metal, p -> p);
    public static final DeferredBlock<Block> RUBY_BRICKS = R.blockWithItem("ruby_bricks", Block::new, MainBlocks::metal, p -> p);
    public static final DeferredBlock<RotatedPillarBlock> RUBY_PILLAR = R.blockWithItem("ruby_pillar", RotatedPillarBlock::new,
            MainBlocks::metal, p -> p);
    public static final DeferredBlock<DropExperienceBlock> RUBY_ORE = ore("ruby_ore");
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_RUBY_ORE = ore("deepslate_ruby_ore");

    /** Diamond bricks, with a diamond block's properties. */
    public static final DeferredBlock<Block> DIAMOND_BRICK = R.blockWithItem("diamond_brick", Block::new,
            () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.DIAMOND_BLOCK), p -> p);

    private MainBlocks() {}

    /** Platinum's and ruby's storage blocks, bricks and pillars: an iron block's properties with stone sounds. */
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofLegacyCopy(Blocks.IRON_BLOCK).strength(5.0F, 6.0F).sound(SoundType.STONE);
    }

    /** An ore with stone's properties but hardness 2, dropping 2 to 5 experience; ORForged's deepslate ores are the same. */
    private static DeferredBlock<DropExperienceBlock> ore(String name) {
        return R.blockWithItem(name, p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
                () -> BlockBehaviour.Properties.ofLegacyCopy(Blocks.STONE).strength(2.0F), p -> p);
    }

    public static List<DeferredBlock<DropExperienceBlock>> ores() {
        return List.of(PLATINUM_ORE, DEEPSLATE_PLATINUM_ORE, RUBY_ORE, DEEPSLATE_RUBY_ORE);
    }

    public static void init(ApeiraRegistrar registrar) {}
}
