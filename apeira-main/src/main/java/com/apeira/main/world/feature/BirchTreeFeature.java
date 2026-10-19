package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainSpecialBlocks;

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
 * Faithful recreation of birch_tree_0–3.nbt in code.
 * Replaces vanilla birch tree placement in all birch biomes.
 *
 * Leaf colour (birch / yellow_birch) is chosen randomly per tree.
 *
 * Shape mapping:
 *   SMALL  (30%) — tree_0:  8-block trunk,  2 branch arms, asymmetric early clusters
 *   MEDIUM (35%) — tree_1: 16-block trunk,  7 branch arms, multi-cluster canopy
 *   TALL   (25%) — tree_2: 17-block trunk,  8 branch arms, tall asymmetric canopy
 *   GIANT  (10%) — tree_3: 24-block trunk, 10 branch arms, massive multi-tier canopy
 */
public record BirchTreeFeature() implements Feature {
    public static final MapCodec<BirchTreeFeature> CODEC = MapCodec.unit(BirchTreeFeature::new);


    private static final int MIN_SPACING = 5;

    // ── SMALL (tree_0) ────────────────────────────────────────────────────────
    // Trunk height 8, branches at relY +3 and +4 from base

    private static final int[][] SMALL_BRANCHES = {
            {1, 3, 0}, {0, 4, 1}
    };
    private static final int[][] SMALL_MINUS4 = {{-1,-1},{-1,0},{0,-1}};
    private static final int[][] SMALL_MINUS3 = {{1,-1},{1,0},{2,-1},{2,0}};
    private static final int[][] SMALL_MINUS2 = {{-1,1},{-1,2},{0,1},{0,2}};
    // relY -1 intentionally empty
    private static final int[][] SMALL_0      = {
            {-2,-1},{-2,0},{-2,1},{-1,-2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-2},{0,-1},{0,1},{0,2},{1,-2},{1,-1},{1,0},{1,1},{1,2},
            {2,-1},{2,0},{2,1}
    };
    private static final int[][] SMALL_PLUS1  = {
            {-1,-1},{-1,0},{-1,1},{0,-1},{0,0},{0,1},{1,-1},{1,0},{1,1}
    };

    // ── MEDIUM (tree_1) ───────────────────────────────────────────────────────
    // Trunk height 16, 7 branch arms

    private static final int[][] MEDIUM_BRANCHES = {
            {-1,6,0},{0,8,-1},{0,9,1},{-1,11,0},{1,11,0},{-2,12,1},{0,12,-1}
    };
    private static final int[][] MEDIUM_MINUS10 = {{0,-1},{1,-1},{1,0}};
    private static final int[][] MEDIUM_MINUS8  = {
            {-2,-1},{-2,0},{-2,1},{-2,2},{-1,-1},{-1,0},{-1,1},{-1,2},
            {0,-1},{0,1},{0,2},{1,-1},{1,0},{1,1}
    };
    private static final int[][] MEDIUM_MINUS6  = {
            {0,-3},{0,-2},{0,-1},{1,-3},{1,-2},{1,-1},{1,0},{2,-2},{2,-1}
    };
    private static final int[][] MEDIUM_MINUS5  = {
            {-1,1},{-1,2},{-1,3},{0,-2},{0,-1},{0,1},{0,2},{0,3},{1,-2},{1,-1},{1,1},{1,2}
    };
    private static final int[][] MEDIUM_MINUS4  = {{-1,-2},{-1,-1},{0,-2},{0,-1}};
    private static final int[][] MEDIUM_MINUS3  = {
            {-3,0},{-3,1},{-2,0},{-2,2},{-1,0},{-1,1},{-1,2},{1,0},{1,1},{2,0},{2,1}
    };
    private static final int[][] MEDIUM_MINUS2  = {
            {-2,0},{-2,1},{-1,0},{-1,1},{0,-2},{0,-1},{1,-2},{1,-1}
    };
    private static final int[][] MEDIUM_0       = SMALL_0;   // shared canopy shape
    private static final int[][] MEDIUM_PLUS1   = SMALL_PLUS1;

    // ── TALL (tree_2) ─────────────────────────────────────────────────────────
    // Trunk height 17, 8 branch arms

    private static final int[][] TALL_BRANCHES = {
            {-1,5,0},{1,6,0},{0,7,1},{0,9,-1},{1,9,-2},{1,11,0},{-1,13,1},{0,13,-1}
    };
    private static final int[][] TALL_MINUS10 = {{-2,0},{-2,1},{-1,0},{-1,1}};
    private static final int[][] TALL_MINUS9  = {{1,-1},{1,0},{2,-1},{2,0}};
    private static final int[][] TALL_MINUS8  = {
            {-2,1},{-2,2},{-1,0},{-1,1},{-1,2},{0,1},{0,2}
    };
    private static final int[][] TALL_MINUS7  = {
            {-1,0},{-1,1},{0,-3},{0,-2},{0,1},{1,-3},{1,-1},{2,-2},{2,-1}
    };
    private static final int[][] TALL_MINUS6  = {{0,-2},{0,-1},{1,-2},{1,-1}};
    private static final int[][] TALL_MINUS5  = {{-1,0},{-1,1},{0,1}};
    private static final int[][] TALL_MINUS4  = {{1,0},{1,1},{2,0},{2,1}};
    private static final int[][] TALL_MINUS3  = {
            {-2,0},{-2,1},{-2,2},{-1,-1},{-1,0},{-1,2},{0,-3},{0,-2},{0,1},{0,2},
            {1,-3},{1,-2},{1,-1},{1,0},{2,-2},{2,-1}
    };
    private static final int[][] TALL_MINUS2  = {{-2,1},{-2,2},{-1,1},{-1,2},{0,-2},{0,-1},{1,-2},{1,-1}};
    private static final int[][] TALL_0       = SMALL_0;
    private static final int[][] TALL_PLUS1   = SMALL_PLUS1;

