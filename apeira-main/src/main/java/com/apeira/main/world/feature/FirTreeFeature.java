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
 * Xenon's fir tree, placed from its structure files ({@link StructureTree}). The five shapes are Xenon's
 * {@code green_fir_0-4} / {@code orange_fir_0-4} (each shape existed once per colour, block for block the same), shipped
 * once as {@code data/apeira/structure/fir/fir_<n>.nbt} in size order (0 the 3x8 sapling-sized tree ... 4 the 7x20
 * giant), drawn with green leaves; each tree is green or orange, half each.
 */
public record FirTreeFeature() implements Feature {
    public static final MapCodec<FirTreeFeature> CODEC = MapCodec.unit(FirTreeFeature::new);

    /** Odds of shapes 0-4: tiny, small, medium, tall, giant. */
    private static final StructureTree TREE = new StructureTree("fir", new int[] {4, 5, 6, 3, 2}, 5);

    @Override
    public MapCodec<FirTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        return TREE.place(world, random, origin, MainWoods.FIR.log().get(),
                List.of(MainWoods.FIR.leaves().get(0).get(), MainWoods.FIR.leaves().get(1).get()));
    }
}
