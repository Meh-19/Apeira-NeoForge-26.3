package com.apeira.main.world;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.apeira.main.block.DecorationBlock;
import com.apeira.main.registry.MainCrystals;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

/**
 * Xenon's Depths terrain ({@code DepthsTerrainMixin}), run on every overworld chunk by {@code mixin/DepthsTerrainMixin}:
 * from y -128 to -66 a lava-floored cavern carved by noise, with a bedrock floor, towers rising from the lava, deepslate
 * pillars, magma shores, tuff and slate walls, damp moss hanging from the ceiling, rocks, pebbles, mushrooms, cobwebs
 * and crystal clusters; chasms opening it up to y -50, and now and then a steep shaft from y 30 down into it. The
 * overworld reaches down to y -128 for it ({@code data/minecraft/dimension_type/overworld.json}). Everything is
 * decided by the world seed and block positions, so chunks agree at their borders.
 */
public final class DepthsTerrain {
    // ── Y-level constants ────────────────────────────────────────────────
    private static final int DEPTHS_BOTTOM = -128;
    private static final int BEDROCK_TOP   = -124;
    private static final int LAVA_LEVEL    = -112;
    private static final int MAIN_TOP      = -66;
    private static final int BLEND_TOP     = -50;

    // ── Noise samplers, seeded from the world seed ───────────────────────
    private static volatile @Nullable DepthsTerrain current;

    private final long seed;
    private final PerlinNoise cave1;
    private final PerlinNoise cave2;
    private final PerlinNoise detail;
    private final PerlinNoise chasmNoise;
    private final PerlinNoise pillarNoise;

    // ── Cached default block-states ──────────────────────────────────────
    private static final BlockState DSL = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BDR = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState LVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState MAG = Blocks.MAGMA_BLOCK.defaultBlockState();
    private static final BlockState TUF = Blocks.TUFF.defaultBlockState();
    private static final BlockState COB = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    private static final BlockState WEB = Blocks.COBWEB.defaultBlockState();
    private static final BlockState MSH = Blocks.BROWN_MUSHROOM.defaultBlockState();

    // ── Tower footprint sizes (for rotation) ────────────────────────────
    private static final int[] TOWER_SIZES = { 4, 4, 3, 5, 3 };

    // ── Tower 0 : 4×4 footprint, height 22 ──────────────────────────────
    private static final int[][] TOWER_0 = {
            {0,0,0},{1,0,0},{2,0,0},{3,0,0},{0,0,1},{1,0,1},{2,0,1},{3,0,1},
            {0,0,2},{1,0,2},{2,0,2},{3,0,2},{0,0,3},{1,0,3},{2,0,3},{3,0,3},
            {0,1,0},{1,1,0},{2,1,0},{3,1,0},{0,1,1},{1,1,1},{2,1,1},{3,1,1},
            {0,1,2},{1,1,2},{2,1,2},{3,1,2},{0,1,3},{1,1,3},{2,1,3},{3,1,3},
            {0,2,0},{1,2,0},{2,2,0},{3,2,0},{0,2,1},{1,2,1},{2,2,1},{3,2,1},
            {0,2,2},{1,2,2},{2,2,2},{3,2,2},{0,2,3},{1,2,3},{2,2,3},{3,2,3},
            {0,3,0},{1,3,0},{2,3,0},{3,3,0},{0,3,1},{1,3,1},{2,3,1},{3,3,1},
            {0,3,2},{1,3,2},{2,3,2},{3,3,2},{0,3,3},{1,3,3},{2,3,3},{3,3,3},
            {0,4,0},{1,4,0},{2,4,0},{3,4,0},{0,4,1},{1,4,1},{2,4,1},{3,4,1},
            {0,4,2},{1,4,2},{2,4,2},{3,4,2},{0,4,3},{1,4,3},{2,4,3},{3,4,3},
            {0,5,0},{1,5,0},{2,5,0},{3,5,0},{0,5,1},{1,5,1},{2,5,1},{3,5,1},
            {0,5,2},{1,5,2},{2,5,2},{3,5,2},{0,5,3},{1,5,3},{2,5,3},{3,5,3},
            {1,6,0},{2,6,0},{3,6,0},{0,6,1},{1,6,1},{2,6,1},{3,6,1},
            {0,6,2},{1,6,2},{2,6,2},{3,6,2},{0,6,3},{1,6,3},{2,6,3},{3,6,3},
            {1,7,0},{2,7,0},{0,7,1},{1,7,1},{2,7,1},{3,7,1},
            {0,7,2},{1,7,2},{2,7,2},{3,7,2},{0,7,3},{1,7,3},{2,7,3},{3,7,3},
            {1,8,0},{2,8,0},{0,8,1},{1,8,1},{2,8,1},{3,8,1},
            {0,8,2},{1,8,2},{2,8,2},{3,8,2},{0,8,3},{1,8,3},{2,8,3},
            {1,9,0},{2,9,0},{0,9,1},{1,9,1},{2,9,1},{3,9,1},
            {0,9,2},{1,9,2},{2,9,2},{3,9,2},{1,9,3},{2,9,3},
            {1,10,0},{2,10,0},{0,10,1},{1,10,1},{2,10,1},{3,10,1},
            {0,10,2},{1,10,2},{2,10,2},{3,10,2},{1,10,3},{2,10,3},
            {1,11,0},{2,11,0},{0,11,1},{1,11,1},{2,11,1},{3,11,1},
            {0,11,2},{1,11,2},{2,11,2},{3,11,2},{1,11,3},{2,11,3},
            {1,12,0},{2,12,0},{0,12,1},{1,12,1},{2,12,1},{3,12,1},
            {0,12,2},{1,12,2},{2,12,2},{3,12,2},{1,12,3},
            {2,13,0},{0,13,1},{1,13,1},{2,13,1},{3,13,1},
            {0,13,2},{1,13,2},{2,13,2},{1,13,3},
            {0,14,1},{1,14,1},{2,14,1},{1,14,2},{2,14,2},
            {1,15,1},{2,15,1},{1,15,2},{2,15,2},
            {1,16,1},{2,16,1},{1,16,2},{1,17,1},{1,17,2},
            {1,18,1},{1,19,1},{1,20,1},{1,21,1},
    };

