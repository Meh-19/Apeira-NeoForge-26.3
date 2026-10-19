package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainWoods;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Faithful recreation of the original maple NBT structures (0-4) in code.
 *
 * All branch log positions and leaf layer patterns are hardcoded directly
 * from the NBT data, then rotated 0/90/180/270° each spawn for variety.
 * Leaf colour (orange / red) is chosen independently.
 *
 * Shape mapping:
 *   NORMAL (75%) — trees 1, 2, 3:  straight trunk, small early-leaf branch stubs,
 *                                   7-layer rounded canopy.
 *   LARGE  (15%) — tree 0:         short trunk, 25 branch logs arching out in two
 *                                   directions, huge asymmetric canopy.
 *   TALL   (10%) — tree 4:         19-block trunk, 3 single-block arms at
 *                                   relY -12/-8/-5, small cluster per arm, wide top cap.
 */
public record MapleTreeFeature() implements Feature {
    public static final MapCodec<MapleTreeFeature> CODEC = MapCodec.unit(MapleTreeFeature::new);


    private static final int MIN_SPACING = 6;

    // ── NORMAL canopy layers (offsets from trunk top, tree_1 pattern) ─────────
    // Each int[][] is {dx, dz} offsets placed at that Y relative to trunk top.

    /** Sparse early stub — placed near upper trunk to simulate small branch tips. */
    private static final int[][][] NORMAL_STUBS = {
            // tree_1 style:  relY -6
            {{0,1},{1,0},{1,1}},
            // tree_2 style:  relY -7 and -5 (two separate calls)
            {{-1,0},{-1,1},{0,1}},
            {{0,-1},{1,-1},{1,0}},
            // tree_3 style:  relY -9 and -7
            {{0,1},{1,0},{1,1}},
            {{-1,-1},{-1,0},{0,-1}},
    };

    private static final int[][] NORMAL_Y4 = {
            {-2,-1},{-2,1},{-1,-2},{-1,-1},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{0,2},{1,-1},{1,0},{1,1},{2,-1},{2,0},{2,1}
    };
    private static final int[][] NORMAL_Y3 = {
            {-2,-2},{-2,-1},{-2,0},{-2,1},{-2,2},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{0,2},{1,-2},{1,-1},{1,0},{1,1},{1,2},
            {2,-2},{2,-1},{2,0},{2,1},{2,2}
    };
    private static final int[][] NORMAL_Y2 = {
            {-2,-1},{-2,0},{-2,1},{-2,2},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{0,2},{1,-2},{1,-1},{1,0},{1,1},{1,2},
            {2,-2},{2,-1},{2,0},{2,1},{2,2}
    };
    private static final int[][] NORMAL_Y1 = {
            {-2,0},{-2,1},{-1,-1},{-1,0},{-1,1},{-1,2},{0,-2},{0,-1},{0,1},
            {1,-2},{1,-1},{1,0},{1,1},{1,2},{2,-1},{2,1}
    };
    private static final int[][] NORMAL_Y0 = {
            {-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}
    };
    private static final int[][] NORMAL_Y_PLUS1 = {
            {-1,0},{0,-1},{0,0},{0,1},{1,0}
    };

    // ── LARGE branch log offsets (dx, dz) at each relY from trunk top ─────────
    // Extracted directly from orange_maple_0.nbt.
    // Two "arms" spread in different diagonal directions from the trunk top.

    private static final int[][] LARGE_BRANCHES_RELMINUS2 = {{0,-1}};
    private static final int[][] LARGE_BRANCHES_REL0      = {{-1,-1},{1,-1}};
    private static final int[][] LARGE_BRANCHES_REL1      = {{-2,-1},{-1,-1},{0,1},{1,-1},{2,-2}};
    private static final int[][] LARGE_BRANCHES_REL2      = {{-3,-1},{-2,-1},{-1,2},{0,2},{2,-2},{3,-3}};
    private static final int[][] LARGE_BRANCHES_REL3      = {{-3,-1},{-2,2},{0,2},{1,-2},{2,-2},{3,-2}};
    private static final int[][] LARGE_BRANCHES_REL4      = {{-4,-1},{-3,-1},{0,3},{3,-2}};
    private static final int[][] LARGE_BRANCHES_REL5      = {{0,3}};

