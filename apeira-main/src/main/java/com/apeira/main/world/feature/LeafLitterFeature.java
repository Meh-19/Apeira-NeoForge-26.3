package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters dried leaf litter on the ground.
 * Only places on top of grass blocks — never floats above plants or leaves.
 * Produces a natural irregular patch shape.
 */
public record LeafLitterFeature() implements Feature {
    public static final MapCodec<LeafLitterFeature> CODEC = MapCodec.unit(LeafLitterFeature::new);

    @Override
    public MapCodec<LeafLitterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius  = 2 + random.nextInt(3); // patch radius 2-4
        int tries   = 4 + random.nextInt(6); // 4-9 pieces per patch
        int placed  = 0;

        for (int attempt = 0; attempt < tries * 3 && placed < tries; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;

            // Circular shape
            if (dx * dx + dz * dz > radius * radius) continue;

            BlockPos check = origin.offset(dx, 0, dz);

            // Scan a small range to find the surface
            BlockPos surface = findGrassSurface(world, check);
            if (surface == null) continue;

            // Place leaf litter on top of the grass block
            BlockPos placePos = surface.above();
            if (!world.getBlockState(placePos).isAir()) continue;

            world.setBlock(placePos,
                    MainPlants.DRIED_LEAF_LITTER.get().defaultBlockState()
                            .setValue(RandomRotationBlock.ROTATION, random.nextInt(4)),
                    Block.UPDATE_ALL);
            placed++;
        }

        return placed > 0;
    }

    /**
     * Scans ±3 Y to find a grass block with air above it.
     * Returns the grass block position (not the air above it).
     */
    private static BlockPos findGrassSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            if (state.is(Blocks.GRASS_BLOCK) && above.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}