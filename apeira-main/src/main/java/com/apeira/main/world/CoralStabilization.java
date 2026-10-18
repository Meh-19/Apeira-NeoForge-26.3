package com.apeira.main.world;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainTags;
import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Xenon's coral support: a living coral does not die out of water while it touches a block of
 * {@code #apeira:keeps_coral_alive} (dolomite and its shapes) or has been given dolomite dust, which this saved data
 * remembers for each dimension. The coral mixins ({@code com.apeira.main.mixin.Coral*Mixin}) ask {@link #keepsAlive}
 * before a coral's death check.
 */
public final class CoralStabilization extends SavedData {
    private static final Codec<CoralStabilization> CODEC = BlockPos.CODEC.listOf()
            .xmap(CoralStabilization::new, stabilization -> List.copyOf(stabilization.positions));
    public static final SavedDataType<CoralStabilization> TYPE = new SavedDataType<>(ApeiraIds.id("coral_stabilization"),
            CoralStabilization::new, CODEC);

    private final Set<BlockPos> positions = new HashSet<>();

    private CoralStabilization() {}

    private CoralStabilization(List<BlockPos> positions) {
        this.positions.addAll(positions);
    }

    public static CoralStabilization get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isStabilized(BlockPos pos) {
        return positions.contains(pos);
    }

    public void add(BlockPos pos) {
        if (positions.add(pos.immutable())) {
            setDirty();
        }
    }

    /** Whether a living coral, coral fan or coral block. */
    public static boolean isLivingCoral(BlockState state) {
        return state.is(BlockTags.CORALS) || state.is(BlockTags.WALL_CORALS) || state.is(BlockTags.CORAL_BLOCKS);
    }

    /** Whether the coral at {@code pos} stays alive whatever water there is: dusted, or touching dolomite. */
    public static boolean keepsAlive(ServerLevel level, BlockPos pos) {
        if (get(level).isStabilized(pos)) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(MainTags.KEEPS_CORAL_ALIVE)) {
                return true;
            }
        }
        return false;
    }
}
