package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.DecorationBlock;
import com.apeira.main.block.RandomRotationBlock;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainStones;

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
 * Places an irregular disk of dried silt and packed dirt by directly
 * replacing grass/dirt surface blocks.
 * After placing each block, has a small chance to place decorations
 * on top: brown mushroom, shelf fungus, or rocks/pebbles.
 */
public record SurfacePatchFeature() implements Feature {
    public static final MapCodec<SurfacePatchFeature> CODEC = MapCodec.unit(SurfacePatchFeature::new);

    @Override
    public MapCodec<SurfacePatchFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int radius  = 4 + random.nextInt(3); // radius 4-6, up from 2-4
        int placed  = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                float dist = dx * dx + dz * dz;
                if (dist > radius * radius + random.nextFloat()) continue;

                BlockPos surface = findReplaceableSurface(world, origin.offset(dx, 0, dz));
                if (surface == null) continue;

                // 60% dried silt, 40% packed dirt
                BlockState replacement = random.nextFloat() < 0.6f
                        ? MainStones.DRIED_SILT.get().defaultBlockState()
                        : MainStones.PACKED_DIRT.get().defaultBlockState();

                world.setBlock(surface, replacement, Block.UPDATE_ALL);
                placed++;

                // Decorations on top — ~15% chance per block
                if (random.nextFloat() < 0.15f) {
                    BlockPos above = surface.above();
                    if (world.getBlockState(above).isAir()) {
                        world.setBlock(above, pickDecoration(random), Block.UPDATE_ALL);
                    }
                }
            }
        }

        return placed > 0;
    }

    /**
     * Randomly picks one of: brown mushroom, shelf fungus (random rotation),
     * rock (random count), or pebbles (random count).
     */
    private BlockState pickDecoration(RandomSource random) {
        int roll = random.nextInt(4);
        return switch (roll) {
            case 0 -> Blocks.BROWN_MUSHROOM.defaultBlockState();
            case 1 -> MainPlants.SHELF_FUNGUS.get().defaultBlockState()
                    .setValue(RandomRotationBlock.ROTATION, random.nextInt(4));
            case 2 -> MainPlants.ROCK.get().defaultBlockState()
                    .setValue(DecorationBlock.COUNT, 1 + random.nextInt(3));
            default -> MainPlants.PEBBLES.get().defaultBlockState()
                    .setValue(DecorationBlock.COUNT, 1 + random.nextInt(3));
        };
    }

    private static BlockPos findReplaceableSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos candidate = target.offset(0, dy, 0);
            BlockState state   = world.getBlockState(candidate);
            BlockState above   = world.getBlockState(candidate.above());
            if ((state.is(Blocks.GRASS_BLOCK)
                    || state.is(BlockTags.DIRT)
                    || state.is(Blocks.COARSE_DIRT)
                    || state.is(MainRocks.LOAM.get()))
                    && above.isAir()) {
                return candidate;
            }
        }
        return null;
    }
}