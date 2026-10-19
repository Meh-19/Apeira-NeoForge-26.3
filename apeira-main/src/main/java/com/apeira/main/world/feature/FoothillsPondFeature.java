package com.apeira.main.world.feature;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Carves a large shallow pond into the terrain.
 *
 * The pond is an irregular oval depression:
 *   - Radius 5–9 blocks (X), 4–7 blocks (Z), independently randomised
 *   - Depth 2–3 blocks at centre, tapering to 1 at the edge
 *   - Floor: silt (60%), clay (25%), gravel (15%) — all placed below water
 *   - Water fills the depression up to the original surface level
 *   - Edge band (~1–2 blocks wide): silt replaces grass/dirt on the bank
 *   - Vegetation: lily pads, small lilies, and cattails placed on/near water
 *
 * Placed via OCEAN_FLOOR_WG so origin lands on solid ground, not water.
 * The feature bails early if the origin is already underwater or floating in air.
 */
public record FoothillsPondFeature() implements Feature {
    public static final MapCodec<FoothillsPondFeature> CODEC = MapCodec.unit(FoothillsPondFeature::new);

    @Override
    public MapCodec<FoothillsPondFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        // Don't carve if the origin is already water or air
        BlockState originState = world.getBlockState(origin);
        if (originState.is(Blocks.WATER) || originState.isAir()) return false;

        // Don't carve if there's no solid ground within 2 blocks down
        boolean foundGround = false;
        for (int dy = 0; dy >= -2; dy--) {
            BlockState s = world.getBlockState(origin.offset(0, dy, 0));
            if (!s.isAir() && !s.is(Blocks.WATER)) { foundGround = true; break; }
        }
        if (!foundGround) return false;

        int radiusX   = 5 + random.nextInt(5);  // 5-9
        int radiusZ   = 4 + random.nextInt(4);  // 4-7
        int maxDepth  = 2 + random.nextInt(2);  // 2-3
        int waterY    = origin.getY();           // water surface = original surface

        // ── Pass 1: carve and fill ────────────────────────────────────────────
        int placed = 0;
        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                // Normalised distance (ellipse)
                float normDist = ((float)(dx * dx) / (radiusX * radiusX))
                        + ((float)(dz * dz) / (radiusZ * radiusZ));

                // Edge noise — makes the pond shape irregular
                float edgeNoise = 0.85f + random.nextFloat() * 0.25f;
                if (normDist > edgeNoise) continue;

                // Depth tapers from maxDepth at centre to 1 at edge
                int depth = Math.max(1, Math.round(maxDepth * (1.0f - normDist / edgeNoise)));

                // Dig downward from waterY - depth up to waterY - 1
                for (int dy = -depth; dy < 0; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState current = world.getBlockState(pos);

                    // Dig through dirt, grass, stone, gravel, sand, existing water
                    if (current.isAir()) continue;
                    if (isDiggable(current)) {
                        if (dy == -depth) {
                            // Bottom layer — place floor material
                            world.setBlock(pos, pickFloor(random), Block.UPDATE_ALL);
                        } else {
                            // Interior layers — water
                            world.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                        }
                        placed++;
                    }
                }

