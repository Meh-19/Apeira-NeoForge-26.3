package com.apeira.main.world.feature;

import java.util.List;

import com.mojang.serialization.MapCodec;

import com.apeira.main.registry.MainWoods;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Xenon's maple tree, placed from Ren's structure files ({@link StructureTree}), the same as the fir tree. The five shapes
 * are {@code orange_maple_0-4} / {@code red_maple_0-4} (each shape the same in both colours, block for block; Ren's
 * {@code orange_maple_1d} is shape 1), shipped once as {@code data/apeira/structure/maple/maple_<n>.nbt}, drawn with
 * orange leaves; each tree is orange or red, half each. Shape 0 is the large tree with arching branches, 1-3 the normal
 * ones and 4 the tall one, at the odds the hand-copied version had (large 15%, normal 75%, tall 10%).
 */
public record MapleTreeFeature() implements Feature {
    public static final MapCodec<MapleTreeFeature> CODEC = MapCodec.unit(MapleTreeFeature::new);

    /** Odds of shapes 0-4: large, three normal, tall. */
    private static final StructureTree TREE = new StructureTree("maple", new int[] {15, 25, 25, 25, 10}, 6);

    @Override
    public MapCodec<MapleTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        return TREE.place(world, random, origin, MainWoods.MAPLE.log().get(),
                List.of(MainWoods.MAPLE.leaves().get(0).get(), MainWoods.MAPLE.leaves().get(1).get()));
    }
}
