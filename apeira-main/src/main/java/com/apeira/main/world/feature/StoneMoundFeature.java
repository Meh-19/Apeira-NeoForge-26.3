package com.apeira.main.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.apeira.main.block.DecorationBlock;
import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places one of five hardcoded stone mound variants (V0–V4).
 *
 * When {@code decorations} is false (used for slate water mound),
 * only the main stone type is placed — no rocks or pebbles.
 */
public record StoneMoundFeature(BlockState stoneState, boolean waterlogged, boolean decorations) implements Feature {
    public static final MapCodec<StoneMoundFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockState.CODEC.fieldOf("stone_state").forGetter(StoneMoundFeature::stoneState),
            Codec.BOOL.fieldOf("waterlogged").forGetter(StoneMoundFeature::waterlogged),
            Codec.BOOL.fieldOf("decorations").forGetter(StoneMoundFeature::decorations)
    ).apply(instance, StoneMoundFeature::new));

    private static final int STONE  = 0;
    private static final int ROCK   = 1;
    private static final int PEBBLE = 2;

    // ── Original variants (V0–V4) ──────────────────────────────────────────

    private static final int[][] V0 = {
            {0,0,0,STONE},{0,0,1,STONE},{0,0,2,STONE},{0,0,3,STONE},
            {1,0,0,STONE},{1,0,1,STONE},{1,0,2,STONE},{1,0,3,STONE},
            {2,0,0,ROCK}, {2,0,1,STONE},{2,0,2,STONE},{2,0,3,STONE},
            {3,0,1,STONE},{3,0,2,STONE},{3,0,3,STONE},
            {0,1,0,STONE},{0,1,1,STONE},
            {1,1,0,STONE},{1,1,1,STONE},{1,1,2,STONE},
            {2,1,1,STONE},{2,1,2,STONE},{2,1,3,STONE},
            {3,1,2,STONE},{3,1,3,STONE},
            {1,2,1,STONE},{1,2,2,STONE},{2,2,1,STONE},{2,2,2,STONE},
            {3,2,3,ROCK},
            {1,3,1,ROCK},
    };
    private static final int[] SZ0 = {4, 4};

    private static final int[][] V1 = {
            {0,0,0,STONE},{0,0,1,STONE},
            {1,0,0,STONE},{1,0,1,STONE},{1,0,2,STONE},{1,0,3,STONE},{1,0,4,ROCK},
            {2,0,0,PEBBLE},{2,0,1,STONE},{2,0,2,STONE},{2,0,3,STONE},
            {3,0,1,STONE},{3,0,2,STONE},{3,0,3,STONE},
            {1,1,0,ROCK},{1,1,1,STONE},{1,1,2,STONE},
            {2,1,1,STONE},{2,1,2,STONE},{2,1,3,STONE},
            {3,1,1,PEBBLE},{3,1,2,STONE},{3,1,3,STONE},
            {1,2,1,STONE},{1,2,2,STONE},{2,2,1,STONE},{2,2,2,STONE},
            {2,3,2,ROCK},
    };
    private static final int[] SZ1 = {4, 5};

    private static final int[][] V2 = {
            {0,0,1,STONE},{0,0,2,STONE},{0,0,3,STONE},
            {1,0,0,STONE},{1,0,1,STONE},{1,0,2,STONE},{1,0,3,ROCK},
            {2,0,0,STONE},{2,0,1,STONE},
            {0,1,1,STONE},{0,1,2,STONE},{1,1,1,STONE},{1,1,2,STONE},
            {0,2,1,PEBBLE},
    };
    private static final int[] SZ2 = {3, 4};

    private static final int[][] V3 = {
            {0,0,0,STONE},{0,0,1,STONE},
            {1,0,0,STONE},{1,0,1,STONE},{1,0,2,STONE},
            {2,0,0,PEBBLE},{2,0,1,STONE},{2,0,2,STONE},
            {0,1,0,STONE},{0,1,1,STONE},
            {1,1,0,STONE},{1,1,1,STONE},{1,1,2,ROCK},
            {2,1,1,PEBBLE},
            {0,2,0,PEBBLE},{1,2,1,PEBBLE},
    };
    private static final int[] SZ3 = {3, 3};

    private static final int[][] V4 = {
            {0,0,1,ROCK},
            {1,0,0,STONE},{1,0,1,STONE},
            {2,0,0,STONE},{2,0,1,STONE},
            {2,1,0,PEBBLE},
    };
    private static final int[] SZ4 = {3, 2};


    // ─────────────────────────────────────────────────────────────────────────

    private static final int[][][] VARIANTS = {V0, V1, V2, V3, V4};
    private static final int[][]   SIZES    = {SZ0, SZ1, SZ2, SZ3, SZ4};

    @Override
    public MapCodec<StoneMoundFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        BlockState surfaceBlock = world.getBlockState(origin);
        if (surfaceBlock.isAir() || surfaceBlock.canBeReplaced()) return false;

        int vi      = random.nextInt(VARIANTS.length);
        int[][] var = VARIANTS[vi];
        int sizeX   = SIZES[vi][0];
        int sizeZ   = SIZES[vi][1];

        boolean mirrorX = random.nextBoolean();
        boolean mirrorZ = random.nextBoolean();

        int offX = sizeX / 2;
        int offZ = sizeZ / 2;

        boolean anyPlaced = false;

        for (int[] blk : var) {
            int bx = blk[0], by = blk[1], bz = blk[2], type = blk[3];

            if (!decorations && type != STONE) continue;

            int rx = mirrorX ? (sizeX - 1 - bx) : bx;
            int rz = mirrorZ ? (sizeZ - 1 - bz) : bz;

            BlockPos pos = origin.offset(rx - offX, by - 1, rz - offZ);

            BlockState state = buildState(type, random);
            if (state == null) continue;

            BlockState current = world.getBlockState(pos);

            boolean canPlace = (by == 0)
                    ? (!current.isAir())
                    : (current.isAir() || current.is(Blocks.WATER));

            if (canPlace) {
                world.setBlock(pos, state, Block.UPDATE_ALL);
                anyPlaced = true;
            }
        }

        return anyPlaced;
    }

    private BlockState buildState(int type, RandomSource random) {
        return switch (type) {
            case STONE  -> stoneState;
            case ROCK   -> decorState(MainPlants.ROCK.get().defaultBlockState(),    random, waterlogged);
            case PEBBLE -> decorState(MainPlants.PEBBLES.get().defaultBlockState(), random, waterlogged);
            default     -> null;
        };
    }

    private static BlockState decorState(BlockState base, RandomSource random, boolean waterlogged) {
        if (base.hasProperty(DecorationBlock.COUNT)) {
            base = base.setValue(DecorationBlock.COUNT, 1 + random.nextInt(3));
        }
        if (waterlogged && base.hasProperty(BlockStateProperties.WATERLOGGED)) {
            base = base.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return base;
    }

}