    // ── Tower 1 : 4×4 footprint, height 19 ──────────────────────────────
    private static final int[][] TOWER_1 = {
            {0,0,0},{1,0,0},{2,0,0},{3,0,0},{0,0,1},{1,0,1},{2,0,1},{3,0,1},
            {0,0,2},{1,0,2},{2,0,2},{3,0,2},{0,0,3},{1,0,3},{2,0,3},{3,0,3},
            {0,1,0},{1,1,0},{2,1,0},{3,1,0},{0,1,1},{1,1,1},{2,1,1},{3,1,1},
            {0,1,2},{1,1,2},{2,1,2},{3,1,2},{0,1,3},{1,1,3},{2,1,3},{3,1,3},
            {0,2,0},{1,2,0},{2,2,0},{3,2,0},{0,2,1},{1,2,1},{2,2,1},{3,2,1},
            {0,2,2},{1,2,2},{2,2,2},{3,2,2},{0,2,3},{1,2,3},{2,2,3},{3,2,3},
            {0,3,0},{1,3,0},{2,3,0},{3,3,0},{0,3,1},{1,3,1},{2,3,1},{3,3,1},
            {0,3,2},{1,3,2},{2,3,2},{3,3,2},{0,3,3},{1,3,3},{2,3,3},{3,3,3},
            {0,4,0},{1,4,0},{2,4,0},{3,4,0},{0,4,1},{1,4,1},{2,4,1},{3,4,1},
            {0,4,2},{1,4,2},{2,4,2},{3,4,2},{0,4,3},{1,4,3},{2,4,3},{3,4,3},
            {0,5,0},{1,5,0},{2,5,0},{3,5,0},{0,5,1},{1,5,1},{2,5,1},{3,5,1},
            {0,5,2},{1,5,2},{2,5,2},{3,5,2},{0,5,3},{1,5,3},{2,5,3},{3,5,3},
            {0,6,0},{1,6,0},{2,6,0},{3,6,0},{0,6,1},{1,6,1},{2,6,1},{3,6,1},
            {0,6,2},{1,6,2},{2,6,2},{3,6,2},{0,6,3},{1,6,3},{2,6,3},{3,6,3},
            {0,7,0},{1,7,0},{2,7,0},{3,7,0},{0,7,1},{1,7,1},{2,7,1},{3,7,1},
            {0,7,2},{1,7,2},{2,7,2},{3,7,2},{0,7,3},{1,7,3},{2,7,3},{3,7,3},
            {0,8,0},{1,8,0},{2,8,0},{3,8,0},{0,8,1},{1,8,1},{2,8,1},{3,8,1},
            {0,8,2},{1,8,2},{2,8,2},{3,8,2},{0,8,3},{1,8,3},{2,8,3},{3,8,3},
            {1,9,0},{2,9,0},{3,9,0},{0,9,1},{1,9,1},{2,9,1},{3,9,1},
            {0,9,2},{1,9,2},{2,9,2},{3,9,2},{1,9,3},{2,9,3},{3,9,3},
            {2,10,0},{3,10,0},{0,10,1},{1,10,1},{2,10,1},{3,10,1},
            {0,10,2},{1,10,2},{2,10,2},{3,10,2},{1,10,3},{2,10,3},
            {2,11,0},{0,11,1},{1,11,1},{2,11,1},{1,11,2},{2,11,2},{1,11,3},
            {1,12,1},{2,12,1},{1,12,2},{2,12,2},
            {1,13,1},{2,13,1},{1,13,2},{2,13,2},
            {1,14,1},{2,14,1},{2,14,2},{1,15,1},{2,15,1},
            {2,16,1},{2,17,1},{2,18,1},
    };

    // ── Tower 2 : 3×3 footprint, height 15 ──────────────────────────────
    private static final int[][] TOWER_2 = {
            {0,0,0},{1,0,0},{2,0,0},{0,0,1},{1,0,1},{2,0,1},{0,0,2},{1,0,2},{2,0,2},
            {0,1,0},{1,1,0},{2,1,0},{0,1,1},{1,1,1},{2,1,1},{0,1,2},{1,1,2},{2,1,2},
            {0,2,0},{1,2,0},{2,2,0},{0,2,1},{1,2,1},{2,2,1},{0,2,2},{1,2,2},{2,2,2},
            {0,3,0},{1,3,0},{2,3,0},{0,3,1},{1,3,1},{2,3,1},{0,3,2},{1,3,2},{2,3,2},
            {0,4,0},{1,4,0},{2,4,0},{0,4,1},{1,4,1},{2,4,1},{0,4,2},{1,4,2},{2,4,2},
            {0,5,0},{1,5,0},{2,5,0},{0,5,1},{1,5,1},{2,5,1},{0,5,2},{1,5,2},{2,5,2},
            {0,6,0},{1,6,0},{2,6,0},{0,6,1},{1,6,1},{2,6,1},{0,6,2},{1,6,2},{2,6,2},
            {0,7,0},{1,7,0},{0,7,1},{1,7,1},{2,7,1},{0,7,2},{1,7,2},{2,7,2},
            {0,8,0},{1,8,0},{0,8,1},{1,8,1},{2,8,1},{0,8,2},{1,8,2},
            {0,9,0},{0,9,1},{1,9,1},{1,9,2},
            {1,10,1},{1,10,2},{1,11,1},{1,12,1},{1,13,1},{1,14,1},
    };

