package com.apeira.main.world.feature;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.apeira.main.registry.MainWoods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Xenon's fir tree, placed from its structure files. The five shapes are Xenon's {@code green_fir_0-4} /
 * {@code orange_fir_0-4} (each shape existed once per colour, block for block the same), shipped once as
 * {@code data/apeira/structure/fir/fir_<n>.nbt} in size order (0 the 3x8 sapling-sized tree ... 4 the 7x20 giant),
 * drawn with green leaves. Each tree rolls its shape ({@link #WEIGHTS}), its leaf colour (green or orange, half each),
 * a quarter turn and a mirror (about the trunk), the same in worldgen and from a sapling. The trunk's bottom log stands on
 * {@code origin}; logs replace air, water and replaceable plants, leaves only air (persistent, as before).
 */
public record FirTreeFeature() implements Feature {
    public static final MapCodec<FirTreeFeature> CODEC = MapCodec.unit(FirTreeFeature::new);

    private static final int MIN_SPACING = 5;
    /** Odds of shapes 0-4, out of their sum: tiny, small, medium, tall, giant. */
    private static final int[] WEIGHTS = {4, 5, 6, 3, 2};
    private static final int TOTAL = 20;

    private static Identifier shape(int n) {
        return ApeiraIds.id("fir/fir_" + n);
    }

    @Override
    public MapCodec<FirTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.MOSS_BLOCK)) return false;
        // Spacing keeps generated forests apart; a sapling grows wherever it was planted, next to other trees too.
        if (!(world instanceof ServerLevel) && hasNearbyLog(world, origin, MIN_SPACING)) return false;

        int roll = random.nextInt(TOTAL);
        int n = 0;
        while (roll >= WEIGHTS[n]) roll -= WEIGHTS[n++];
        Optional<StructureTemplate> found = world.getLevel().getStructureTemplateManager().get(shape(n));
        if (found.isEmpty()) {
            Apeira.LOGGER.warn("Missing fir structure {}", shape(n));
            return false;
        }
        StructureTemplate template = found.get();

        Block log = MainWoods.FIR.log().get();
        Block templateLeaves = MainWoods.FIR.leaves().get(0).get();
        Block leaves = MainWoods.FIR.leaves().get(random.nextBoolean() ? 0 : 1).get();

        // The trunk is the template's centre column; each block is mirrored and turned about it, so it lands on origin.
        // (StructurePlaceSettings mirrors about x = 0, not its pivot, so the turn is done here.)
        Vec3i size = template.getSize();
        int cx = size.getX() / 2;
        int cz = size.getZ() / 2;
        boolean mirror = random.nextBoolean();
        int turn = random.nextInt(4);
        StructurePlaceSettings plain = new StructurePlaceSettings();

        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, plain, log, false)) {
            placeLog(world, at(origin, info.pos(), cx, cz, mirror, turn), info.state());
        }
        BlockState leaf = leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, plain, templateLeaves, false)) {
            BlockPos pos = at(origin, info.pos(), cx, cz, mirror, turn);
            if (world.getBlockState(pos).isAir()) world.setBlock(pos, leaf, Block.UPDATE_ALL);
        }
        return true;
    }

    /** A template position, relative to its trunk column, mirrored, turned by quarter turns and moved to origin. */
    private static BlockPos at(BlockPos origin, BlockPos rel, int cx, int cz, boolean mirror, int turn) {
        int dx = rel.getX() - cx;
        int dz = rel.getZ() - cz;
        if (mirror) dx = -dx;
        for (int i = 0; i < turn; i++) {
            int t = dx;
            dx = -dz;
            dz = t;
        }
        return origin.offset(dx, rel.getY(), dz);
    }

    private static void placeLog(WorldGenLevel world, BlockPos pos, BlockState log) {
        BlockState s = world.getBlockState(pos);
        if (s.isAir() || s.is(Blocks.WATER) || s.canBeReplaced()) {
            world.setBlock(pos, log, Block.UPDATE_ALL);
        }
    }

    private static boolean hasNearbyLog(WorldGenLevel world, BlockPos origin, int radius) {
        Block log = MainWoods.FIR.log().get();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -1; dy <= 22; dy++) {
                    if (world.getBlockState(origin.offset(dx, dy, dz)).is(log)) return true;
                }
            }
        }
        return false;
    }
}
