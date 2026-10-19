package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainSpecialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Generates orange mycena mushrooms faithful to the 3 NBT structure shapes.
 *
 * Shape 0 (large): tall stem 4-6 blocks, branching horizontal stems,
 *                  wide cap ring, dense top cap.
 * Shape 1 (small): stem 2-3 blocks, cross cap with diagonals, compact.
 * Shape 2 (tiny):  stem 1 block, simple cross cap.
 *
 * Variation per placement:
 *   - Stem height randomised within each variant's range
 *   - Entire structure randomly rotated 0/90/180/270 degrees
 *   - Cap blocks have a ~25% chance to have a small or tall mycena on top
 *   - Scattered mycena appear around the base
 */
public record OrangeMycenaFeature() implements Feature {
    public static final MapCodec<OrangeMycenaFeature> CODEC = MapCodec.unit(OrangeMycenaFeature::new);

    @Override
    public MapCodec<OrangeMycenaFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState ground = world.getBlockState(origin.below());
        if (!ground.is(BlockTags.DIRT)
                && !ground.is(Blocks.GRASS_BLOCK)
                && !ground.is(Blocks.MYCELIUM)
                && !ground.is(Blocks.PODZOL)) {
            return false;
        }

        int variant  = random.nextInt(3);
        int rotation = random.nextInt(4); // 0=none, 1=90, 2=180, 3=270