    // ── Tower 3 : 5×5 footprint, height 20 ──────────────────────────────
    private static final int[][] TOWER_3 = {
            {0,0,0},{1,0,0},{2,0,0},{3,0,0},{4,0,0},
            {0,0,1},{1,0,1},{2,0,1},{3,0,1},{4,0,1},
            {0,0,2},{1,0,2},{2,0,2},{3,0,2},{4,0,2},
            {0,0,3},{1,0,3},{2,0,3},{3,0,3},{4,0,3},
            {0,0,4},{1,0,4},{2,0,4},{3,0,4},{4,0,4},
            {0,1,0},{1,1,0},{2,1,0},{3,1,0},{4,1,0},
            {0,1,1},{1,1,1},{2,1,1},{3,1,1},{4,1,1},
            {0,1,2},{1,1,2},{2,1,2},{3,1,2},{4,1,2},
            {0,1,3},{1,1,3},{2,1,3},{3,1,3},{4,1,3},
            {0,1,4},{1,1,4},{2,1,4},{3,1,4},{4,1,4},
            {0,2,0},{1,2,0},{2,2,0},{3,2,0},{4,2,0},
            {0,2,1},{1,2,1},{2,2,1},{3,2,1},{4,2,1},
            {0,2,2},{1,2,2},{2,2,2},{3,2,2},{4,2,2},
            {0,2,3},{1,2,3},{2,2,3},{3,2,3},{4,2,3},
            {0,2,4},{1,2,4},{2,2,4},{3,2,4},{4,2,4},
            {2,3,0},{3,3,0},{4,3,0},{1,3,1},{2,3,1},{3,3,1},{4,3,1},
            {0,3,2},{1,3,2},{2,3,2},{3,3,2},{4,3,2},
            {0,3,3},{1,3,3},{2,3,3},{3,3,3},{4,3,3},{1,3,4},{2,3,4},{3,3,4},
            {4,4,0},{1,4,1},{2,4,1},{3,4,1},{4,4,1},
            {0,4,2},{1,4,2},{2,4,2},{3,4,2},{4,4,2},
            {0,4,3},{1,4,3},{2,4,3},{3,4,3},{4,4,3},{1,4,4},{2,4,4},
            {1,5,1},{2,5,1},{3,5,1},{4,5,1},
            {0,5,2},{1,5,2},{2,5,2},{3,5,2},{4,5,2},
            {1,5,3},{2,5,3},{3,5,3},{2,5,4},
            {1,6,1},{2,6,1},{3,6,1},{1,6,2},{2,6,2},{3,6,2},{1,6,3},{2,6,3},
            {1,7,1},{2,7,1},{1,7,2},{2,7,2},{3,7,2},{1,7,3},
            {2,8,1},{1,8,2},{2,8,2},
            {2,9,1},{1,9,2},{2,9,2},
            {2,10,2},{2,11,2},{3,11,2},{2,12,2},{3,12,2},{1,12,3},
            {3,13,1},{2,13,2},{3,13,2},{1,13,3},
            {2,14,1},{3,14,1},{1,14,2},{2,14,2},{3,14,2},{1,14,3},{2,14,3},
            {1,15,1},{2,15,1},{3,15,1},{1,15,2},{2,15,2},{3,15,2},{1,15,3},{2,15,3},{3,15,3},
            {2,16,1},{3,16,1},{1,16,2},{2,16,2},{3,16,2},{2,16,3},
            {2,17,1},{1,17,2},{2,17,2},{2,17,3},
            {2,18,2},{2,19,2},
    };

