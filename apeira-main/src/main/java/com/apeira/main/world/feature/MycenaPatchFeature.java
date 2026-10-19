package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

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
 * Combined mycena patch — scatters orange_mycena and tall_orange_mycena,
 * with a ~20% chance of also spawning a full OrangeMycenaFeature mushroom
 * in the same patch. Replaces both MYCENA_PATCH and ORANGE_MYCENA_FEATURE
 * placed features in world gen.
 */
public record MycenaPatchFeature() implements Feature {
    public static final MapCodec<MycenaPatchFeature> CODEC = MapCodec.unit(MycenaPatchFeature::new);

    @Override
    public MapCodec<MycenaPatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int placed  = 0;
        int tries   = 12 + random.nextInt(12); // 12-24 attempts per patch

        for (int i = 0; i < tries; i++) {
            int dx = random.nextInt(9) - 4;
            int dz = random.nextInt(9) - 4;

            BlockPos surface = findSurface(world, origin.offset(dx, 0, dz));
            if (surface == null) continue;

            BlockState below = world.getBlockState(surface.below());
            if (!below.is(BlockTags.DIRT) && !below.is(Blocks.GRASS_BLOCK)) continue;
            if (!world.getBlockState(surface).isAir()) continue;

            // 35% tall, 65% short
            BlockState decor = random.nextFloat() < 0.35f
                    ? MainPlants.TALL_ORANGE_MYCENA.get().defaultBlockState()
                    : MainPlants.ORANGE_MYCENA.get().defaultBlockState();

            world.setBlock(surface, decor, Block.UPDATE_ALL);
            placed++;
        }

        // ~20% chance to also spawn a full mushroom nearby
        if (random.nextFloat() < 0.20f) {
            int dx = random.nextInt(7) - 3;
            int dz = random.nextInt(7) - 3;
            BlockPos mushroomPos = findSurface(world, origin.offset(dx, 0, dz));
            if (mushroomPos != null) {
                OrangeMycenaFeature.generateAt(world, mushroomPos, random);

            }
        }

        return placed > 0;
    }

    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos c = target.offset(0, dy, 0);
            BlockState below = world.getBlockState(c.below());
            BlockState at    = world.getBlockState(c);
            if (!below.isAir() && !below.is(Blocks.WATER) && at.isAir()) {
                return c;
            }
        }
        return null;
    }
}