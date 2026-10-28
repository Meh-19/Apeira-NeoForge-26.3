package com.apeira.main.farming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The server-wide 7:00 AM growth tick.
 *
 * <p>Every server tick ({@link #onServerTick}) reads the overworld clock, which every dimension shares; when it has
 * passed 7:00 AM ({@link FarmClock#morningOf}) since the last tick, the {@link FarmClock} moves on, every loaded
 * planter box catches up ({@link PlanterBoxBlockEntity#catchUp}: one stage per watered morning) and every loaded
 * sprinkler shows its new water level. Boxes and sprinklers in unloaded chunks catch up when they load, through the
 * same calls, so growth never depends on a player being near or online.
 *
 * <p>Loaded planter boxes register here from {@code onLoad} and leave on removal or chunk unload; work for a box or
 * sprinkler that has just loaded is queued for the next tick, as the chunk is still loading when they register.
 */
public final class FarmingService {
    private static final Set<PlanterBoxBlockEntity> PLANTERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<PlanterBoxBlockEntity> PENDING = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<SprinklerRefresh> PENDING_SPRINKLERS = new ArrayList<>();

    private record SprinklerRefresh(ResourceKey<Level> dimension, BlockPos pos) {}

    private FarmingService() {}

    /** The farming day of the server {@code level} belongs to (the last 7:00 AM counted). */
    public static long day(ServerLevel level) {
        return FarmClock.get(level.getServer()).day();
    }

    static void track(PlanterBoxBlockEntity planter) {
        PLANTERS.add(planter);
        PENDING.add(planter);
    }

    static void untrack(PlanterBoxBlockEntity planter) {
        PLANTERS.remove(planter);
        PENDING.remove(planter);
    }

    /** Shows a sprinkler's water level once its chunk has finished loading. */
    static void queueSprinkler(ServerLevel level, BlockPos pos) {
        PENDING_SPRINKLERS.add(new SprinklerRefresh(level.dimension(), pos.immutable()));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        FarmClock clock = FarmClock.get(server);
        long passed = clock.advanceTo(FarmClock.morningOf(server.overworld().getOverworldClockTime()));
        if (passed > 0) {
            PENDING.addAll(PLANTERS);
            clock.allSprinklers().forEach((dimension, sprinklers) -> sprinklers.keySet()
                    .forEach(pos -> PENDING_SPRINKLERS.add(new SprinklerRefresh(dimension, pos))));
        }
        if (!PENDING.isEmpty()) {
            List<PlanterBoxBlockEntity> planters = new ArrayList<>(PENDING);
            PENDING.clear();
            for (PlanterBoxBlockEntity planter : planters) {
                if (!planter.isRemoved() && planter.getLevel() instanceof ServerLevel level) {
                    planter.catchUp(level);
                }
            }
        }
        if (!PENDING_SPRINKLERS.isEmpty()) {
            List<SprinklerRefresh> sprinklers = new ArrayList<>(PENDING_SPRINKLERS);
            PENDING_SPRINKLERS.clear();
            for (SprinklerRefresh refresh : sprinklers) {
                ServerLevel level = server.getLevel(refresh.dimension());
                if (level != null && level.isLoaded(refresh.pos())) {
                    refreshSprinkler(level, refresh.pos(), clock);
                }
            }
        }
    }

    /** Sets a sprinkler's {@link SprinklerBlock#LEVEL} to its water left today. */
    static void refreshSprinkler(ServerLevel level, BlockPos pos, FarmClock clock) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SprinklerBlock)) {
            clock.removeSprinkler(level.dimension(), pos);
            return;
        }
        FarmClock.Sprinkler sprinkler = clock.sprinkler(level.dimension(), pos);
        int water = sprinkler == null ? 0 : Math.min(SprinklerBlock.MAX_LEVEL, sprinkler.waterLeft(clock.day()));
        if (state.getValue(SprinklerBlock.LEVEL) != water) {
            level.setBlock(pos, state.setValue(SprinklerBlock.LEVEL, water), Block.UPDATE_CLIENTS);
        }
    }

    /** Updates the wet look of the loaded planter boxes a sprinkler reaches, after it was filled, placed or removed. */
    static void refreshPlantersAround(ServerLevel level, BlockPos sprinkler, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(sprinkler.offset(-radius, -1, -radius), sprinkler.offset(radius, 1, radius))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity planter) {
                planter.refreshState();
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PLANTERS.clear();
        PENDING.clear();
        PENDING_SPRINKLERS.clear();
    }
}