    // ── GIANT (tree_3) ────────────────────────────────────────────────────────
    // Trunk height 24, 10 branch arms

    private static final int[][] GIANT_BRANCHES = {
            {1,3,0},{-1,5,0},{0,7,-1},{0,10,1},{1,10,-1},
            {-1,15,1},{0,16,-1},{1,16,-2},{-1,19,1},{1,20,-1}
    };
    private static final int[][] GIANT_MINUS15 = {{0,-2},{0,-1},{1,-2},{1,-1}};
    private static final int[][] GIANT_MINUS13 = {{-2,0},{-2,1},{-1,0},{-1,1}};
    private static final int[][] GIANT_MINUS12 = {{-1,1},{-1,2},{0,1},{0,2},{1,-2},{1,-1},{2,-2},{2,-1}};
    private static final int[][] GIANT_MINUS10 = {
            {-1,-2},{-1,-1},{0,-3},{0,-2},{0,-1},{0,1},{1,-3},{1,-2},{1,-1},{1,0},{2,-2},{2,-1}
    };
    private static final int[][] GIANT_MINUS9  = {{0,-2},{0,-1},{1,-2},{1,-1}};
    private static final int[][] GIANT_MINUS8  = {
            {-2,1},{-2,2},{-1,0},{-1,2},{-1,3},{0,1},{0,2},{0,3},{1,1},{1,2}
    };
    private static final int[][] GIANT_MINUS7  = {
            {-1,-2},{-1,-1},{-1,1},{-1,2},{0,-3},{0,-2},{0,1},{0,2},{1,-3},{1,-1},{1,0},{2,-2},{2,-1}
    };
    private static final int[][] GIANT_MINUS6  = {{0,-2},{0,-1},{1,-2},{1,-1}};
    private static final int[][] GIANT_MINUS4  = {
            {-3,1},{-3,2},{-2,0},{-2,1},{-2,2},{-2,3},{-1,0},{-1,2},{-1,3},{0,1},{0,2}
    };
    private static final int[][] GIANT_MINUS3  = {
            {-2,1},{-2,2},{-1,-2},{-1,-1},{-1,1},{-1,2},{0,-3},{0,-2},{0,-1},
            {1,-3},{1,-2},{1,0},{1,1},{2,-2},{2,-1},{2,0},{2,1},{3,0}
    };
    private static final int[][] GIANT_MINUS2  = {{0,-2},{0,-1},{1,-2},{1,-1},{1,0},{2,-1},{2,0}};
    private static final int[][] GIANT_0       = SMALL_0;
    private static final int[][] GIANT_PLUS1   = SMALL_PLUS1;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public MapCodec<BirchTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.GRASS_BLOCK)) return false;
        if (hasNearbyLog(world, origin, MIN_SPACING)) return false;

        Block leaves = random.nextBoolean()
                ? Blocks.BIRCH_LEAVES
                : MainSpecialBlocks.YELLOW_BIRCH_LEAVES.get();
        Block log = Blocks.BIRCH_LOG;
        int rot   = random.nextInt(4);

        int roll = random.nextInt(20);
        if      (roll < 6)  return generateSmall (world, origin, log, leaves, random, rot);
        else if (roll < 13) return generateMedium(world, origin, log, leaves, random, rot);
        else if (roll < 18) return generateTall  (world, origin, log, leaves, random, rot);
        else                return generateGiant (world, origin, log, leaves, random, rot);
    }

    // ── SMALL ─────────────────────────────────────────────────────────────────

    private boolean generateSmall(WorldGenLevel world, BlockPos origin,
                                  Block log, Block leaves, RandomSource random, int rot) {
        int height = 7 + random.nextInt(3); // 7-9 (NBT = 8)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        // Branches
        for (int[] b : SMALL_BRANCHES) {
            int[] r = rot(b[0], b[2], rot);
            placeLog(world, origin.offset(r[0], b[1], r[1]), log);
        }

        BlockPos top = origin.above(height);
        placeLayer(world, top.below(4), leaves, rot, SMALL_MINUS4);
        placeLayer(world, top.below(3), leaves, rot, SMALL_MINUS3);
        placeLayer(world, top.below(2), leaves, rot, SMALL_MINUS2);
        placeLayer(world, top,         leaves, rot, SMALL_0);
        placeLayer(world, top.above(1),   leaves, rot, SMALL_PLUS1);
        return true;
    }

    // ── MEDIUM ────────────────────────────────────────────────────────────────

    private boolean generateMedium(WorldGenLevel world, BlockPos origin,
                                   Block log, Block leaves, RandomSource random, int rot) {
        int height = 15 + random.nextInt(3); // 15-17 (NBT = 16)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        for (int[] b : MEDIUM_BRANCHES) {
            int[] r = rot(b[0], b[2], rot);
            placeLog(world, origin.offset(r[0], b[1], r[1]), log);
        }

        BlockPos top = origin.above(height);
        placeLayer(world, top.below(10), leaves, rot, MEDIUM_MINUS10);
        placeLayer(world, top.below(8),  leaves, rot, MEDIUM_MINUS8);
        placeLayer(world, top.below(6),  leaves, rot, MEDIUM_MINUS6);
        placeLayer(world, top.below(5),  leaves, rot, MEDIUM_MINUS5);
        placeLayer(world, top.below(4),  leaves, rot, MEDIUM_MINUS4);
        placeLayer(world, top.below(3),  leaves, rot, MEDIUM_MINUS3);
        placeLayer(world, top.below(2),  leaves, rot, MEDIUM_MINUS2);
        placeLayer(world, top,          leaves, rot, MEDIUM_0);
        placeLayer(world, top.above(1),    leaves, rot, MEDIUM_PLUS1);
        return true;
    }

    // ── TALL ──────────────────────────────────────────────────────────────────

    private boolean generateTall(WorldGenLevel world, BlockPos origin,
                                 Block log, Block leaves, RandomSource random, int rot) {
        int height = 16 + random.nextInt(3); // 16-18 (NBT = 17)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        for (int[] b : TALL_BRANCHES) {
            int[] r = rot(b[0], b[2], rot);
            placeLog(world, origin.offset(r[0], b[1], r[1]), log);
        }

        BlockPos top = origin.above(height);
        placeLayer(world, top.below(10), leaves, rot, TALL_MINUS10);
        placeLayer(world, top.below(9),  leaves, rot, TALL_MINUS9);
        placeLayer(world, top.below(8),  leaves, rot, TALL_MINUS8);
        placeLayer(world, top.below(7),  leaves, rot, TALL_MINUS7);
        placeLayer(world, top.below(6),  leaves, rot, TALL_MINUS6);
        placeLayer(world, top.below(5),  leaves, rot, TALL_MINUS5);
        placeLayer(world, top.below(4),  leaves, rot, TALL_MINUS4);
        placeLayer(world, top.below(3),  leaves, rot, TALL_MINUS3);
        placeLayer(world, top.below(2),  leaves, rot, TALL_MINUS2);
        placeLayer(world, top,          leaves, rot, TALL_0);
        placeLayer(world, top.above(1),    leaves, rot, TALL_PLUS1);
        return true;
    }

    // ── GIANT ─────────────────────────────────────────────────────────────────

    private boolean generateGiant(WorldGenLevel world, BlockPos origin,
                                  Block log, Block leaves, RandomSource random, int rot) {
        int height = 23 + random.nextInt(3); // 23-25 (NBT = 24)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        for (int[] b : GIANT_BRANCHES) {
            int[] r = rot(b[0], b[2], rot);
            placeLog(world, origin.offset(r[0], b[1], r[1]), log);
        }

        BlockPos top = origin.above(height);
        placeLayer(world, top.below(15), leaves, rot, GIANT_MINUS15);
        placeLayer(world, top.below(13), leaves, rot, GIANT_MINUS13);
        placeLayer(world, top.below(12), leaves, rot, GIANT_MINUS12);
        placeLayer(world, top.below(10), leaves, rot, GIANT_MINUS10);
        placeLayer(world, top.below(9),  leaves, rot, GIANT_MINUS9);
        placeLayer(world, top.below(8),  leaves, rot, GIANT_MINUS8);
        placeLayer(world, top.below(7),  leaves, rot, GIANT_MINUS7);
        placeLayer(world, top.below(6),  leaves, rot, GIANT_MINUS6);
        placeLayer(world, top.below(4),  leaves, rot, GIANT_MINUS4);
        placeLayer(world, top.below(3),  leaves, rot, GIANT_MINUS3);
        placeLayer(world, top.below(2),  leaves, rot, GIANT_MINUS2);
        placeLayer(world, top,          leaves, rot, GIANT_0);
        placeLayer(world, top.above(1),    leaves, rot, GIANT_PLUS1);
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private static int[] rot(int dx, int dz, int rotation) {
        return switch (rotation) {
            case 1 ->  new int[]{ dz, -dx};
            case 2 ->  new int[]{-dx, -dz};
            case 3 ->  new int[]{-dz,  dx};
            default -> new int[]{ dx,  dz};
        };
    }

    private static boolean hasNearbyLog(WorldGenLevel world, BlockPos origin, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int dy = -1; dy <= 26; dy++) {
                    if (world.getBlockState(origin.offset(dx, dy, dz)).is(BlockTags.LOGS)) return true;
                }
            }
        }
        return false;
    }
}