    // ── LARGE leaf layers (offsets from trunk top) ────────────────────────────

    private static final int[][] LARGE_L_MINUS3 = {{0,1},{1,0},{1,1}};
    private static final int[][] LARGE_L_MINUS1 = {{-1,-2},{-1,-1},{0,-2},{0,-1}};
    private static final int[][] LARGE_L_0      = {{0,1},{1,0},{1,1}};
    private static final int[][] LARGE_L_1      = {{2,-4},{3,-2},{4,-4}};
    private static final int[][] LARGE_L_2 = {
            {-2,-2},{-1,-3},{-1,-2},{-1,-1},{-1,0},{-1,1},
            {0,-2},{0,-1},{0,0},{0,1},{1,-4},{1,-3},{1,-2},{1,0},{1,1},
            {2,-5},{2,-4},{2,-3},{2,-1},{2,0},{3,-5},{3,-4},{3,-2},{3,-1},
            {4,-5},{4,-4},{4,-3},{4,-2},{4,-1},{5,-4},{5,-2}
    };
    private static final int[][] LARGE_L_3 = {
            {-5,-3},{-5,-1},{-4,1},{-3,-2},{-2,-3},{-2,-2},{-2,-1},{-2,3},
            {-1,-3},{-1,-2},{-1,-1},{-1,0},{-1,1},{0,-3},{0,-2},{0,-1},{0,0},{0,1},
            {1,-5},{1,-4},{1,-3},{1,-1},{1,0},{1,1},{2,-5},{2,-4},{2,-3},{2,-1},{2,0},{2,1},
            {3,-5},{3,-4},{3,-3},{3,-1},{4,-5},{4,-4},{4,-3},{4,-2},
            {5,-5},{5,-4},{5,-3},{5,-2},{5,-1}
    };
    private static final int[][] LARGE_L_4 = {
            {-6,-2},{-6,-1},{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,0},{-5,1},
            {-4,-3},{-4,-2},{-4,0},{-4,1},{-3,-3},{-3,-2},{-3,0},{-3,2},{-3,3},
            {-2,-2},{-2,-1},{-2,0},{-2,2},{-2,3},{-1,-3},{-1,-2},{-1,-1},{-1,2},{-1,4},
            {0,-3},{0,-2},{0,-1},{0,0},{1,-5},{1,-4},{1,-3},{1,-2},{1,-1},{1,0},{1,1},{1,2},
            {2,-5},{2,-4},{2,-3},{2,-2},{2,-1},{2,0},{2,4},{3,-5},{3,-4},{3,-3},{3,-1},
            {4,-5},{4,-4},{4,-3},{4,-2},{4,-1},{5,-5},{5,-4},{5,-3},{5,-2}
    };
    private static final int[][] LARGE_L_5 = {
            {-6,-3},{-6,-2},{-6,-1},{-6,0},{-6,1},{-5,-3},{-5,-2},{-5,-1},{-5,0},{-5,1},
            {-4,-3},{-4,-2},{-4,-1},{-4,0},{-4,1},{-3,-3},{-3,-2},{-3,-1},{-3,0},{-3,1},
            {-2,-3},{-2,-2},{-2,-1},{-2,0},{-2,1},{-2,2},{-2,4},
            {-1,1},{-1,2},{-1,3},{-1,4},{-1,5},{0,-2},{0,-1},{0,1},{0,2},{0,4},{0,5},
            {1,-2},{1,-1},{1,1},{1,2},{1,3},{1,4},{1,5},{2,-4},{2,-3},{2,2},{2,3},{2,4},
            {3,-4},{3,-3},{3,-2},{4,-4},{4,-3},{4,-2}
    };
    private static final int[][] LARGE_L_6 = {
            {-6,-2},{-6,0},{-5,-3},{-5,-2},{-5,-1},{-5,0},
            {-4,-2},{-4,-1},{-4,0},{-4,1},{-3,-3},{-3,-2},{-3,-1},{-3,0},{-3,1},
            {-2,-2},{-2,-1},{-2,0},{-2,1},{-2,2},{-2,3},{-2,4},{-2,5},
            {-1,1},{-1,2},{-1,3},{-1,4},{-1,5},{0,1},{0,2},{0,3},{0,4},{0,5},
            {1,1},{1,2},{1,3},{1,4},{1,5},{2,1},{2,2},{2,3},{2,4},{2,5}
    };
    private static final int[][] LARGE_L_7 = {
            {-5,-2},{-5,-1},{-4,-2},{-4,-1},{-4,0},{-3,-1},{-3,0},
            {-2,2},{-2,3},{-2,4},{-1,1},{-1,2},{-1,3},{-1,4},{-1,5},
            {0,1},{0,2},{0,3},{0,4},{0,5},{1,1},{1,2},{1,3},{1,4},{1,5},{2,2},{2,3},{2,4}
    };
    private static final int[][] LARGE_L_8 = {{-1,2},{-1,3},{0,2},{0,3},{0,4},{1,3},{1,4}};

