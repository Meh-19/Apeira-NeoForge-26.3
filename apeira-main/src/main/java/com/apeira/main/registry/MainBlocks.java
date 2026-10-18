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
 * The legacy ORForged mod's mineral blocks: platinum and ruby blocks, bricks, pillars and ores, and bricks and pillars
 * of the vanilla minerals. Each is registered with a plain block item of the same name. Its woods are in
 * {@link MainWoods}, its stone variants in {@link MainStones} and its lanterns in {@link MainLanterns}.
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
    public static final DeferredBlock<RotatedPillarBlock> DIAMOND_PILLAR = mineralPillar("diamond_pillar", Blocks.DIAMOND_BLOCK);
    public static final DeferredBlock<Block> EMERALD_BRICK = mineralBrick("emerald_brick", Blocks.EMERALD_BLOCK);
    public static final DeferredBlock<RotatedPillarBlock> EMERALD_PILLAR = mineralPillar("emerald_pillar", Blocks.EMERALD_BLOCK);
    public static final DeferredBlock<Block> GOLD_BRICK = mineralBrick("gold_brick", Blocks.GOLD_BLOCK);
    public static final DeferredBlock<RotatedPillarBlock> GOLD_PILLAR = mineralPillar("gold_pillar", Blocks.GOLD_BLOCK);
    public static final DeferredBlock<Block> IRON_BRICK = mineralBrick("iron_brick", Blocks.IRON_BLOCK);
    public static final DeferredBlock<RotatedPillarBlock> IRON_PILLAR = mineralPillar("iron_pillar", Blocks.IRON_BLOCK);
    public static final DeferredBlock<Block> NETHERITE_BRICK = mineralBrick("netherite_brick", Blocks.NETHERITE_BLOCK);
    public static final DeferredBlock<RotatedPillarBlock> NETHERITE_PILLAR = mineralPillar("netherite_pillar", Blocks.NETHERITE_BLOCK);

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

    /** Bricks of a vanilla mineral, with its block's properties. */
    private static DeferredBlock<Block> mineralBrick(String name, Block mineral) {
        return R.blockWithItem(name, Block::new, () -> BlockBehaviour.Properties.ofLegacyCopy(mineral), p -> p);
    }

    /**
     * A pillar of a vanilla mineral. ORForged copied oak logs' properties and set the mineral block's strength, sound
     * and map colour; copying the mineral block gives the same without the log's.
     */
    private static DeferredBlock<RotatedPillarBlock> mineralPillar(String name, Block mineral) {
        return R.blockWithItem(name, RotatedPillarBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(mineral), p -> p);
    }

    /** The vanilla minerals' bricks and pillars, diamond's first. */
    public static List<Mineral> vanillaMinerals() {
        return List.of(new Mineral(DIAMOND_BRICK, DIAMOND_PILLAR, Blocks.DIAMOND_BLOCK),
                new Mineral(EMERALD_BRICK, EMERALD_PILLAR, Blocks.EMERALD_BLOCK), new Mineral(GOLD_BRICK, GOLD_PILLAR, Blocks.GOLD_BLOCK),
                new Mineral(IRON_BRICK, IRON_PILLAR, Blocks.IRON_BLOCK), new Mineral(NETHERITE_BRICK, NETHERITE_PILLAR, Blocks.NETHERITE_BLOCK));
    }

    public static List<DeferredBlock<DropExperienceBlock>> ores() {
        return List.of(PLATINUM_ORE, DEEPSLATE_PLATINUM_ORE, RUBY_ORE, DEEPSLATE_RUBY_ORE);
    }

    public static void init(ApeiraRegistrar registrar) {}

    /** Bricks and a pillar made from a vanilla mineral's {@code block}. */
    public record Mineral(DeferredBlock<Block> brick, DeferredBlock<RotatedPillarBlock> pillar, Block block) {}
}