    // ── Tower 4 : 3×3 footprint, height 28 ──────────────────────────────
    private static final int[][] TOWER_4 = {
            {0,0,0},{1,0,0},{2,0,0},{0,0,1},{1,0,1},{2,0,1},{0,0,2},{1,0,2},{2,0,2},
            {0,1,0},{1,1,0},{2,1,0},{0,1,1},{1,1,1},{2,1,1},{0,1,2},{1,1,2},
            {1,2,0},{2,2,0},{0,2,1},{1,2,1},{2,2,1},{0,2,2},
            {1,3,0},{0,3,1},{1,3,1},{2,3,1},{1,3,2},
            {1,4,0},{0,4,1},{1,4,1},{2,4,1},{0,4,2},{1,4,2},{2,4,2},
            {0,5,0},{1,5,0},{0,5,1},{1,5,1},{2,5,1},{0,5,2},{1,5,2},{2,5,2},
            {0,6,0},{1,6,0},{2,6,0},{0,6,1},{1,6,1},{2,6,1},{1,6,2},
            {1,7,0},{2,7,0},{0,7,1},{1,7,1},{2,7,1},{1,7,2},
            {1,8,0},{2,8,0},{1,8,1},{2,8,1},{0,8,2},{1,8,2},
            {1,9,0},{2,9,0},{0,9,1},{1,9,1},{2,9,1},{0,9,2},{1,9,2},
            {0,10,0},{1,10,0},{2,10,0},{0,10,1},{1,10,1},{2,10,1},{0,10,2},{1,10,2},{2,10,2},
            {1,11,0},{2,11,0},{0,11,1},{1,11,1},{2,11,1},{0,11,2},{1,11,2},{2,11,2},
            {0,12,1},{1,12,1},{2,12,1},{0,12,2},{1,12,2},
            {1,13,1},{1,14,1},{1,15,1},{1,16,1},{1,17,1},{1,18,1},
            {1,19,1},{1,20,1},{1,21,1},{1,22,1},
            {0,23,1},{1,23,1},{2,23,2},
            {1,24,0},{0,24,1},{1,24,1},{2,24,1},{2,24,2},
            {0,25,0},{1,25,0},{0,25,1},{1,25,1},{2,25,1},{1,25,2},{2,25,2},
            {0,26,0},{1,26,0},{2,26,0},{0,26,1},{1,26,1},{2,26,1},{0,26,2},{1,26,2},{2,26,2},
            {1,27,0},{0,27,1},{1,27,1},{2,27,1},{1,27,2},
    };

    // ── Crystal cluster shapes ───────────────────────────────────────────
    private static final int[][] CRYSTAL_V0 = {
            {0,0,0,0},{1,0,0,0},{0,0,1,0},{1,0,1,0},
            {1,1,0,0},{0,1,1,1},{1,1,1,0},{1,2,0,1},{1,2,1,0},{1,3,1,1},
    };
    private static final int[][] CRYSTAL_V1 = {
            {0,0,0,0},{1,0,0,0},{0,0,1,0},{1,0,1,0},
            {0,1,0,0},{1,1,0,1},{0,1,1,0},{1,1,1,0},
            {0,2,0,0},{0,2,1,1},{0,3,0,1},
    };

    // ── Helper: pick tower shape by variant index ────────────────────────
    private static int[][] getTower(int variant) {
        if (variant == 1) return TOWER_1;
        if (variant == 2) return TOWER_2;
        if (variant == 3) return TOWER_3;
        if (variant == 4) return TOWER_4;
        return TOWER_0;
    }