    // ── TALL leaf layers (offsets from trunk top) ─────────────────────────────
    // Branch arms: relY -12 at (-1,0),  relY -8 at (+1,0),  relY -5 at (0,+1)

    private static final int[][] TALL_ARM1_LEAVES_Y0 = {
            {-2,0},{-2,1},{-1,-1},{-1,1},{-1,2},{0,-1},{0,1},{0,2},{1,0},{1,1}
    };
    private static final int[][] TALL_ARM1_LEAVES_Y1 = {{-1,0},{-1,1},{0,1}};
    private static final int[][] TALL_ARM2_LEAVES_Y0 = {
            {-1,-1},{-1,0},{0,-2},{0,-1},{0,1},{1,-2},{1,-1},{1,1},{2,-1},{2,0}
    };
    private static final int[][] TALL_ARM2_LEAVES_Y1 = {{0,-1},{1,-1},{1,0}};
    private static final int[][] TALL_ARM3_LEAVES_Y0 = {{-1,0},{0,-1},{1,1}};

    // Top cap layers (relative to trunk top)
    private static final int[][] TALL_TOP_MINUS3 = {
            {-2,-1},{-2,1},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-1},{0,1},{0,2},{1,-2},{1,-1},{1,0},{1,1},{1,2},{2,-1},{2,0}
    };
    private static final int[][] TALL_TOP_MINUS2 = {
            {-2,-2},{-2,-1},{-2,0},{-2,1},{-2,2},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{0,2},{1,-2},{1,-1},{1,0},{1,1},{1,2},
            {2,-2},{2,-1},{2,0},{2,1},{2,2}
    };
    private static final int[][] TALL_TOP_MINUS1 = TALL_TOP_MINUS2; // same shape
    private static final int[][] TALL_TOP_0 = {
            {-2,-1},{-2,0},{-2,1},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{1,-2},{1,-1},{1,0},{1,1},{1,2},{2,-1},{2,0},{2,1}
    };
    private static final int[][] TALL_TOP_PLUS1 = {
            {-1,-1},{-1,0},{-1,1},{0,-1},{0,0},{0,1},{1,-1},{1,0},{1,1}
    };
    private static final int[][] TALL_TOP_PLUS2 = {{-1,0},{0,-1},{0,0},{0,1},{1,0}};

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public MapCodec<MapleTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.MOSS_BLOCK)) return false;
        if (hasNearbyLog(world, origin, MIN_SPACING)) return false;

        Block leaves = random.nextBoolean() ? MainWoods.MAPLE.leaves().get(0).get() : MainWoods.MAPLE.leaves().get(1).get();
        Block log    = MainWoods.MAPLE.log().get();
        int   rot    = random.nextInt(4);

        int roll = random.nextInt(20);
        if (roll < 15)      return generateNormal(world, origin, log, leaves, random, rot);
        else if (roll < 18) return generateLarge (world, origin, log, leaves, random, rot);
        else                return generateTall  (world, origin, log, leaves, random, rot);
    }

    // ── NORMAL ─────────────────────────────────────────────────────────────────

    private boolean generateNormal(WorldGenLevel world, BlockPos origin,
                                   Block log, Block leaves, RandomSource random, int rot) {
        int height = 9 + random.nextInt(5); // 9-13, matches trees 1-3
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        // Small early-leaf stubs near the upper trunk.
        // Pick one of three variants matching the different NBT structures.
        int stubVariant = random.nextInt(3);
        if (stubVariant == 0) {
            // tree_1: single 3-leaf cluster at relY -6
            placeLayer(world, top.below(6), leaves, rot, NORMAL_STUBS[0]);
        } else if (stubVariant == 1) {
            // tree_2: two 3-leaf clusters at relY -7 and -5
            placeLayer(world, top.below(7), leaves, rot, NORMAL_STUBS[1]);
            placeLayer(world, top.below(5), leaves, rot, NORMAL_STUBS[2]);
        } else {
            // tree_3: two 3-leaf clusters at relY -9 and -7
            placeLayer(world, top.below(9), leaves, rot, NORMAL_STUBS[3]);
            placeLayer(world, top.below(7), leaves, rot, NORMAL_STUBS[4]);
        }

        // Main 7-layer canopy
        placeLayer(world, top.below(4), leaves, rot, NORMAL_Y4);
        placeLayer(world, top.below(3), leaves, rot, NORMAL_Y3);
        placeLayer(world, top.below(2), leaves, rot, NORMAL_Y2);
        placeLayer(world, top.below(1), leaves, rot, NORMAL_Y1);
        placeLayer(world, top,         leaves, rot, NORMAL_Y0);
        placeLayer(world, top.above(1),   leaves, rot, NORMAL_Y_PLUS1);

        return true;
    }

    // ── LARGE ──────────────────────────────────────────────────────────────────

    private boolean generateLarge(WorldGenLevel world, BlockPos origin,
                                  Block log, Block leaves, RandomSource random, int rot) {
        int trunkHeight = 4 + random.nextInt(3); // 4-6 (NBT uses 6, allow variation)
        for (int dy = 0; dy < trunkHeight; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(trunkHeight);

        // Branch logs — exact offsets from NBT, rotated
        placeLogs(world, top.below(2), log, rot, LARGE_BRANCHES_RELMINUS2);
        placeLogs(world, top,         log, rot, LARGE_BRANCHES_REL0);
        placeLogs(world, top.above(1),   log, rot, LARGE_BRANCHES_REL1);
        placeLogs(world, top.above(2),   log, rot, LARGE_BRANCHES_REL2);
        placeLogs(world, top.above(3),   log, rot, LARGE_BRANCHES_REL3);
        placeLogs(world, top.above(4),   log, rot, LARGE_BRANCHES_REL4);
        placeLogs(world, top.above(5),   log, rot, LARGE_BRANCHES_REL5);

        // Leaf layers — exact offsets from NBT, rotated
        placeLayer(world, top.below(3), leaves, rot, LARGE_L_MINUS3);
        placeLayer(world, top.below(1), leaves, rot, LARGE_L_MINUS1);
        placeLayer(world, top,         leaves, rot, LARGE_L_0);
        placeLayer(world, top.above(1),   leaves, rot, LARGE_L_1);
        placeLayer(world, top.above(2),   leaves, rot, LARGE_L_2);
        placeLayer(world, top.above(3),   leaves, rot, LARGE_L_3);
        placeLayer(world, top.above(4),   leaves, rot, LARGE_L_4);
        placeLayer(world, top.above(5),   leaves, rot, LARGE_L_5);
        placeLayer(world, top.above(6),   leaves, rot, LARGE_L_6);
        placeLayer(world, top.above(7),   leaves, rot, LARGE_L_7);
        placeLayer(world, top.above(8),   leaves, rot, LARGE_L_8);

        return true;
    }

    // ── TALL ───────────────────────────────────────────────────────────────────

    private boolean generateTall(WorldGenLevel world, BlockPos origin,
                                 Block log, Block leaves, RandomSource random, int rot) {
        int height = 15 + random.nextInt(5); // 15-19 (NBT uses 19)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        // ── Arm 1: relY -12, offset (-1, 0) ──────────────────────────────────
        BlockPos arm1 = top.below(12).offset(rot(-1, 0, rot)[0], 0, rot(-1, 0, rot)[1]);
        placeLog(world, arm1, log);
        placeLayer(world, arm1,         leaves, rot, TALL_ARM1_LEAVES_Y0);
        placeLayer(world, arm1.above(1),   leaves, rot, TALL_ARM1_LEAVES_Y1);

        // ── Arm 2: relY -8, offset (+1, 0) ───────────────────────────────────
        BlockPos arm2 = top.below(8).offset(rot(1, 0, rot)[0], 0, rot(1, 0, rot)[1]);
        placeLog(world, arm2, log);
        placeLayer(world, arm2,         leaves, rot, TALL_ARM2_LEAVES_Y0);
        placeLayer(world, arm2.above(1),   leaves, rot, TALL_ARM2_LEAVES_Y1);

        // ── Arm 3: relY -5, offset (0, +1) ───────────────────────────────────
        BlockPos arm3 = top.below(5).offset(rot(0, 1, rot)[0], 0, rot(0, 1, rot)[1]);
        placeLog(world, arm3, log);
        placeLayer(world, arm3,         leaves, rot, TALL_ARM3_LEAVES_Y0);

        // ── Top cap ───────────────────────────────────────────────────────────
        placeLayer(world, top.below(3), leaves, rot, TALL_TOP_MINUS3);
        placeLayer(world, top.below(2), leaves, rot, TALL_TOP_MINUS2);
        placeLayer(world, top.below(1), leaves, rot, TALL_TOP_MINUS1);
        placeLayer(world, top,         leaves, rot, TALL_TOP_0);
        placeLayer(world, top.above(1),   leaves, rot, TALL_TOP_PLUS1);
        placeLayer(world, top.above(2),   leaves, rot, TALL_TOP_PLUS2);

        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Place a set of log blocks from an offset array, with rotation. */
    private static void placeLogs(WorldGenLevel world, BlockPos base,
                                  Block log, int rot, int[][] offsets) {
        for (int[] o : offsets) {
            int[] r = rot(o[0], o[1], rot);
            placeLog(world, base.offset(r[0], 0, r[1]), log);
        }
    }

    /** Place a set of leaf blocks from an offset array, with rotation. */
    private static void placeLayer(WorldGenLevel world, BlockPos base,
                                   Block leaves, int rot, int[][] offsets) {
        for (int[] o : offsets) {
            int[] r = rot(o[0], o[1], rot);
            placeLeaf(world, base.offset(r[0], 0, r[1]), leaves);
        }
    }

    private static void placeLog(WorldGenLevel world, BlockPos pos, Block log) {
        BlockState s = world.getBlockState(pos);
        if (s.isAir() || s.is(Blocks.WATER)) {
            world.setBlock(pos, log.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void placeLeaf(WorldGenLevel world, BlockPos pos, Block leaves) {
        if (world.getBlockState(pos).isAir()) {
            world.setBlock(pos,
                    leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                    Block.UPDATE_ALL);
        }
    }

    /** Rotates (dx, dz) by 90° increments: 0=none, 1=90°, 2=180°, 3=270°. */
    private static int[] rot(int dx, int dz, int rotation) {
        return switch (rotation) {
            case 1 ->  new int[]{ dz, -dx};
            case 2 ->  new int[]{-dx, -dz};
            case 3 ->  new int[]{-dz,  dx};
            default -> new int[]{ dx,  dz};
        };
    }

    private static boolean hasNearbyLog(WorldGenLevel world, BlockPos origin, int radius) {
        Block log = MainWoods.MAPLE.log().get();
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