package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places a small cluster of 1-3 shelf fungus on the ground near each other.
 * Each piece gets a random rotation.
 */
public record ShelfFungusClusterFeature() implements Feature {
    public static final MapCodec<ShelfFungusClusterFeature> CODEC = MapCodec.unit(ShelfFungusClusterFeature::new);

    @Override
    public MapCodec<ShelfFungusClusterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int count    = 1 + random.nextInt(3); // 1-3 pieces
        int placed   = 0;
        int maxTries = count * 6;

        for (int attempt = 0; attempt < maxTries && placed < count; attempt++) {
            int dx = random.nextInt(5) - 2; // spread of ±2
            int dz = random.nextInt(5) - 2;

            BlockPos candidate = findSurface(world, origin.offset(dx, 0, dz));
            if (candidate == null) continue;

            BlockState below = world.getBlockState(candidate.below());
            if (!below.is(BlockTags.DIRT)
                    && !below.is(Blocks.GRASS_BLOCK)
                    && !below.is(Blocks.MOSS_BLOCK)
                    && !below.is(BlockTags.LOGS)) continue;

            if (!world.getBlockState(candidate).isAir()) continue;

            world.setBlock(candidate,
                    MainPlants.SHELF_FUNGUS.get().defaultBlockState()
                            .setValue(RandomRotationBlock.ROTATION, random.nextInt(4)),
                    Block.UPDATE_ALL);
            placed++;
        }

        return placed > 0;
    }

    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState below   = world.getBlockState(candidate.below());
            BlockState at      = world.getBlockState(candidate);
            if (!below.isAir() && !below.is(Blocks.WATER) && at.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}