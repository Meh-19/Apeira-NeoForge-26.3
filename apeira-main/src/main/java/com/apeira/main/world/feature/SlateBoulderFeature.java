package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places one of five hardcoded slate boulder shapes, extracted directly from
 * slate_0–slate_4.nbt. Only spawns on underwater floors.
 *
 * Shape summary:
 *   V0 — slate_2: tiny flat 2×2 slab (1 layer)
 *   V1 — slate_0: small dome (3 layers, tapered)
 *   V2 — slate_4: low flat boulder (2 layers)
 *   V3 — slate_3: medium dome with moss cap (3 layers)
 *   V4 — slate_1: large boulder (4 layers)
 *
 * Post-placement:
 *   - Any slate block whose top face is exposed to air (above waterline) has
 *     a 35% chance to receive a damp_moss block on top.
 *   - Cattails are scattered on ground adjacent to the boulder that is
 *     directly beside shallow (1-block-deep) water.
 *   - Clovers are scattered on ground at the very water's edge around the boulder.
 *
 * Placement: use OCEAN_FLOOR_WG heightmap so origin lands on the underwater floor.
 */
public record SlateBoulderFeature() implements Feature {
    public static final MapCodec<SlateBoulderFeature> CODEC = MapCodec.unit(SlateBoulderFeature::new);

    // ── Shape data (dx, dy, dz) offsets from origin ───────────────────────────
    // All positions are relative to the centre base of each structure.

    /** V0 — slate_2: tiny flat 2×2 slab */
    private static final int[][] V0 = {
            {0,0,0},{0,0,1},{1,0,0},{1,0,1}
    };

    /** V1 — slate_0: small dome */
    private static final int[][] V1 = {
            // y=0
            {-1,-1},{-1,0},{-1,1},{0,-1},{0,0},{0,1},{1,-1},{1,0},{1,1},
            // y=1
            {-1,0},{-1,1},{0,-1},{0,0},{0,1},{1,-1},{1,0},
            // y=2
            {-1,0},{-1,1},{0,0},{0,1}
    };
    // V1 needs Y encoded — using full {dx,dy,dz}
    private static final int[][] V1_FULL = {
            {-1,0,-1},{-1,0,0},{-1,0,1},{0,0,-1},{0,0,0},{0,0,1},{1,0,-1},{1,0,0},{1,0,1},
            {-1,1,0},{-1,1,1},{0,1,-1},{0,1,0},{0,1,1},{1,1,-1},{1,1,0},
            {-1,2,0},{-1,2,1},{0,2,0},{0,2,1}
    };

    /** V2 — slate_4: low flat boulder */
    private static final int[][] V2_FULL = {
            {-2,0,-1},{-1,0,-1},{-1,0,0},{0,0,-1},{0,0,0},{0,0,1},{1,0,0},{1,0,1},
            {-1,1,-1},{-1,1,0},{0,1,-1},{0,1,0}
    };

    /** V3 — slate_3: medium dome */
    private static final int[][] V3_FULL = {
            {-2,0,0},{-2,0,1},{-2,0,2},{-1,0,-1},{-1,0,0},{-1,0,1},{-1,0,2},
            {0,0,-1},{0,0,0},{0,0,1},{1,0,-1},{1,0,0},{1,0,1},
            {-1,1,0},{-1,1,1},{0,1,-1},{0,1,0},{0,1,1},{1,1,-1},{1,1,0},
            {-1,2,0},{-1,2,1},{0,2,0},{0,2,1}
    };

    /** V4 — slate_1: large boulder */
    private static final int[][] V4_FULL = {
            // y=0
            {-2,0,-2},{-2,0,-1},{-2,0,0},{-2,0,2},{-2,0,3},
            {-1,0,-2},{-1,0,-1},{-1,0,0},{-1,0,1},{-1,0,2},{-1,0,3},
            {0,0,-2},{0,0,-1},{0,0,0},{0,0,1},{0,0,2},
            {1,0,-2},{1,0,-1},{1,0,0},{1,0,1},
            // y=1
            {-2,1,-2},{-2,1,-1},
            {-1,1,-2},{-1,1,-1},{-1,1,0},{-1,1,1},{-1,1,2},
            {0,1,-2},{0,1,-1},{0,1,0},{0,1,1},{0,1,2},
            {1,1,-1},{1,1,0},{1,1,1},
            // y=2
            {-1,2,-2},{-1,2,-1},{-1,2,0},
            {0,2,-2},{0,2,-1},{0,2,0},{0,2,1},
            {1,2,0},{1,2,1},
            // y=3
            {-1,3,-1},{-1,3,0},{0,3,-1},{0,3,0}
    };

    private static final int[][][] VARIANTS = { V1_FULL, V2_FULL, V3_FULL, V4_FULL,
            // V0 inline
            {{0,0,0},{0,0,1},{1,0,0},{1,0,1}} };

