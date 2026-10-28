package com.apeira.main.farming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
 *
 * <p>Sprinkler waves: a sprinkler that starts watering (filled while dry, or each morning it has water) does not wet its
 * whole square at once. Its {@link Wave} spreads one ring a {@link #RING_TICKS} ticks, as farmland hydrates, the boxes at
 * Chebyshev distance 1 first, then 2, then 3, each with a splash; a box shows wet ({@link #sprinklerReaches}) once its
 * ring is reached. Waves are only the look: growth reads {@link FarmClock#sprinklerWaters}, so a box's crop grows the
 * same whether the wave reached it before a restart or not, and waves are not saved.
 *
 * <p>Rain ({@link #rain}): every {@link #RAIN_PLANTER_TICKS} ticks, each loaded planter box that rain falls on
 * ({@link Level#isRainingAt} the block above it: open sky, a raining biome) is watered for the day, as a can would; every
 * {@link #RAIN_SPRINKLER_TICKS} ticks each loaded sprinkler that rain falls on has a {@link #RAIN_SPRINKLER_CHANCE}
 * chance to gain a day of water, up to {@link SprinklerBlock#CAPACITY}.
 */
public final class FarmingService {
    private static final Set<PlanterBoxBlockEntity> PLANTERS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<PlanterBoxBlockEntity> PENDING = Collections.newSetFromMap(new WeakHashMap<>());
    private static final List<SprinklerRefresh> PENDING_SPRINKLERS = new ArrayList<>();

    /** Ticks between a sprinkler wave's rings. */
    private static final int RING_TICKS = 10;
    /** Ticks between rain waterings of planter boxes. */
    private static final int RAIN_PLANTER_TICKS = 100;
    /** Ticks between rain refills of sprinklers, and the chance of a day's water at each. */
    private static final int RAIN_SPRINKLER_TICKS = 1200;
    private static final float RAIN_SPRINKLER_CHANCE = 0.2F;
    private static final Map<SprinklerRefresh, Wave> WAVES = new HashMap<>();

    private record SprinklerRefresh(ResourceKey<Level> dimension, BlockPos pos) {}

    /** A sprinkler's spreading water: the rings reached so far, of {@code radius}, since {@code startTick}. */
    private static final class Wave {
        final int radius;
        final int startTick;
        int ring;

        Wave(int radius, int startTick) {
            this.radius = radius;
            this.startTick = startTick;
        }
    }

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
            clock.allSprinklers().forEach((dimension, sprinklers) -> sprinklers.values().forEach(sprinkler -> {
                PENDING_SPRINKLERS.add(new SprinklerRefresh(dimension, sprinkler.pos()));
                ServerLevel level = server.getLevel(dimension);
                if (level != null && sprinkler.waters(clock.day()) && level.isLoaded(sprinkler.pos())) {
                    startWave(level, sprinkler.pos(), sprinkler.radius());
                }
            }));
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
        spreadWaves(server);
        rain(server, clock);
    }

    /** Starts a sprinkler's wave from its first ring; the boxes it reaches show dry until their ring comes. */
    static void startWave(ServerLevel level, BlockPos pos, int radius) {
        WAVES.put(new SprinklerRefresh(level.dimension(), pos.immutable()), new Wave(radius, level.getServer().getTickCount()));
    }

    /** Moves each wave on to the ring it has reached by now, wetting and splashing that ring's boxes. */
    private static void spreadWaves(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<Map.Entry<SprinklerRefresh, Wave>> waves = WAVES.entrySet().iterator();
        while (waves.hasNext()) {
            Map.Entry<SprinklerRefresh, Wave> entry = waves.next();
            Wave wave = entry.getValue();
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            BlockPos center = entry.getKey().pos();
            if (level == null || !level.isLoaded(center)) {
                waves.remove();
                continue;
            }
            int target = Math.min(wave.radius, (now - wave.startTick) / RING_TICKS + 1);
            while (wave.ring < target) {
                wave.ring++;
                wetRing(level, center, wave.ring);
            }
            if (wave.ring >= wave.radius) {
                waves.remove();
            }
        }
    }

    /** Refreshes the loaded boxes at Chebyshev distance {@code ring} from a sprinkler, one block up or down too. */
    private static void wetRing(ServerLevel level, BlockPos center, int ring) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-ring, -1, -ring), center.offset(ring, 1, ring))) {
            if (Math.max(Math.abs(pos.getX() - center.getX()), Math.abs(pos.getZ() - center.getZ())) == ring && level.isLoaded(pos)
                    && level.getBlockEntity(pos) instanceof PlanterBoxBlockEntity planter) {
                planter.refreshState();
                level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 6, 0.3, 0.0, 0.3, 0.0);
            }
        }
    }

    /** Whether a watering sprinkler covers {@code pos} on {@code day} and its wave, if one is spreading, has reached it. */
    static boolean sprinklerReaches(ServerLevel level, BlockPos pos, long day) {
        for (FarmClock.Sprinkler sprinkler : FarmClock.get(level.getServer()).sprinklers(level.dimension())) {
            if (sprinkler.waters(day) && sprinkler.covers(pos)) {
                Wave wave = WAVES.get(new SprinklerRefresh(level.dimension(), sprinkler.pos()));
                int ring = Math.max(Math.abs(pos.getX() - sprinkler.pos().getX()), Math.abs(pos.getZ() - sprinkler.pos().getZ()));
                if (wave == null || ring <= wave.ring) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Rain waters the loaded planter boxes it falls on and now and then adds a day to the sprinklers it falls on. */
    private static void rain(MinecraftServer server, FarmClock clock) {
        int now = server.getTickCount();
        if (now % RAIN_PLANTER_TICKS == 0) {
            for (PlanterBoxBlockEntity planter : new ArrayList<>(PLANTERS)) {
                if (!planter.isRemoved() && planter.getLevel() instanceof ServerLevel level && level.isRaining()
                        && level.isRainingAt(planter.getBlockPos().above())) {
                    planter.rain(level);
                }
            }
        }
        if (now % RAIN_SPRINKLER_TICKS == 0) {
            clock.allSprinklers().forEach((dimension, sprinklers) -> {
                ServerLevel level = server.getLevel(dimension);
                if (level == null || !level.isRaining()) {
                    return;
                }
                for (FarmClock.Sprinkler sprinkler : List.copyOf(sprinklers.values())) {
                    if (level.isLoaded(sprinkler.pos()) && level.isRainingAt(sprinkler.pos().above())
                            && sprinkler.waterLeft(clock.day()) < SprinklerBlock.CAPACITY && level.getRandom().nextFloat() < RAIN_SPRINKLER_CHANCE) {
                        setSprinklerWater(level, sprinkler.pos(), sprinkler.waterLeft(clock.day()) + 1, sprinkler.radius());
                    }
                }
            });
        }
    }

    /**
     * Sets a sprinkler's water to {@code days} from today (at most {@link SprinklerBlock#CAPACITY}) and shows it; a
     * sprinkler that was dry starts its wave.
     */
    static void setSprinklerWater(ServerLevel level, BlockPos pos, int days, int radius) {
        FarmClock clock = FarmClock.get(level.getServer());
        long day = clock.day();
        FarmClock.Sprinkler current = clock.sprinkler(level.dimension(), pos);
        boolean wasWatering = current != null && current.waters(day);
        int water = Math.min(SprinklerBlock.CAPACITY, Math.max(0, days));
        clock.setSprinkler(new FarmClock.Sprinkler(level.dimension(), pos, day, water, radius));
        refreshSprinkler(level, pos, clock);
        if (water > 0 && !wasWatering) {
            startWave(level, pos, radius);
        } else if (water == 0) {
            refreshPlantersAround(level, pos, radius);
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
        WAVES.clear();
    }
}