                // Fill water from floor+1 up to waterY-1
                for (int dy = -depth + 1; dy < 0; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (world.getBlockState(pos).isAir() || isDiggable(world.getBlockState(pos))) {
                        world.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        if (placed == 0) return false;

        // ── Pass 2: bank silt + vegetation ───────────────────────────────────
        int bankRadius = 2; // silt extends this many blocks beyond the pond edge
        for (int dx = -(radiusX + bankRadius); dx <= (radiusX + bankRadius); dx++) {
            for (int dz = -(radiusZ + bankRadius); dz <= (radiusZ + bankRadius); dz++) {
                float normDist = ((float)(dx * dx) / (radiusX * radiusX))
                        + ((float)(dz * dz) / (radiusZ * radiusZ));
                // Only process the bank fringe, not the interior
                if (normDist <= 0.85f) continue;

                float bankDist = normDist - 0.85f;
                if (bankDist > 0.6f) continue;

                // Replace grass/dirt with silt on the bank
                BlockPos bankSurface = findSurface(world, origin.offset(dx, 0, dz));
                if (bankSurface != null) {
                    BlockState bs = world.getBlockState(bankSurface);
                    if ((bs.is(Blocks.GRASS_BLOCK) || bs.is(BlockTags.DIRT))
                            && random.nextFloat() < 0.6f) {
                        world.setBlock(bankSurface, MainStones.SILT.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }

        // ── Pass 3: surface water vegetation ─────────────────────────────────
        int vegAttempts = 12 + random.nextInt(10);
        for (int attempt = 0; attempt < vegAttempts; attempt++) {
            int dx = random.nextInt(radiusX * 2 + 1) - radiusX;
            int dz = random.nextInt(radiusZ * 2 + 1) - radiusZ;

            float normDist = ((float)(dx * dx) / (radiusX * radiusX))
                    + ((float)(dz * dz) / (radiusZ * radiusZ));
            if (normDist > 0.85f) continue;

            BlockPos waterSurface = findWaterSurface(world, origin.offset(dx, 0, dz));
            if (waterSurface == null) continue;

            BlockPos above = waterSurface.above();
            if (!world.getBlockState(above).isAir()) continue;

            // Pick vegetation by weighted roll
            int roll = random.nextInt(10);
            if (roll < 4) {
                // Lily pad (40%)
                world.setBlock(above, Blocks.LILY_PAD.defaultBlockState(), Block.UPDATE_ALL);
            } else if (roll < 7) {
                // Small lily (30%)
                world.setBlock(above,
                        MainPlants.SMALL_LILY.get().defaultBlockState(),
                        Block.UPDATE_ALL);
            }
            // 30% — leave bare water
        }

        // Cattails around the edge
        int cattailAttempts = 6 + random.nextInt(6);
        for (int attempt = 0; attempt < cattailAttempts; attempt++) {
            int dx = (int)((radiusX + 1) * (random.nextFloat() * 2 - 1));
            int dz = (int)((radiusZ + 1) * (random.nextFloat() * 2 - 1));

            BlockPos ground = findGroundNearWater(world, origin.offset(dx, 0, dz));
            if (ground == null) continue;
            if (!world.getBlockState(ground).isAir()) continue;
            if (!world.getBlockState(ground.above()).isAir()) continue;

            DoublePlantBlock.placeAt(world, MainPlants.CATTAIL.get().defaultBlockState(), ground, Block.UPDATE_CLIENTS);
        }

        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static BlockState pickFloor(RandomSource random) {
        int roll = random.nextInt(20);
        if (roll < 12) return MainStones.SILT.get().defaultBlockState();
        if (roll < 17) return Blocks.CLAY.defaultBlockState();
        return Blocks.GRAVEL.defaultBlockState();
    }

    private static boolean isDiggable(BlockState s) {
        return s.is(BlockTags.DIRT)
                || s.is(Blocks.GRASS_BLOCK)
                || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SAND)
                || s.is(Blocks.CLAY)
                || s.is(Blocks.STONE)
                || s.is(Blocks.WATER)
                || s.is(MainStones.SILT.get())
                || s.is(MainRocks.LOAM.get());
    }

    /** Find the topmost non-air, non-water surface block within ±4 Y. */
    private static BlockPos findSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos c = target.offset(0, dy, 0);
            BlockState s = world.getBlockState(c);
            BlockState above = world.getBlockState(c.above());
            if (!s.isAir() && !s.is(Blocks.WATER) && (above.isAir() || above.is(Blocks.WATER))) {
                return c;
            }
        }
        return null;
    }

    /** Find the top water block (water with air above) within ±4 Y. */
    private static BlockPos findWaterSurface(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -8; dy--) {
            BlockPos c = target.offset(0, dy, 0);
            if (world.getBlockState(c).is(Blocks.WATER)
                    && world.getBlockState(c.above()).isAir()) {
                return c;
            }
        }
        return null;
    }

    /** Find ground that is directly adjacent to water, for cattail placement. */
    private static BlockPos findGroundNearWater(WorldGenLevel world, BlockPos target) {
        for (int dy = 4; dy >= -4; dy--) {
            BlockPos c = target.offset(0, dy, 0);
            BlockState below = world.getBlockState(c.below());
            BlockState at    = world.getBlockState(c);
            if (at.isAir() && !below.isAir() && !below.is(Blocks.WATER)) {
                // Check adjacent water
                for (int ddx = -1; ddx <= 1; ddx++) {
                    for (int ddz = -1; ddz <= 1; ddz++) {
                        if (world.getBlockState(c.offset(ddx, 0, ddz)).is(Blocks.WATER)) return c;
                        if (world.getBlockState(c.offset(ddx, -1, ddz)).is(Blocks.WATER)) return c;
                    }
                }
            }
        }
        return null;
    }
}