    @Override
    public MapCodec<SlateBoulderFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // Origin must be a solid block with water somewhere above it
        BlockState originState = world.getBlockState(origin);
        if (originState.isAir()) return false;
        // Scan up to 3 blocks above origin for water — guards against heightmap off-by-one
        // and custom floor blocks placed by other features
        boolean hasWaterAbove = false;
        for (int scanY = 1; scanY <= 3; scanY++) {
            if (world.getFluidState(origin.above(scanY)).is(FluidTags.WATER)) {
                hasWaterAbove = true;
                break;
            }
        }
        if (!hasWaterAbove) return false;

        int rot = random.nextInt(4);
        int vi  = random.nextInt(VARIANTS.length);
        int[][] shape = VARIANTS[vi];

        // ── Place slate ───────────────────────────────────────────────────────
        boolean anyPlaced = false;
        for (int[] o : shape) {
            int dx = o[0], dy = o[1], dz = o.length > 2 ? o[2] : 0;
            int[] r = rot(dx, dz, rot);
            BlockPos pos = origin.offset(r[0], dy, r[1]);

            BlockState current = world.getBlockState(pos);
            // y=0 replaces the floor block; above y=0 replaces air or water only
            boolean canPlace = dy == 0
                    ? !current.isAir()
                    : (current.isAir() || world.getFluidState(pos).is(FluidTags.WATER));
            if (canPlace) {
                world.setBlock(pos, MainStones.SLATE.get().defaultBlockState(), Block.UPDATE_ALL);
                anyPlaced = true;
            }
        }

        if (!anyPlaced) return false;

        // ── Moss on above-water slate tops ────────────────────────────────────
        for (int[] o : shape) {
            int dx = o[0], dy = o[1], dz = o.length > 2 ? o[2] : 0;
            int[] r = rot(dx, dz, rot);
            BlockPos slatePos = origin.offset(r[0], dy, r[1]);

            if (!world.getBlockState(slatePos).is(MainStones.SLATE.get())) continue;

            BlockPos above = slatePos.above();
            BlockState aboveState = world.getBlockState(above);
            // Only add moss if above the waterline (top face is exposed to air)
            // Air above = above waterline; fluid above = submerged, skip moss
            boolean isAboveWater = aboveState.isAir() || !world.getFluidState(above).is(FluidTags.WATER);
            if (isAboveWater && aboveState.isAir() && random.nextFloat() < 0.35f) {
                world.setBlock(above, MainPlants.DAMP_MOSS.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        // ── Vegetation around the boulder ─────────────────────────────────────
        int vegRadius = 4;
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = random.nextInt(vegRadius * 2 + 1) - vegRadius;
            int dz = random.nextInt(vegRadius * 2 + 1) - vegRadius;
            BlockPos candidate = findGroundSurface(world, origin.offset(dx, 0, dz));
            if (candidate == null) continue;

            BlockState below = world.getBlockState(candidate.below());
            if (below.isAir() || below.is(Blocks.WATER)) continue;

            boolean adjWater = isAdjacentToWater(world, candidate);
            boolean adjShallowWater = isAdjacentToShallowWater(world, candidate);

            int roll = random.nextInt(10);
            if (adjShallowWater && roll < 4) {
                // Cattail on shallow water edge
                if (world.getBlockState(candidate).isAir()
                        && world.getBlockState(candidate.above()).isAir()) {
                    DoublePlantBlock.placeAt(world, MainPlants.CATTAIL.get().defaultBlockState(), candidate, Block.UPDATE_CLIENTS);
                }
            } else if (adjWater && roll < 7) {
                // Clover at water's edge
                if (world.getBlockState(candidate).isAir()) {
                    world.setBlock(candidate, MainPlants.CLOVER.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static BlockPos findGroundSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos c = target.offset(0, dy, 0);
            BlockState below = world.getBlockState(c.below());
            BlockState at    = world.getBlockState(c);
            if (!below.isAir() && !below.is(Blocks.WATER) && at.isAir()) return c;
        }
        return null;
    }

    private static boolean isAdjacentToWater(WorldGenLevel world, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(pos.relative(dir)).is(Blocks.WATER)) return true;
            if (world.getBlockState(pos.relative(dir).below()).is(Blocks.WATER)) return true;
        }
        return false;
    }

    /** Shallow water = water block with solid floor directly below it. */
    private static boolean isAdjacentToShallowWater(WorldGenLevel world, BlockPos pos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = pos.relative(dir);
            if (world.getFluidState(neighbor).is(FluidTags.WATER)) {
                BlockState floor = world.getBlockState(neighbor.below());
                if (!floor.isAir() && !world.getFluidState(neighbor.below()).is(FluidTags.WATER)) return true;
            }
        }
        return false;
    }

    private static int[] rot(int dx, int dz, int rotation) {
        return switch (rotation) {
            case 1 ->  new int[]{ dz, -dx};
            case 2 ->  new int[]{-dx, -dz};
            case 3 ->  new int[]{-dz,  dx};
            default -> new int[]{ dx,  dz};
        };
    }
}