    // ── Helper: crystal block/tip pairs by colour ────────────────────────
    private static Block[][] crystalPairs() {
        return MainCrystals.CRYSTALS.stream().map(crystal -> new Block[] {crystal.block().get(), crystal.cluster().get()})
                .toArray(Block[][]::new);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MAIN INJECT
    // ════════════════════════════════════════════════════════════════════════
    private DepthsTerrain(long seed) {
        this.seed = seed;
        cave1 = new PerlinNoise(new LegacyRandomSource(seed ^ 0x4445505448534CL));
        cave2 = new PerlinNoise(new LegacyRandomSource(seed ^ 0xCAFEBABEDEADL));
        detail = new PerlinNoise(new LegacyRandomSource(seed ^ 0xAA6AAB1L));
        chasmNoise = new PerlinNoise(new LegacyRandomSource(seed ^ 0xC4A5A00BL));
        pillarNoise = new PerlinNoise(new LegacyRandomSource(seed ^ 0xB107E55L));
    }

    /** The Depths' generator for {@code seed}, made once per seed. */
    public static DepthsTerrain forSeed(long seed) {
        DepthsTerrain terrain = current;
        if (terrain == null || terrain.seed != seed) {
            terrain = new DepthsTerrain(seed);
            current = terrain;
        }
        return terrain;
    }

    /**
     * Reshapes everything below y -50 of {@code chunk}, after the noise and surface rules and before the carvers. Does
     * nothing in a dimension that does not reach down to y -128.
     */
    public void generate(ChunkAccess chunk) {
        if (chunk.getMinY() > DEPTHS_BOTTOM) {
            return;
        }

        int sx = chunk.getPos().getMinBlockX();
        int sz = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int yRange = BLEND_TOP - DEPTHS_BOTTOM + 1;
        boolean[][][] carved = new boolean[16][yRange][16];

        int[][] floorY = new int[16][16];
        int[][] ceilY  = new int[16][16];
        for (int[] row : floorY) Arrays.fill(row, -999);
        for (int[] row : ceilY)  Arrays.fill(row, -999);

        // ══════════════════════════════════════════════════════════════════
        //  PASS 0 — Fill transition gap y = -68 to -58
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = -68; y <= -58; y++) {
                    pos.set(sx + lx, y, sz + lz);
                    BlockState ex = chunk.getBlockState(pos);
                    if (ex.is(Blocks.BEDROCK) || ex.isAir() || ex.is(Blocks.CAVE_AIR)) {
                        chunk.setBlockState(pos, DSL);
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 1 — Main cavern carving
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = sx + lx;
                int wz = sz + lz;

                for (int y = DEPTHS_BOTTOM; y <= MAIN_TOP; y++) {
                    int yi = y - DEPTHS_BOTTOM;
                    pos.set(wx, y, wz);

                    // Bedrock layer
                    if (y <= BEDROCK_TOP) {
                        int d = y - DEPTHS_BOTTOM;
                        if (d == 0 || (d <= 4 && hash(wx, y, wz) % (d + 1) == 0)) {
                            chunk.setBlockState(pos, BDR);
                        } else {
                            chunk.setBlockState(pos, DSL);
                        }
                        continue;
                    }

                    double n1   = cave1.get(wx * 0.014, y * 0.009, wz * 0.014);
                    double n2   = cave2.get(wx * 0.04,  y * 0.03,  wz * 0.04);
                    double cave = n1 * 0.8 + n2 * 0.2;

                    double totalH  = MAIN_TOP - BEDROCK_TOP;
                    double relY    = (y - BEDROCK_TOP) / totalH;
                    double edgeDist = Math.min(relY, 1.0 - relY) * 2.0;
                    edgeDist = Math.min(edgeDist, 1.0);
                    double openness = edgeDist * edgeDist;
                    double threshold = Mth.lerp(openness, 0.3, -0.06);

                    boolean isOpen = cave > threshold;
                    carved[lx][yi][lz] = isOpen;
                    chunk.setBlockState(pos, isOpen ? (y <= LAVA_LEVEL ? LVA : AIR) : solidBlock(wx, y, wz));
                }

                // Track floor and ceiling for later passes
                for (int y = LAVA_LEVEL + 1; y < MAIN_TOP; y++) {
                    int yi = y - DEPTHS_BOTTOM;
                    if (yi <= 0) continue;
                    if (carved[lx][yi][lz] && !carved[lx][yi - 1][lz]) {
                        floorY[lx][lz] = y;
                        break;
                    }
                }
                for (int y = MAIN_TOP; y > LAVA_LEVEL + 5; y--) {
                    int yi = y - DEPTHS_BOTTOM;
                    if (yi + 1 >= yRange) continue;
                    if (!carved[lx][yi][lz] && yi > 0 && carved[lx][yi - 1][lz]) {
                        ceilY[lx][lz] = y;
                        break;
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 2 — Chasm blending upward through transition
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = sx + lx;
                int wz = sz + lz;

                boolean openBelow = carved[lx][MAIN_TOP - DEPTHS_BOTTOM][lz];
                double chasmVal = chasmNoise.get(wx * 0.01, wz * 0.01, 0);

                if (!openBelow || chasmVal <= 0.1) continue;

                for (int y = MAIN_TOP + 1; y <= BLEND_TOP; y++) {
                    double progress = (double)(y - MAIN_TOP) / (BLEND_TOP - MAIN_TOP);
                    if (chasmVal - progress * 0.4 > 0.1) {
                        pos.set(wx, y, wz);
                        chunk.setBlockState(pos, AIR);
                        int yi = y - DEPTHS_BOTTOM;
                        if (yi >= 0 && yi < yRange) {
                            carved[lx][yi][lz] = true;
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 2b — Steep shafts from y=30 down into the depths
        // ══════════════════════════════════════════════════════════════════
        // Starts at y=30 (regular deepslate cave territory) and plunges
        // 2 blocks down per horizontal step. 55 steps reaches y=-80, well
        // into the depths. Players peering in from above see a long,
        // intimidating descent into the dark.
        int tSeed = hash(sx >> 4, 42, sz >> 4);
        if (tSeed % 8 == 0) {
            int tx  = sx + 2 + (tSeed / 8) % 12;
            int tz  = sz + 2 + (tSeed / 24) % 12;
            int dir = tSeed % 4;
            int ddx = (dir == 0 || dir == 1) ? 1 : -1;
            int ddz = (dir == 0 || dir == 3) ? 1 : -1;

            for (int step = 0; step < 55; step++) {
                int bx = tx + ddx * step;
                int bz = tz + ddz * step;
                int by = 30 - step * 2;

                // Wider mouth near the top so the opening is noticeable,
                // narrows to a tight shaft deeper down.
                int radius = (step < 8) ? 2 : 1;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            int cx  = bx + dx;
                            int cz2 = bz + dz;
                            int cy  = by + dy;
                            int clx = cx - sx;
                            int clz = cz2 - sz;
                            if (clx < 0 || clx >= 16 || clz < 0 || clz >= 16) continue;
                            pos.set(cx, cy, cz2);
                            if (chunk.getBlockState(pos).is(Blocks.BEDROCK)) continue;
                            chunk.setBlockState(pos, AIR);
                            int yi = cy - DEPTHS_BOTTOM;
                            if (yi >= 0 && yi < yRange) {
                                carved[clx][yi][clz] = true;
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 3 — Towers (5 NBT-style variants, randomised block types)
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx += 4) {
            for (int lz = 0; lz < 16; lz += 4) {
                int wx = sx + lx;
                int wz = sz + lz;
                int lavaYi = LAVA_LEVEL - DEPTHS_BOTTOM;

                if (lavaYi < 0 || lavaYi >= yRange || !carved[lx][lavaYi][lz]) continue;
                if (hash(wx, LAVA_LEVEL, wz) % 25 != 0) continue;

                int h       = hash(wx + 7, LAVA_LEVEL, wz + 13);
                int variant = h % 5;
                int[][] tower    = getTower(variant);
                int footprint    = TOWER_SIZES[variant];
                int rot          = h % 4;
                int baseY        = LAVA_LEVEL - 5;

                int maxTowerY = 0;
                for (int[] e : tower) {
                    if (e[1] > maxTowerY) maxTowerY = e[1];
                }

                for (int[] e : tower) {
                    int dx = e[0];
                    int dy = e[1];
                    int dz = e[2];
                    int rdx, rdz;
                    int fp = footprint - 1;

                    if (rot == 1) {
                        rdx = fp - dz;
                        rdz = dx;
                    } else if (rot == 2) {
                        rdx = fp - dx;
                        rdz = fp - dz;
                    } else if (rot == 3) {
                        rdx = dz;
                        rdz = fp - dx;
                    } else {
                        rdx = dx;
                        rdz = dz;
                    }

                    int bx = wx + rdx;
                    int bz = wz + rdz;
                    int by = baseY + dy;

                    if (bx - sx < 0 || bx - sx >= 16 || bz - sz < 0 || bz - sz >= 16) continue;
                    if (by < DEPTHS_BOTTOM || by > BLEND_TOP) continue;

                    pos.set(bx, by, bz);
                    double relHeight = (maxTowerY > 0) ? (double) dy / maxTowerY : 0;
                    int bh = hash(bx, by, bz);
                    BlockState block;

                    if (relHeight < 0.25 && bh % 3 == 0) {
                        block = MAG;
                    } else if (bh % 7 == 0) {
                        block = TUF;
                    } else if (bh % 11 == 0) {
                        block = MainStones.SLATE.get().defaultBlockState();
                    } else {
                        block = DSL;
                    }
                    chunk.setBlockState(pos, block);
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 4 — Deepslate pillar columns (radius 1-2)
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = sx + lx;
                int wz = sz + lz;
                double pn = pillarNoise.get(wx * 0.025, wz * 0.025, 0);
                if (pn < 0.55) continue;

                int fy = floorY[lx][lz];
                int cy = ceilY[lx][lz];
                if (fy == -999 || cy == -999 || cy <= fy) continue;

                int radius = pn > 0.62 ? 2 : 1;

                for (int y = fy - 1; y <= cy; y++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (dx * dx + dz * dz > radius * radius) continue;
                            int bx  = wx + dx;
                            int bz  = wz + dz;
                            int blx = bx - sx;
                            int blz = bz - sz;
                            if (blx < 0 || blx >= 16 || blz < 0 || blz >= 16) continue;
                            pos.set(bx, y, bz);
                            chunk.setBlockState(pos, DSL);
                            int yi = y - DEPTHS_BOTTOM;
                            if (yi >= 0 && yi < yRange) {
                                carved[blx][yi][blz] = false;
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 5 — Magma patches, wall variety, ceiling moss
        // ══════════════════════════════════════════════════════════════════
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = sx + lx;
                int wz = sz + lz;

                for (int y = BEDROCK_TOP + 1; y <= MAIN_TOP; y++) {
                    int yi = y - DEPTHS_BOTTOM;
                    if (yi < 0 || yi >= yRange) continue;

                    if (!carved[lx][yi][lz]) {
                        // Magma near lava shore
                        if (y >= LAVA_LEVEL - 1 && y <= LAVA_LEVEL + 1) {
                            if (touchesLava(carved, lx, yi, lz, LAVA_LEVEL, DEPTHS_BOTTOM)) {
                                if (detail.get(wx * 0.12, wz * 0.12, 42.0) > 0.35) {
                                    pos.set(wx, y, wz);
                                    chunk.setBlockState(pos, MAG);
                                }
                            }
                        }

                        // Wall texture: tuff / slate patches
                        boolean isWall =
                                (lx > 0  && carved[lx - 1][yi][lz]) ||
                                        (lx < 15 && carved[lx + 1][yi][lz]) ||
                                        (lz > 0  && carved[lx][yi][lz - 1]) ||
                                        (lz < 15 && carved[lx][yi][lz + 1]);

                        if (isWall) {
                            double wn = detail.get(wx * 0.09, y * 0.09, wz * 0.09 + 77.0);
                            pos.set(wx, y, wz);
                            if (wn > 0.45) {
                                chunk.setBlockState(pos, TUF);
                            } else if (wn < -0.5) {
                                chunk.setBlockState(pos, MainStones.SLATE.get().defaultBlockState());
                            }
                        }
                    }
                }

                // Ceiling moss patches with hanging strands (sparse — patch centres only)
                int cy = ceilY[lx][lz];
                if (cy != -999 && hash(wx, cy, wz) % 180 == 0) {
                    int patchR = 1 + hash(wx + 1, cy, wz + 1) % 2;

                    for (int dx = -patchR; dx <= patchR; dx++) {
                        for (int dz = -patchR; dz <= patchR; dz++) {
                            if (dx * dx + dz * dz > patchR * patchR) continue;
                            int bx  = wx + dx;
                            int bz  = wz + dz;
                            int blx = bx - sx;
                            int blz = bz - sz;
                            if (blx < 0 || blx >= 16 || blz < 0 || blz >= 16) continue;

                            int localCy = ceilY[blx][blz];
                            if (localCy == -999) continue;
                            if (hash(bx, localCy, bz) % 3 == 0) continue;

                            pos.set(bx, localCy, bz);
                            BlockState existing = chunk.getBlockState(pos);
                            if (existing.is(Blocks.DEEPSLATE) || existing.is(Blocks.TUFF) || existing.is(Blocks.COBBLED_DEEPSLATE)) {
                                // Keep the solid ceiling block so the vine has a valid anchor above it.
                                // Place 0-2 stem (body) blocks, then 1 tip block at the end.
                                int bodyLen   = hash(bx + 3, localCy, bz + 5) % 3; // 0, 1, or 2
                                int lastBody  = 0;
                                for (int hdy = 1; hdy <= bodyLen; hdy++) {
                                    int hangY = localCy - hdy;
                                    int hyi   = hangY - DEPTHS_BOTTOM;
                                    if (hyi < 0 || hyi >= yRange || !carved[blx][hyi][blz]) break;
                                    pos.set(bx, hangY, bz);
                                    chunk.setBlockState(pos, MainPlants.DAMP_MOSS_HANG_BOTTOM.get().defaultBlockState());
                                    lastBody = hdy;
                                }
                                // Tip goes one block below the last body (or directly below ceiling)
                                int tipY  = localCy - lastBody - 1;
                                int tipYi = tipY - DEPTHS_BOTTOM;
                                if (tipYi >= 0 && tipYi < yRange && carved[blx][tipYi][blz]) {
                                    pos.set(bx, tipY, bz);
                                    chunk.setBlockState(pos, MainPlants.DAMP_MOSS_HANG.get().defaultBlockState());
                                }
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  PASS 6 — Floor decoration, cobwebs, crystals
        // ══════════════════════════════════════════════════════════════════
        for (int cx = 0; cx < 16; cx += 4) {
            for (int cz2 = 0; cz2 < 16; cz2 += 4) {
                int wx = sx + cx;
                int wz = sz + cz2;
                int fy = floorY[cx][cz2];
                if (fy == -999 || fy <= LAVA_LEVEL) continue;

                int h = hash(wx, fy, wz);

                // Rock / pebble patches  (1/6 chance)
                if (h % 6 == 0) {
                    boolean isRock = (h / 6) % 2 == 0;
                    int cnt = 3 + h % 5;
                    for (int i = 0; i < cnt; i++) {
                        int lx = cx + (hash(wx + i,     fy, wz + i * 3) % 5) - 2;
                        int lz = cz2 + (hash(wx + i * 7, fy, wz + i    ) % 5) - 2;
                        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
                        int lfy = floorY[lx][lz];
                        if (lfy == -999 || lfy <= LAVA_LEVEL) continue;
                        int fyi = lfy - DEPTHS_BOTTOM;
                        if (fyi < 0 || fyi >= yRange || !carved[lx][fyi][lz]) continue;
                        pos.set(sx + lx, lfy, sz + lz);
                        BlockState at = chunk.getBlockState(pos);
                        if (!at.isAir() && !at.is(Blocks.CAVE_AIR)) continue;
                        int c = 1 + hash(wx + i + 5, fy, wz + i + 5) % 3;
                        BlockState deco = isRock
                                ? MainPlants.ROCK.get().defaultBlockState().setValue(DecorationBlock.COUNT, c)
                                : MainPlants.PEBBLES.get().defaultBlockState().setValue(DecorationBlock.COUNT, c);
                        chunk.setBlockState(pos, deco);
                    }
                }

                // Mushroom patches  (1/25 chance)
                if (h % 25 == 1) {
                    int cnt = 1 + h % 2;
                    for (int i = 0; i < cnt; i++) {
                        int lx = cx + (hash(wx + i * 2, fy + 1, wz + i    ) % 5) - 2;
                        int lz = cz2 + (hash(wx + i,     fy + 1, wz + i * 2) % 5) - 2;
                        if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
                        int lfy = floorY[lx][lz];
                        if (lfy == -999 || lfy <= LAVA_LEVEL) continue;
                        int fyi = lfy - DEPTHS_BOTTOM;
                        if (fyi < 0 || fyi >= yRange || !carved[lx][fyi][lz]) continue;
                        pos.set(sx + lx, lfy, sz + lz);
                        BlockState at = chunk.getBlockState(pos);
                        if (!at.isAir() && !at.is(Blocks.CAVE_AIR)) continue;
                        chunk.setBlockState(pos, MSH);
                    }
                }

                // Tuff floor patches  (1/5 chance)
                if (h % 5 == 2) {
                    int r = 2 + h % 2;
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz > r * r) continue;
                            int lx = cx + dx;
                            int lz = cz2 + dz;
                            if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
                            int lfy = floorY[lx][lz];
                            if (lfy == -999 || lfy <= LAVA_LEVEL) continue;
                            pos.set(sx + lx, lfy - 1, sz + lz);
                            if (chunk.getBlockState(pos).is(Blocks.DEEPSLATE)) {
                                chunk.setBlockState(pos, TUF);
                            }
                        }
                    }
                }

                // Slate floor patches  (1/8 chance)
                if (h % 8 == 3) {
                    int r = 1 + h % 3;
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz > r * r) continue;
                            int lx = cx + dx;
                            int lz = cz2 + dz;
                            if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
                            int lfy = floorY[lx][lz];
                            if (lfy == -999 || lfy <= LAVA_LEVEL) continue;
                            pos.set(sx + lx, lfy - 1, sz + lz);
                            BlockState bl = chunk.getBlockState(pos);
                            if (bl.is(Blocks.DEEPSLATE) || bl.is(Blocks.TUFF)) {
                                chunk.setBlockState(pos, MainStones.SLATE.get().defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

        // Cobwebs + crystal clusters
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = sx + lx;
                int wz = sz + lz;
                int cy = ceilY[lx][lz];
                int fy = floorY[lx][lz];

                // Ceiling cobweb patches
                if (cy != -999) {
                    int cyi = cy - 1 - DEPTHS_BOTTOM;
                    if (cyi >= 0 && cyi < yRange && carved[lx][cyi][lz] && hash(wx, cy, wz) % 1500 == 0) {
                        placeCobwebPatch(chunk, wx, cy - 1, wz, sx, sz, carved, DEPTHS_BOTTOM, yRange);
                    }
                }

                // Floor crystals
                if (fy != -999 && fy > LAVA_LEVEL && hash(wx + 3, fy, wz + 5) % 3000 == 0) {
                    placeCrystal(chunk, wx, fy, wz, sx, sz, true);
                }

                // Ceiling crystals
                if (cy != -999) {
                    int cyi = cy - 1 - DEPTHS_BOTTOM;
                    if (cyi >= 0 && cyi < yRange && carved[lx][cyi][lz] && hash(wx + 11, cy, wz + 7) % 2500 == 0) {
                        placeCrystal(chunk, wx, cy - 1, wz, sx, sz, false);
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER — place a crystal cluster (floor or ceiling)
    // ════════════════════════════════════════════════════════════════════════
    private void placeCrystal(ChunkAccess chunk, int wx, int wy, int wz, int cx, int cz, boolean floor) {
        Block[][] pairs = crystalPairs();
        int ci = hash(wx, wy, wz) % pairs.length;
        Block bb = pairs[ci][0];
        Block tb = pairs[ci][1];

        BlockState base = bb.defaultBlockState();
        BlockState tip  = tb.defaultBlockState().setValue(BlockStateProperties.FACING, floor ? Direction.UP : Direction.DOWN);

        int[][] template = (hash(wx + 1, wy, wz + 1) % 2 == 0) ? CRYSTAL_V0 : CRYSTAL_V1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        for (int[] e : template) {
            int dx   = e[0];
            int dy   = e[1];
            int dz   = e[2];
            int type = e[3];
            int bx = wx + dx;
            int bz = wz + dz;
            if (bx - cx < 0 || bx - cx >= 16 || bz - cz < 0 || bz - cz >= 16) continue;
            int by = floor ? wy + dy : wy - dy;
            p.set(bx, by, bz);
            BlockState ex = chunk.getBlockState(p);
            if (!ex.isAir() && !ex.is(Blocks.CAVE_AIR)) continue;
            chunk.setBlockState(p, type == 0 ? base : tip);
        }

        // Fill base blocks below/above
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                int bx = wx + dx;
                int bz = wz + dz;
                if (bx - cx < 0 || bx - cx >= 16 || bz - cz < 0 || bz - cz >= 16) continue;
                if (floor) {
                    for (int f = 1; f <= 2; f++) {
                        p.set(bx, wy - f, bz);
                        BlockState b = chunk.getBlockState(p);
                        if (b.isAir() || b.is(Blocks.CAVE_AIR)) chunk.setBlockState(p, base);
                        else break;
                    }
                } else {
                    for (int f = 1; f <= 2; f++) {
                        p.set(bx, wy + f, bz);
                        BlockState b = chunk.getBlockState(p);
                        if (b.isAir() || b.is(Blocks.CAVE_AIR)) chunk.setBlockState(p, base);
                        else break;
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER — place a cobweb patch hanging from a ceiling
    // ════════════════════════════════════════════════════════════════════════
    private void placeCobwebPatch(ChunkAccess chunk, int cx, int cy, int cz,
                                        int chX, int chZ,
                                        boolean[][][] carved, int db, int yr) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int bx = cx + dx;
                int bz = cz + dz;
                int lx = bx - chX;
                int lz = bz - chZ;
                if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
                if (hash(bx, cy, bz) % 3 == 0) continue;
                int hangLen = 1 + hash(bx, cy + 1, bz) % 2;
                for (int dy = 0; dy < hangLen; dy++) {
                    int y  = cy - dy;
                    int yi = y - db;
                    if (yi < 0 || yi >= yr || !carved[lx][yi][lz]) break;
                    p.set(bx, y, bz);
                    chunk.setBlockState(p, WEB);
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER — pick a solid block variant by noise
    // ════════════════════════════════════════════════════════════════════════
    private BlockState solidBlock(int wx, int y, int wz) {
        double d = detail.get(wx * 0.06, y * 0.06, wz * 0.06);
        if (d > 0.55) return TUF;
        if (d < -0.6) return COB;
        return DSL;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER — check if a solid block touches a lava cell
    // ════════════════════════════════════════════════════════════════════════
    private static boolean touchesLava(boolean[][][] c, int lx, int yi, int lz, int ll, int db) {
        int maxIdx = c[0].length - 1;
        int[][] neighbours = { {-1,0,0},{1,0,0},{0,-1,0},{0,1,0},{0,0,-1},{0,0,1} };
        for (int[] n : neighbours) {
            int nx = lx + n[0];
            int ny = yi + n[1];
            int nz = lz + n[2];
            if (nx < 0 || nx > 15 || nz < 0 || nz > 15 || ny < 0 || ny > maxIdx) continue;
            if (c[nx][ny][nz] && (db + ny) <= ll) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPER — deterministic integer hash (no Random overhead)
    // ════════════════════════════════════════════════════════════════════════
    private static int hash(int x, int y, int z) {
        int h = x * 3129871 ^ z * 116129781 ^ y;
        h = h * h * 42317861 + h * 11;
        return Math.abs((h >> 16) & 0xFFF);
    }
}
