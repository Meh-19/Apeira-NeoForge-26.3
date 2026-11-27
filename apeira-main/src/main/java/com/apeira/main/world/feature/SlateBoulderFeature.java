package com.apeira.main.world.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import com.apeira.main.block.CloverBlock;
import com.apeira.main.block.DecorationBlock;
import com.apeira.main.registry.MainPlants;
import com.apeira.main.registry.MainRocks;
import com.apeira.main.registry.MainStones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * One of five slate boulder shapes (taken from Xenon's {@code slate_0}-{@code slate_4} structures), at the edge of
 * water: on a bank beside water or under at most {@link #MAX_DEPTH} blocks of it.
 *
 * <ul>
 * <li>Never deep in water, and never on an underwater hill: every column round the boulder must be under at most
 * {@link #MAX_AROUND_DEPTH} blocks of water.</li>
 * <li>It is set into the ground (its bottom layer replaces dirt, sand, gravel and stone where the ground rises under
 * it, and the larger shapes sit a block down), and where the ground falls away under part of it, slate supports are
 * built down to the ground, up to {@link #MAX_SUPPORT} blocks; over a deeper drop it is not placed.</li>
 * <li>Upper corners and edges are sometimes slabs, so it looks rounder.</li>
 * <li>Its dry tops get damp moss, and dry or flooded tops rocks and pebbles in counts of one to three; cattails and
 * clover grow round it.</li>
 * </ul>
 */
public record SlateBoulderFeature() implements Feature {
    public static final MapCodec<SlateBoulderFeature> CODEC = MapCodec.unit(SlateBoulderFeature::new);

    /** The most water over the boulder's own spot. */
    private static final int MAX_DEPTH = 2;
    /** The most water over any column within two blocks of the boulder. */
    private static final int MAX_AROUND_DEPTH = 3;
    /** The most support blocks under one column; a larger drop and nothing is placed. */
    private static final int MAX_SUPPORT = 3;

    // Shapes as {dx, dy, dz} from the centre of the bottom layer.
    /** slate_0: small dome. */
    private static final int[][] SMALL_DOME = {
            {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {0, 0, -1}, {0, 0, 0}, {0, 0, 1}, {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
            {-1, 1, 0}, {-1, 1, 1}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}, {1, 1, -1}, {1, 1, 0},
            {-1, 2, 0}, {-1, 2, 1}, {0, 2, 0}, {0, 2, 1}
    };
    /** slate_4: low flat boulder. */
    private static final int[][] LOW = {
            {-2, 0, -1}, {-1, 0, -1}, {-1, 0, 0}, {0, 0, -1}, {0, 0, 0}, {0, 0, 1}, {1, 0, 0}, {1, 0, 1},
            {-1, 1, -1}, {-1, 1, 0}, {0, 1, -1}, {0, 1, 0}
    };
    /** slate_3: medium dome. */
    private static final int[][] MEDIUM_DOME = {
            {-2, 0, 0}, {-2, 0, 1}, {-2, 0, 2}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {-1, 0, 2},
            {0, 0, -1}, {0, 0, 0}, {0, 0, 1}, {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
            {-1, 1, 0}, {-1, 1, 1}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}, {1, 1, -1}, {1, 1, 0},
            {-1, 2, 0}, {-1, 2, 1}, {0, 2, 0}, {0, 2, 1}
    };
    /** slate_1: large boulder. */
    private static final int[][] LARGE = {
            {-2, 0, -2}, {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 2}, {-2, 0, 3},
            {-1, 0, -2}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {-1, 0, 2}, {-1, 0, 3},
            {0, 0, -2}, {0, 0, -1}, {0, 0, 0}, {0, 0, 1}, {0, 0, 2},
            {1, 0, -2}, {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
            {-2, 1, -2}, {-2, 1, -1},
            {-1, 1, -2}, {-1, 1, -1}, {-1, 1, 0}, {-1, 1, 1}, {-1, 1, 2},
            {0, 1, -2}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}, {0, 1, 2},
            {1, 1, -1}, {1, 1, 0}, {1, 1, 1},
            {-1, 2, -2}, {-1, 2, -1}, {-1, 2, 0},
            {0, 2, -2}, {0, 2, -1}, {0, 2, 0}, {0, 2, 1},
            {1, 2, 0}, {1, 2, 1},
            {-1, 3, -1}, {-1, 3, 0}, {0, 3, -1}, {0, 3, 0}
    };
    /** slate_2: tiny flat 2x2. */
    private static final int[][] TINY = {{0, 0, 0}, {0, 0, 1}, {1, 0, 0}, {1, 0, 1}};

    private static final int[][][] SHAPES = {SMALL_DOME, LOW, MEDIUM_DOME, LARGE, TINY};

    @Override
    public MapCodec<SlateBoulderFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int floorY = floor(world, origin.getX(), origin.getZ(), origin.getY() + 3, origin.getY() - 6);
        if (floorY == Integer.MIN_VALUE) {
            return false;
        }
        BlockPos ground = new BlockPos(origin.getX(), floorY, origin.getZ());
        int depth = waterDepth(world, ground);
        if (depth > MAX_DEPTH || depth == 0 && !waterNear(world, ground.above(), 4)) {
            return false;
        }

        int[][] shape = SHAPES[random.nextInt(SHAPES.length)];
        int turn = random.nextInt(4);
        int layers = 0;
        for (int[] cell : shape) {
            layers = Math.max(layers, cell[1] + 1);
        }
        // Tall boulders sit a block down, so they look set into the ground rather than placed on it.
        int baseY = layers >= 3 ? floorY : floorY + 1;

        Set<BlockPos> cells = new HashSet<>();
        Map<Long, Integer> lowest = new HashMap<>();
        for (int[] cell : shape) {
            int[] r = turn(cell[0], cell[2], turn);
            BlockPos pos = new BlockPos(origin.getX() + r[0], baseY + cell[1], origin.getZ() + r[1]);
            cells.add(pos);
            lowest.merge(columnKey(pos), pos.getY(), Math::min);
        }

        // No underwater hills: the ground round the boulder must be as shallow as its own spot.
        for (long key : lowest.keySet()) {
            int x = BlockPos.getX(key), z = BlockPos.getZ(key);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int f = floor(world, x + dx, z + dz, baseY + 4, baseY - 8);
                    // No ground within reach is deep water or a cliff: both rule the spot out.
                    if (f == Integer.MIN_VALUE || waterDepth(world, new BlockPos(x + dx, f, z + dz)) > MAX_AROUND_DEPTH) {
                        return false;
                    }
                }
            }
        }

        // Supports where the ground drops away under a column; too deep a drop and nothing is placed.
        List<BlockPos> supports = new ArrayList<>();
        for (Map.Entry<Long, Integer> column : lowest.entrySet()) {
            BlockPos pos = new BlockPos(BlockPos.getX(column.getKey()), column.getValue() - 1, BlockPos.getZ(column.getKey()));
            int count = 0;
            while (isFree(world.getBlockState(pos))) {
                if (++count > MAX_SUPPORT) {
                    return false;
                }
                supports.add(pos);
                pos = pos.below();
            }
        }

        BlockState slate = MainStones.SLATE.get().defaultBlockState();
        Set<BlockPos> placed = new HashSet<>();
        for (BlockPos pos : cells) {
            BlockState current = world.getBlockState(pos);
            if (isFree(current) || isGround(current)) {
                world.setBlock(pos, slate, Block.UPDATE_CLIENTS);
                placed.add(pos);
            }
        }
        if (placed.isEmpty()) {
            return false;
        }
        for (BlockPos pos : supports) {
            world.setBlock(pos, slate, Block.UPDATE_CLIENTS);
        }

        roundEdges(world, random, cells, placed);
        decorateTops(world, random, placed);
        decorateAround(world, random, origin.atY(floorY + 1));
        return true;
    }

    /** Some upper corners and edges become bottom slabs, waterlogged under water. */
    private static void roundEdges(WorldGenLevel world, RandomSource random, Set<BlockPos> cells, Set<BlockPos> placed) {
        SlabBlock slab = MainRocks.SLATE_SHAPES.slab() == null ? null : MainRocks.SLATE_SHAPES.slab().get();
        if (slab == null) {
            return;
        }
        int bottom = Integer.MAX_VALUE;
        for (BlockPos pos : cells) {
            bottom = Math.min(bottom, pos.getY());
        }
        for (BlockPos pos : List.copyOf(placed)) {
            if (pos.getY() == bottom || cells.contains(pos.above()) || !isFree(world.getBlockState(pos.above()))) {
                continue;
            }
            int open = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!cells.contains(pos.relative(direction))) {
                    open++;
                }
            }
            if (open >= 2 && random.nextFloat() < 0.4F) {
                boolean wet = world.getFluidState(pos.above()).is(FluidTags.WATER);
                world.setBlock(pos, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM).setValue(SlabBlock.WATERLOGGED, wet),
                        Block.UPDATE_CLIENTS);
                placed.remove(pos);
            }
        }
    }

    /** Damp moss on dry tops, rocks and pebbles (one to three) on dry or flooded ones. */
    private static void decorateTops(WorldGenLevel world, RandomSource random, Set<BlockPos> placed) {
        for (BlockPos pos : placed) {
            BlockPos above = pos.above();
            if (placed.contains(above)) {
                continue;
            }
            BlockState aboveState = world.getBlockState(above);
            boolean dry = aboveState.isAir();
            boolean flooded = aboveState.is(Blocks.WATER) && world.getFluidState(above).isSource();
            if (!dry && !flooded) {
                continue;
            }
            float roll = random.nextFloat();
            if (dry && roll < 0.35F) {
                BlockState moss = MainPlants.DAMP_MOSS.get().defaultBlockState();
                if (moss.canSurvive(world, above)) {
                    world.setBlock(above, moss, Block.UPDATE_CLIENTS);
                }
            } else if (roll < (dry ? 0.5F : 0.15F)) {
                placeStones(world, random, above, flooded);
            }
        }
    }

    /** Cattails and clover at the water's edge round the boulder, and a few stones on the ground beside it. */
    private static void decorateAround(WorldGenLevel world, RandomSource random, BlockPos centre) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos column = centre.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4);
            int roll = random.nextInt(10);
            if (roll < 4) {
                WaterEdgeCattailFeature.tryPlace(world, column);
                continue;
            }
            int f = floor(world, column.getX(), column.getZ(), column.getY() + 4, column.getY() - 4);
            if (f == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos pos = new BlockPos(column.getX(), f + 1, column.getZ());
            BlockState at = world.getBlockState(pos);
            boolean flooded = at.is(Blocks.WATER) && world.getFluidState(pos).isSource();
            if (!at.isAir() && !flooded) {
                continue;
            }
            if (roll < 7) {
                BlockState clover = MainPlants.CLOVER.get().defaultBlockState().setValue(CloverBlock.WATERLOGGED, flooded);
                if ((flooded || waterNear(world, pos, 1)) && clover.canSurvive(world, pos)) {
                    world.setBlock(pos, clover, Block.UPDATE_CLIENTS);
                }
            } else if (roll < 8) {
                placeStones(world, random, pos, flooded);
            }
        }
    }

    /** Rock or pebbles at {@code pos}, one to three of them (one most often). */
    private static void placeStones(WorldGenLevel world, RandomSource random, BlockPos pos, boolean waterlogged) {
        Block block = random.nextBoolean() ? MainPlants.ROCK.get() : MainPlants.PEBBLES.get();
        float roll = random.nextFloat();
        int count = roll < 0.6F ? 1 : roll < 0.9F ? 2 : 3;
        BlockState state = block.defaultBlockState().setValue(DecorationBlock.COUNT, count).setValue(DecorationBlock.WATERLOGGED, waterlogged);
        if (state.canSurvive(world, pos)) {
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** The y of the highest solid block in a column between {@code fromY} and {@code toY}, or {@code MIN_VALUE}. */
    private static int floor(WorldGenLevel world, int x, int z, int fromY, int toY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, fromY, z);
        for (int y = fromY; y >= toY; y--) {
            pos.setY(y);
            BlockState state = world.getBlockState(pos);
            if (!isFree(state) && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    /** Blocks of water over the ground block {@code ground}. */
    private static int waterDepth(WorldGenLevel world, BlockPos ground) {
        int depth = 0;
        BlockPos pos = ground.above();
        while (depth <= MAX_AROUND_DEPTH && world.getFluidState(pos).is(FluidTags.WATER)) {
            depth++;
            pos = pos.above();
        }
        return depth;
    }

    private static boolean waterNear(WorldGenLevel world, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos side = pos.offset(dx, 0, dz);
                if (world.getFluidState(side).is(FluidTags.WATER) || world.getFluidState(side.below()).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Air, water or a plant that may be replaced: where slate may go and what supports fill. */
    private static boolean isFree(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.canBeReplaced();
    }

    /** Natural ground that the boulder's bottom may sink into. */
    private static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static long columnKey(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), 0, pos.getZ());
    }

    private static int[] turn(int dx, int dz, int turn) {
        return switch (turn) {
            case 1 -> new int[] {dz, -dx};
            case 2 -> new int[] {-dx, -dz};
            case 3 -> new int[] {-dz, dx};
            default -> new int[] {dx, dz};
        };
    }
}
