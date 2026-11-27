package com.apeira.main.world.feature;

import java.util.List;
import java.util.Optional;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A tree placed from structure files, shared by {@link FirTreeFeature} and {@link MapleTreeFeature}. Each tree rolls one
 * of its shapes ({@code data/apeira/structure/<folder>/<folder>_<n>.nbt}, by weight), one of its leaf colours, a mirror
 * and a quarter turn about its trunk (the template's log at y 0 nearest its centre), so worldgen and saplings give every
 * shape and colour. A template is drawn with the tree's log and its first leaves colour; every log of the template is
 * placed with its axis turned, every leaves block as the rolled colour (persistent, as Xenon's were).
 *
 * <p>As vanilla's trees: the trunk stands on {@link BlockTags#SUBSTRATE_OVERWORLD} (dirt, grass, podzol, mycelium, moss,
 * mud; 26.x's {@code #minecraft:dirt} is only dirt, coarse and rooted dirt, so a dirt-only check refused grass), logs
 * replace air, water and replaceable plants, leaves only air. Worldgen keeps trees {@code spacing} blocks apart; a
 * sapling ({@code world} is the {@link ServerLevel} then) grows next to other trees, but only when its trunk has room.
 */
public record StructureTree(String folder, int[] weights, int spacing) {
    private static final StructurePlaceSettings PLAIN = new StructurePlaceSettings();

    private Identifier shape(int n) {
        return ApeiraIds.id(folder + "/" + folder + "_" + n);
    }

    /**
     * Places a tree whose trunk's bottom log is at {@code origin}, drawn with {@code log} and one of {@code leaves}; the
     * templates are drawn with {@code log} and {@code leaves.get(0)}.
     */
    public boolean place(WorldGenLevel world, RandomSource random, BlockPos origin, Block log, List<Block> leaves) {
        Block templateLog = log;
        Block templateLeaves = leaves.get(0);
        boolean sapling = world instanceof ServerLevel;
        if (!world.getBlockState(origin.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) return false;
        if (!sapling && hasNearbyLog(world, origin, log)) return false;

        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        int n = 0;
        while (roll >= weights[n]) roll -= weights[n++];
        Optional<StructureTemplate> found = world.getLevel().getStructureTemplateManager().get(shape(n));
        if (found.isEmpty()) {
            Apeira.LOGGER.warn("Missing tree structure {}", shape(n));
            return false;
        }
        StructureTemplate template = found.get();
        Block leafBlock = leaves.get(random.nextInt(leaves.size()));
        boolean mirror = random.nextBoolean();
        int turn = random.nextInt(4);

        List<StructureTemplate.StructureBlockInfo> logs = template.filterBlocks(BlockPos.ZERO, PLAIN, templateLog, false);
        BlockPos trunk = trunk(template, logs);
        if (trunk == null) {
            Apeira.LOGGER.warn("Tree structure {} has no log at its bottom", shape(n));
            return false;
        }
        if (sapling) {
            for (StructureTemplate.StructureBlockInfo info : logs) {
                if (info.pos().getX() != trunk.getX() || info.pos().getZ() != trunk.getZ()) continue;
                BlockPos pos = at(origin, info.pos(), trunk, mirror, turn);
                if (!world.isInsideBuildHeight(pos) || !takesLog(world.getBlockState(pos))) return false;
            }
        }

        Rotation axisTurn = turn % 2 == 1 ? Rotation.CLOCKWISE_90 : Rotation.NONE;
        for (StructureTemplate.StructureBlockInfo info : logs) {
            BlockPos pos = at(origin, info.pos(), trunk, mirror, turn);
            BlockState state = log.withPropertiesOf(info.state()).rotate(axisTurn);
            if (takesLog(world.getBlockState(pos))) world.setBlock(pos, state, Block.UPDATE_ALL);
        }
        BlockState leaf = leafBlock.defaultBlockState().trySetValue(LeavesBlock.PERSISTENT, true);
        for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(BlockPos.ZERO, PLAIN, templateLeaves, false)) {
            BlockPos pos = at(origin, info.pos(), trunk, mirror, turn);
            if (world.getBlockState(pos).isAir()) world.setBlock(pos, leaf, Block.UPDATE_ALL);
        }
        return true;
    }

    /** The template's log at y 0 nearest the middle of its footprint, which stands on the ground. */
    private static BlockPos trunk(StructureTemplate template, List<StructureTemplate.StructureBlockInfo> logs) {
        double cx = (template.getSize().getX() - 1) / 2.0;
        double cz = (template.getSize().getZ() - 1) / 2.0;
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (StructureTemplate.StructureBlockInfo info : logs) {
            if (info.pos().getY() != 0) continue;
            double dx = info.pos().getX() - cx;
            double dz = info.pos().getZ() - cz;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = info.pos();
            }
        }
        return best;
    }

    /** A template position, relative to its trunk, mirrored, turned by quarter turns and moved to origin. */
    private static BlockPos at(BlockPos origin, BlockPos rel, BlockPos trunk, boolean mirror, int turn) {
        int dx = rel.getX() - trunk.getX();
        int dz = rel.getZ() - trunk.getZ();
        if (mirror) dx = -dx;
        for (int i = 0; i < turn; i++) {
            int t = dx;
            dx = -dz;
            dz = t;
        }
        return origin.offset(dx, rel.getY(), dz);
    }

    private static boolean takesLog(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.canBeReplaced() || state.is(BlockTags.LEAVES);
    }

    private boolean hasNearbyLog(WorldGenLevel world, BlockPos origin, Block log) {
        for (int dx = -spacing; dx <= spacing; dx++) {
            for (int dz = -spacing; dz <= spacing; dz++) {
                if (dx * dx + dz * dz > spacing * spacing) continue;
                for (int dy = -1; dy <= 22; dy++) {
                    if (world.getBlockState(origin.offset(dx, dy, dz)).is(log)) return true;
                }
            }
        }
        return false;
    }
}
