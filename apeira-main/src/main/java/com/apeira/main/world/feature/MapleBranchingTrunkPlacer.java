package com.apeira.main.world.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.apeira.main.registry.MainFeatures;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

/**
 * Xenon's {@code maple_branching_trunk}: a straight trunk with three one-block arms, at a third, half and two thirds of
 * its height in shuffled directions, each with a foliage cluster above its tip, and a one-step-larger cluster on top.
 * Xenon registered it but no configured tree uses it, so it is here for data packs.
 */
public class MapleBranchingTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<MapleBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
            instance -> trunkPlacerParts(instance).apply(instance, MapleBranchingTrunkPlacer::new));

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public MapleBranchingTrunkPlacer(int baseHeight, int heightRandA, int heightRandB) {
        super(baseHeight, heightRandA, heightRandB);
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return MainFeatures.MAPLE_BRANCHING_TRUNK.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(WorldGenLevel level, BiConsumer<BlockPos, BlockState> trunkSetter,
            RandomSource random, int treeHeight, BlockPos origin, TreeFeature tree) {
        placeBelowTrunkBlock(level, trunkSetter, random, origin.below(), tree);
        for (int dy = 0; dy < treeHeight; dy++) {
            placeLog(level, trunkSetter, random, origin.above(dy), tree);
        }

        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
        int[] armHeights = {treeHeight / 3, treeHeight / 2, treeHeight * 2 / 3};
        Direction[] directions = HORIZONTALS.clone();
        for (int i = 3; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction swap = directions[i];
            directions[i] = directions[j];
            directions[j] = swap;
        }
        for (int i = 0; i < 3; i++) {
            BlockPos tip = origin.above(armHeights[i]).relative(directions[i]);
            placeLog(level, trunkSetter, random, tip, tree);
            attachments.add(new FoliagePlacer.FoliageAttachment(tip.above(), 0, false));
        }
        attachments.add(new FoliagePlacer.FoliageAttachment(origin.above(treeHeight), 1, false));
        return attachments;
    }
}