        return switch (variant) {
            case 0 -> generateLarge(world, origin, random, rotation);
            case 1 -> generateSmall(world, origin, random, rotation);
            default -> generateTiny(world, origin, random, rotation);
        };
    }

    // ── Shape 2 — tiny (faithful to orange_mycena_2.nbt) ─────────────────────
    // Original: 3×3×3, 1 stem, cross cap at y=1 with one mycena decoration
    private boolean generateTiny(WorldGenLevel world, BlockPos origin,
                                 RandomSource random, int rot) {
        int stemH = 1; // always 1 for tiny

        if (!canPlace(world, origin) || !canPlace(world, origin.above())) return false;

        // Stem
        place(world, origin, stemY());

        // Cap — cross shape matching NBT (center + 4 cardinal + 1-2 diagonals)
        BlockPos cap = origin.above(stemH);
        placeCap(world, cap, random);

        // Scatter 1-2 mycena around base
        return true;
    }

    // ── Shape 1 — small (faithful to orange_mycena_1.nbt) ────────────────────
    // Original: 3×4×3, 2 stem, cross+diagonal cap at y=2, mycena at y=3
    private boolean generateSmall(WorldGenLevel world, BlockPos origin,
                                  RandomSource random, int rot) {
        int stemH = 2 + random.nextInt(2); // 2-3

        for (int dy = 0; dy <= stemH + 1; dy++) {
            if (!canPlace(world, origin.above(dy))) return false;
        }

        // Vertical stem
        for (int dy = 0; dy < stemH; dy++) {
            place(world, origin.above(dy), stemY());
        }

        // Cap — asymmetric cross matching NBT shape 1
        // NBT had: [0,2,1][0,2,2][1,2,0][1,2,1][1,2,2][2,2,0][2,2,1]
        // Centered on stem (1,2,1) → offsets: (-1,0,0)(-1,0,1)(0,0,-1)(0,0,0)
        //   (0,0,1)(1,0,-1)(1,0,0)
        // Rotated per placement:
        BlockPos capBase = origin.above(stemH);
        int[][] capOffsets = {
                {-1, 0}, {-1, 1}, {0, -1}, {0, 0}, {0, 1}, {1, -1}, {1, 0}
        };
        for (int[] off : capOffsets) {
            int rx = rotX(off[0], off[1], rot);
            int rz = rotZ(off[0], off[1], rot);
            place(world, capBase.offset(rx, 0, rz), cap());
            // ~25% chance of mycena decoration on top of each cap block
            maybeCapDecor(world, capBase.offset(rx, 0, rz), random);
        }

        // Decoration at top matching NBT (1,3,0) → (-0,1,-1) from cap center
        int[] topOff = rotatedOff(0, -1, rot);
        BlockPos topDecor = capBase.above().offset(topOff[0], 0, topOff[1]);
        if (world.getBlockState(topDecor).isAir()) {
            world.setBlock(topDecor, smallMycena(), Block.UPDATE_ALL);
        }

        return true;
    }

    // ── Shape 0 — large (faithful to orange_mycena_0.nbt) ────────────────────
    // Original: 5×9×5, tall stem, branching, wide cap ring, dense top cap
    private boolean generateLarge(WorldGenLevel world, BlockPos origin,
                                  RandomSource random, int rot) {
        int stemH = 4 + random.nextInt(3); // 4-6 (NBT had 5)

        for (int dy = 0; dy <= stemH + 3; dy++) {
            if (!canPlace(world, origin.above(dy))) return false;
        }

        // Vertical stem
        for (int dy = 0; dy < stemH; dy++) {
            place(world, origin.above(dy), stemY());
        }

        // Branching stem at y=1 — NBT had (3,1,1) offset from center (2,y,2)
        // → offset (+1, 0, -1) from origin
        {
            int[] br = rotatedOff(1, -1, rot);
            place(world, origin.above(1).offset(br[0], 0, br[1]), stemY());
        }

        // Lower cap cluster at y=2 — NBT offsets from center (2,2,2):
        // (0,2,0)(0,2,1)(1,2,0)(1,2,1)(1,2,2)(2,2,1)(2,2,2)
        // → relative: (-2,0,-2)(-2,0,-1)(-1,0,-2)(-1,0,-1)(-1,0,0)(0,0,-1)(0,0,0)
        int[][] lowerCap = {
                {-2,-2},{-2,-1},{-1,-2},{-1,-1},{-1,0},{0,-1},{0,0}
        };
        for (int[] off : lowerCap) {
            int[] r = rotatedOff(off[0], off[1], rot);
            place(world, origin.above(2).offset(r[0], 0, r[1]), cap());
            maybeCapDecor(world, origin.above(2).offset(r[0], 0, r[1]), random);
        }

        // Upper cap ring at stemH — NBT had radiating pattern
        // Outer ring offsets from center:
        int[][] upperRing = {
                {-2,-1},{-2,0},{-2,1},
                {-1,-2},{-1,2},
                {0,-2},{0,2},
                {1,-2},{1,2},
                {2,-1},{2,0},{2,1}
        };
        for (int[] off : upperRing) {
            int[] r = rotatedOff(off[0], off[1], rot);
            place(world, origin.above(stemH).offset(r[0], 0, r[1]), cap());
        }

        // Radiating stems at stemH matching NBT axis pattern
        // NBT had stems at various axes filling the ring
        int[][] ringStems = {
                {-1,-1},{0,-1},{1,-1},  // z-axis stems
                {-1,0},{0,0},{1,0},     // y at center
                {-1,1},{0,1},{1,1}      // z-axis stems
        };
        Direction.Axis[] ringAxes = {
                axisZ(rot), axisX(rot), axisX(rot),
                axisZ(rot), Direction.Axis.Y, axisZ(rot),
                axisX(rot), axisX(rot), axisZ(rot)
        };
        for (int i = 0; i < ringStems.length; i++) {
            int[] r = rotatedOff(ringStems[i][0], ringStems[i][1], rot);
            BlockPos sp = origin.above(stemH).offset(r[0], 0, r[1]);
            BlockState cur = world.getBlockState(sp);
            // Only place stem if cap isn't already there
            if (cur.isAir() || cur.canBeReplaced()) {
                place(world, sp,
                        MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get().defaultBlockState()
                                .setValue(RotatedPillarBlock.AXIS, ringAxes[i]));
            }
        }

        // Dense top cap at stemH+1 — 3×3 with random corners
        BlockPos topCap = origin.above(stemH + 1);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                if (corner && random.nextBoolean()) continue;
                int[] r = rotatedOff(dx, dz, rot);
                place(world, topCap.offset(r[0], 0, r[1]), cap());
                maybeCapDecor(world, topCap.offset(r[0], 0, r[1]), random);
            }
        }

        // Tip
        place(world, origin.above(stemH + 2), cap());
        maybeCapDecor(world, origin.above(stemH + 2), random);

        // Scattered decorations at base — NBT had 2 orange_mycena and 1 tall
        return true;
    }

    // ── Cap decoration helper ─────────────────────────────────────────────────

    /** 25% chance to place a small or tall mycena on top of a cap block. */
    private static void maybeCapDecor(WorldGenLevel world, BlockPos capPos, RandomSource random) {
        if (random.nextFloat() >= 0.25f) return;
        BlockPos above = capPos.above();
        if (!world.getBlockState(above).isAir()) return;
        world.setBlock(above,
                random.nextBoolean() ? smallMycena() : tallMycena(),
                Block.UPDATE_ALL);
    }

    // ── Scatter helper ────────────────────────────────────────────────────────

    // ── Cap shape helper ─────────────────────────────────────────────────────

    /** Places a compact cross cap for the tiny variant. */
    private static void placeCap(WorldGenLevel world, BlockPos center, RandomSource random) {
        place(world, center, cap());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            place(world, center.relative(dir), cap());
            maybeCapDecor(world, center.relative(dir), random);
        }
        // 1-2 diagonal cap blocks
        int diags = 1 + random.nextInt(2);
        Direction[] hDirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
        for (int i = 0; i < diags; i++) {
            Direction d = hDirs[random.nextInt(hDirs.length)];
            Direction d2 = hDirs[random.nextInt(hDirs.length)];
            place(world, center.relative(d).relative(d2), cap());
        }
        maybeCapDecor(world, center, random);
    }

    // ── Rotation helpers ──────────────────────────────────────────────────────

    private static int rotX(int x, int z, int rot) {
        return switch (rot) {
            case 1  -> -z;
            case 2  -> -x;
            case 3  ->  z;
            default ->  x;
        };
    }

    private static int rotZ(int x, int z, int rot) {
        return switch (rot) {
            case 1  ->  x;
            case 2  -> -z;
            case 3  -> -x;
            default ->  z;
        };
    }

    private static int[] rotatedOff(int x, int z, int rot) {
        return new int[]{ rotX(x, z, rot), rotZ(x, z, rot) };
    }

    /** Returns the X axis, rotated — 90/270 swaps X↔Z */
    private static Direction.Axis axisX(int rot) {
        return (rot == 1 || rot == 3) ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** Returns the Z axis, rotated */
    private static Direction.Axis axisZ(int rot) {
        return (rot == 1 || rot == 3) ? Direction.Axis.X : Direction.Axis.Z;
    }

    // ── Block state helpers ───────────────────────────────────────────────────

    private static void place(WorldGenLevel world, BlockPos pos, BlockState state) {
        BlockState cur = world.getBlockState(pos);
        if (cur.isAir() || cur.canBeReplaced()
                || cur.is(MainPlants.ORANGE_MYCENA.get())
                || cur.is(MainPlants.TALL_ORANGE_MYCENA.get())) {
            world.setBlock(pos, state, Block.UPDATE_ALL);
        }
    }

    private static boolean canPlace(WorldGenLevel world, BlockPos pos) {
        BlockState s = world.getBlockState(pos);
        return s.isAir() || s.canBeReplaced()
                || s.is(MainPlants.ORANGE_MYCENA.get())
                || s.is(MainPlants.TALL_ORANGE_MYCENA.get());
    }

    private static BlockState stemY() {
        return MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    private static BlockState stemAxis(Direction.Axis axis) {
        return MainSpecialBlocks.MYCENA_MUSHROOM_STEM.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState cap()       { return MainSpecialBlocks.MYCENA_MUSHROOM_BLOCK.get().defaultBlockState(); }
    private static BlockState smallMycena() { return MainPlants.ORANGE_MYCENA.get().defaultBlockState(); }
    private static BlockState tallMycena()  { return MainPlants.TALL_ORANGE_MYCENA.get().defaultBlockState(); }

    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 3; dy >= -3; dy--) {
            BlockPos c     = target.offset(0, dy, 0);
            BlockState below = world.getBlockState(c.below());
            BlockState at    = world.getBlockState(c);
            if (!below.isAir() && !below.is(Blocks.WATER) && at.isAir()) return c;
        }
        return null;
    }

    // ── Public entry point for bonemeal / MycenaPatchFeature ─────────────────

    public static boolean generateAt(WorldGenLevel world, BlockPos pos, RandomSource random) {
        BlockState ground = world.getBlockState(pos.below());
        if (!ground.is(BlockTags.DIRT)
                && !ground.is(Blocks.GRASS_BLOCK)
                && !ground.is(Blocks.MYCELIUM)
                && !ground.is(Blocks.PODZOL)) {
            return false;
        }
        int variant  = random.nextInt(3);
        int rotation = random.nextInt(4);
        OrangeMycenaFeature feature = new OrangeMycenaFeature();
        return switch (variant) {
            case 0 -> feature.generateLarge(world, pos, random, rotation);
            case 1 -> feature.generateSmall(world, pos, random, rotation);
            default -> feature.generateTiny(world, pos, random, rotation);
        };
    }
}