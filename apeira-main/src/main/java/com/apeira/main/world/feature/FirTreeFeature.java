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
 * Faithful recreation of the fir NBT structures (green/orange, 0–4) in code.
 *
 * All leaf layer patterns are hardcoded directly from the NBT data, then
 * rotated 0/90/180/270° each spawn for variety.
 * Leaf colour (green / orange) is chosen independently of shape.
 *
 * Shape mapping (probability):
 *   TINY   (20%) — fir_0/fir_1:  6-block trunk, minimal spire + tip.
 *   SMALL  (25%) — fir_3:        8-block trunk, tiered conical canopy.
 *   MEDIUM (30%) — fir_0/fir_1:  11-block trunk, layered mid-size canopy.
 *   TALL   (15%) — fir_4:        15-block trunk, 3-tier canopy with stub tips.
 *   GIANT  (10%) — fir_2:        18-block trunk, full multi-tier giant canopy.
 */
public record FirTreeFeature() implements Feature {
    public static final MapCodec<FirTreeFeature> CODEC = MapCodec.unit(FirTreeFeature::new);


    private static final int MIN_SPACING = 5;

    // ── TINY leaf layers (relY from trunk top, trunk height ~6) ───────────────
    // Source: green_fir_1 / orange_fir_0

    private static final int[][] TINY_MINUS3  = {{1, 0}};
    private static final int[][] TINY_MINUS1  = {{0, -1}, {0, 1}};
    private static final int[][] TINY_0       = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] TINY_PLUS1   = {{0, 0}};
    private static final int[][] TINY_PLUS2   = {{0, 0}};

    // ── SMALL leaf layers (relY from trunk top, trunk height ~8) ──────────────
    // Source: green_fir_3 / orange_fir_3

    private static final int[][] SMALL_MINUS5 = {
            {-2, 0}, {0, -2}, {0, 2}, {2, 0}
    };
    private static final int[][] SMALL_MINUS4 = {
            {-2, 0}, {-1, 0}, {0, -2}, {0, -1}, {0, 1}, {0, 2}, {1, 0}, {2, 0}
    };
    private static final int[][] SMALL_MINUS3 = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    // relY -2 is intentionally empty — creates a visible tier break
    private static final int[][] SMALL_MINUS1 = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] SMALL_0      = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] SMALL_PLUS1  = {{0, 0}};
    private static final int[][] SMALL_PLUS2  = {{0, 0}};

    // ── MEDIUM leaf layers (relY from trunk top, trunk height ~11) ────────────
    // Source: green_fir_0 / orange_fir_1

    private static final int[][] MED_MINUS8  = {{1, 0}};
    private static final int[][] MED_MINUS6  = {{-2, 0}, {0, -2}, {0, 2}, {2, 0}};
    private static final int[][] MED_MINUS5  = {
            {-2, 0}, {-1, 0}, {-1, 1}, {0, -2}, {0, -1}, {0, 1}, {0, 2},
            {1, 0}, {1, 1}, {2, 0}
    };
    private static final int[][] MED_MINUS4  = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] MED_MINUS3  = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] MED_MINUS2  = {{-1, -1}, {1, 1}};
    private static final int[][] MED_MINUS1  = {
            {-1, -1}, {-1, 0}, {0, -1}, {0, 1}, {1, 0}, {1, 1}
    };
    private static final int[][] MED_0       = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] MED_PLUS1   = {{0, 0}};
    private static final int[][] MED_PLUS2   = {{0, 0}};

    // ── TALL leaf layers (relY from trunk top, trunk height ~15) ──────────────
    // Source: green_fir_4 / orange_fir_4

    private static final int[][] TALL_MINUS12 = {{1, 0}};
    private static final int[][] TALL_MINUS11 = {{0, 1}};
    private static final int[][] TALL_MINUS10 = {{0, -1}};
    private static final int[][] TALL_MINUS9  = {
            {-2, -2}, {-2, 1}, {-2, 2}, {-1, -2}, {1, 2}, {2, -2}, {2, -1}, {2, 2}
    };
    private static final int[][] TALL_MINUS8  = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -1}, {0, 1}, {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] TALL_MINUS7  = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] TALL_MINUS6  = {{-2, 0}, {0, -2}, {0, 2}, {2, 0}};
    private static final int[][] TALL_MINUS5  = {
            {-2, 0}, {-1, 0}, {0, -2}, {0, -1}, {0, 1}, {0, 2},
            {1, -1}, {1, 0}, {1, 1}, {2, 0}
    };
    private static final int[][] TALL_MINUS4  = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] TALL_MINUS3  = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] TALL_MINUS2  = {{-1, -1}, {1, 1}};
    private static final int[][] TALL_MINUS1  = {
            {-1, -1}, {-1, 0}, {0, -1}, {0, 1}, {1, 0}, {1, 1}
    };
    private static final int[][] TALL_0       = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] TALL_PLUS1   = {{0, 0}};
    private static final int[][] TALL_PLUS2   = {{0, 0}};

    // ── GIANT leaf layers (relY from trunk top, trunk height ~18) ─────────────
    // Source: green_fir_2 / orange_fir_2

    private static final int[][] GIANT_MINUS15 = {{0, -1}};
    private static final int[][] GIANT_MINUS14 = {{0, 1}};
    private static final int[][] GIANT_MINUS12 = {
            {-3, 0}, {-2, 0}, {0, -3}, {0, -2}, {0, 2}, {0, 3},
            {1, 1}, {2, 0}, {3, 0}
    };
    private static final int[][] GIANT_MINUS11 = {
            {-2, -1}, {-2, 0}, {-2, 1}, {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -2}, {0, -1}, {0, 1}, {0, 2}, {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2},
            {2, -1}, {2, 0}, {2, 1}
    };
    private static final int[][] GIANT_MINUS10 = {
            {-1, -1}, {-1, 0}, {0, -1}, {0, 1}, {1, 0}, {1, 1}
    };
    private static final int[][] GIANT_MINUS9  = {
            {-2, -2}, {-2, -1}, {-2, 2}, {-1, 2}, {1, -2}, {2, -2}, {2, 1}, {2, 2}
    };
    private static final int[][] GIANT_MINUS8  = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, -1}, {-1, 0}, {-1, 1}, {-1, 2},
            {0, -1}, {0, 1}, {1, -2}, {1, -1}, {1, 0}, {1, 1}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] GIANT_MINUS7  = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] GIANT_MINUS6  = {{-2, 0}, {0, -2}, {0, 2}, {2, 0}};
    private static final int[][] GIANT_MINUS5  = {
            {-2, 0}, {-1, -1}, {-1, 0}, {0, -2}, {0, -1}, {0, 1}, {0, 2},
            {1, 0}, {1, 1}, {2, 0}
    };
    private static final int[][] GIANT_MINUS4  = {
            {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] GIANT_MINUS3  = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] GIANT_MINUS2  = {{-1, -1}, {1, 1}};
    private static final int[][] GIANT_MINUS1  = {
            {-1, -1}, {-1, 0}, {0, -1}, {0, 1}, {1, 0}, {1, 1}
    };
    private static final int[][] GIANT_0       = {{-1, 0}, {0, -1}, {0, 1}, {1, 0}};
    private static final int[][] GIANT_PLUS1   = {{0, 0}};
    private static final int[][] GIANT_PLUS2   = {{0, 0}};

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public MapCodec<FirTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.MOSS_BLOCK)) return false;
        if (hasNearbyLog(world, origin, MIN_SPACING)) return false;

        Block leaves = random.nextBoolean()
                ? MainWoods.FIR.leaves().get(0).get()
                : MainWoods.FIR.leaves().get(1).get();
        Block log = MainWoods.FIR.log().get();
        int rot   = random.nextInt(4);

        int roll = random.nextInt(20);
        if      (roll < 4)  return generateTiny  (world, origin, log, leaves, random, rot);
        else if (roll < 9)  return generateSmall (world, origin, log, leaves, random, rot);
        else if (roll < 15) return generateMedium(world, origin, log, leaves, random, rot);
        else if (roll < 18) return generateTall  (world, origin, log, leaves, random, rot);
        else                return generateGiant (world, origin, log, leaves, random, rot);
    }

    // ── TINY ───────────────────────────────────────────────────────────────────

    private boolean generateTiny(WorldGenLevel world, BlockPos origin,
                                 Block log, Block leaves, RandomSource random, int rot) {
        int height = 5 + random.nextInt(3); // 5-7 (NBT uses 6)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        placeLayer(world, top.below(3), leaves, rot, TINY_MINUS3);
        placeLayer(world, top.below(1), leaves, rot, TINY_MINUS1);
        placeLayer(world, top,         leaves, rot, TINY_0);
        placeLayer(world, top.above(1),   leaves, rot, TINY_PLUS1);
        placeLayer(world, top.above(2),   leaves, rot, TINY_PLUS2);

        return true;
    }

    // ── SMALL ──────────────────────────────────────────────────────────────────

    private boolean generateSmall(WorldGenLevel world, BlockPos origin,
                                  Block log, Block leaves, RandomSource random, int rot) {
        int height = 7 + random.nextInt(3); // 7-9 (NBT uses 8)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        placeLayer(world, top.below(5), leaves, rot, SMALL_MINUS5);
        placeLayer(world, top.below(4), leaves, rot, SMALL_MINUS4);
        placeLayer(world, top.below(3), leaves, rot, SMALL_MINUS3);
        // relY -2 intentionally skipped — tier break matches NBT
        placeLayer(world, top.below(1), leaves, rot, SMALL_MINUS1);
        placeLayer(world, top,         leaves, rot, SMALL_0);
        placeLayer(world, top.above(1),   leaves, rot, SMALL_PLUS1);
        placeLayer(world, top.above(2),   leaves, rot, SMALL_PLUS2);

        return true;
    }

    // ── MEDIUM ─────────────────────────────────────────────────────────────────

    private boolean generateMedium(WorldGenLevel world, BlockPos origin,
                                   Block log, Block leaves, RandomSource random, int rot) {
        int height = 10 + random.nextInt(3); // 10-12 (NBT uses 11)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        placeLayer(world, top.below(8), leaves, rot, MED_MINUS8);
        placeLayer(world, top.below(6), leaves, rot, MED_MINUS6);
        placeLayer(world, top.below(5), leaves, rot, MED_MINUS5);
        placeLayer(world, top.below(4), leaves, rot, MED_MINUS4);
        placeLayer(world, top.below(3), leaves, rot, MED_MINUS3);
        placeLayer(world, top.below(2), leaves, rot, MED_MINUS2);
        placeLayer(world, top.below(1), leaves, rot, MED_MINUS1);
        placeLayer(world, top,         leaves, rot, MED_0);
        placeLayer(world, top.above(1),   leaves, rot, MED_PLUS1);
        placeLayer(world, top.above(2),   leaves, rot, MED_PLUS2);

        return true;
    }

    private boolean generateTall(WorldGenLevel world, BlockPos origin,
                                 Block log, Block leaves, RandomSource random, int rot) {
        int height = 14 + random.nextInt(3); // 14-16 (NBT uses 15)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        placeLayer(world, top.below(12), leaves, rot, TALL_MINUS12);
        placeLayer(world, top.below(11), leaves, rot, TALL_MINUS11);
        placeLayer(world, top.below(10), leaves, rot, TALL_MINUS10);
        placeLayer(world, top.below(9),  leaves, rot, TALL_MINUS9);
        placeLayer(world, top.below(8),  leaves, rot, TALL_MINUS8);
        placeLayer(world, top.below(7),  leaves, rot, TALL_MINUS7);
        placeLayer(world, top.below(6),  leaves, rot, TALL_MINUS6);
        placeLayer(world, top.below(5),  leaves, rot, TALL_MINUS5);
        placeLayer(world, top.below(4),  leaves, rot, TALL_MINUS4);
        placeLayer(world, top.below(3),  leaves, rot, TALL_MINUS3);
        placeLayer(world, top.below(2),  leaves, rot, TALL_MINUS2);
        placeLayer(world, top.below(1),  leaves, rot, TALL_MINUS1);
        placeLayer(world, top,          leaves, rot, TALL_0);
        placeLayer(world, top.above(1),    leaves, rot, TALL_PLUS1);
        placeLayer(world, top.above(2),    leaves, rot, TALL_PLUS2);

        return true;
    }

    // ── GIANT ──────────────────────────────────────────────────────────────────

    private boolean generateGiant(WorldGenLevel world, BlockPos origin,
                                  Block log, Block leaves, RandomSource random, int rot) {
        int height = 17 + random.nextInt(3); // 17-19 (NBT uses 18)
        for (int dy = 0; dy < height; dy++) placeLog(world, origin.above(dy), log);

        BlockPos top = origin.above(height);

        placeLayer(world, top.below(15), leaves, rot, GIANT_MINUS15);
        placeLayer(world, top.below(14), leaves, rot, GIANT_MINUS14);
        placeLayer(world, top.below(12), leaves, rot, GIANT_MINUS12);
        placeLayer(world, top.below(11), leaves, rot, GIANT_MINUS11);
        placeLayer(world, top.below(10), leaves, rot, GIANT_MINUS10);
        placeLayer(world, top.below(9),  leaves, rot, GIANT_MINUS9);
        placeLayer(world, top.below(8),  leaves, rot, GIANT_MINUS8);
        placeLayer(world, top.below(7),  leaves, rot, GIANT_MINUS7);
        placeLayer(world, top.below(6),  leaves, rot, GIANT_MINUS6);
        placeLayer(world, top.below(5),  leaves, rot, GIANT_MINUS5);
        placeLayer(world, top.below(4),  leaves, rot, GIANT_MINUS4);
        placeLayer(world, top.below(3),  leaves, rot, GIANT_MINUS3);
        placeLayer(world, top.below(2),  leaves, rot, GIANT_MINUS2);
        placeLayer(world, top.below(1),  leaves, rot, GIANT_MINUS1);
        placeLayer(world, top,          leaves, rot, GIANT_0);
        placeLayer(world, top.above(1),    leaves, rot, GIANT_PLUS1);
        placeLayer(world, top.above(2),    leaves, rot, GIANT_PLUS2);

        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

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