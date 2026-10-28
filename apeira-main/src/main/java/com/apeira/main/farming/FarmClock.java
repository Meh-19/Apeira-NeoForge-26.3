package com.apeira.main.farming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The server's farming clock and its sprinklers (overworld saved data {@code apeira:farm_clock}).
 *
 * <p>{@link #day()} counts the 7:00 AM ticks: it is the number of the last morning that has passed on the overworld
 * clock, which every dimension shares (day time 0 is 6:00 AM, so 7:00 AM is 1000). Planter boxes remember the last
 * morning they grew for and catch up on the ones they missed while unloaded, so crops grow whether or not anyone is
 * near or online.
 *
 * <p>A sprinkler's water is worked out from the clock too, so sprinklers need no ticking: filled on day {@code fillDay}
 * with {@code days} days of water, it waters its area on the days {@code fillDay} to {@code fillDay + days - 1}.
 */
public final class FarmClock extends SavedData {
    /** No morning counted yet: the first tick sets the clock to the current morning without growing anything. */
    private static final long UNSET = Long.MIN_VALUE;

    /** A registered sprinkler. */
    public record Sprinkler(ResourceKey<Level> dimension, BlockPos pos, long fillDay, int days, int radius) {
        static final Codec<Sprinkler> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Sprinkler::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(Sprinkler::pos),
                Codec.LONG.fieldOf("fill_day").forGetter(Sprinkler::fillDay),
                Codec.INT.fieldOf("days").forGetter(Sprinkler::days),
                Codec.INT.fieldOf("radius").forGetter(Sprinkler::radius)).apply(instance, Sprinkler::new));

        /** Days of water left on {@code day}: {@code days} on the day it was filled, one less each morning after. */
        public int waterLeft(long day) {
            return (int) Math.max(0L, Math.min(days, fillDay + days - day));
        }

        /** Whether the sprinkler waters on {@code day}. */
        public boolean waters(long day) {
            return day >= fillDay && day < fillDay + days;
        }

        /** Whether {@code target} is in the sprinkler's square (its own block excluded), one block up or down. */
        public boolean covers(BlockPos target) {
            int dx = Math.abs(target.getX() - pos.getX());
            int dz = Math.abs(target.getZ() - pos.getZ());
            return dx <= radius && dz <= radius && Math.abs(target.getY() - pos.getY()) <= 1 && (dx != 0 || dz != 0);
        }
    }

    private static final Codec<FarmClock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("day", UNSET).forGetter(clock -> clock.day),
            Sprinkler.CODEC.listOf().optionalFieldOf("sprinklers", List.of()).forGetter(FarmClock::sprinklers))
            .apply(instance, FarmClock::new));
    public static final SavedDataType<FarmClock> TYPE = new SavedDataType<>(ApeiraIds.id("farm_clock"), FarmClock::new, CODEC);

    private long day = UNSET;
    private final Map<ResourceKey<Level>, Map<BlockPos, Sprinkler>> sprinklers = new HashMap<>();

    private FarmClock() {}

    private FarmClock(long day, List<Sprinkler> sprinklers) {
        this.day = day;
        sprinklers.forEach(this::putSprinkler);
    }

    public static FarmClock get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The morning from the overworld clock's time: 0 at the first 7:00 AM, one more at each one after. */
    public static long morningOf(long clockTime) {
        return Math.floorDiv(clockTime - 1000L, 24000L);
    }

    /** The last morning counted; before the first tick, the current one. */
    public long day() {
        return day;
    }

    /**
     * Moves the clock to {@code morning}. Returns how many mornings passed (0 when none did, the first time, or when the
     * time was set back).
     */
    long advanceTo(long morning) {
        if (day == UNSET || morning <= day) {
            if (day != morning) {
                day = morning;
                setDirty();
            }
            return 0L;
        }
        long passed = morning - day;
        day = morning;
        setDirty();
        return passed;
    }

    private List<Sprinkler> sprinklers() {
        List<Sprinkler> all = new ArrayList<>();
        sprinklers.values().forEach(map -> all.addAll(map.values()));
        return all;
    }

    private void putSprinkler(Sprinkler sprinkler) {
        sprinklers.computeIfAbsent(sprinkler.dimension(), key -> new HashMap<>()).put(sprinkler.pos(), sprinkler);
    }

    public @Nullable Sprinkler sprinkler(ResourceKey<Level> dimension, BlockPos pos) {
        Map<BlockPos, Sprinkler> map = sprinklers.get(dimension);
        return map == null ? null : map.get(pos);
    }

    /** The sprinklers of one dimension. */
    public Iterable<Sprinkler> sprinklers(ResourceKey<Level> dimension) {
        Map<BlockPos, Sprinkler> map = sprinklers.get(dimension);
        return map == null ? List.of() : List.copyOf(map.values());
    }

    public Map<ResourceKey<Level>, Map<BlockPos, Sprinkler>> allSprinklers() {
        return sprinklers;
    }

    void setSprinkler(Sprinkler sprinkler) {
        putSprinkler(new Sprinkler(sprinkler.dimension(), sprinkler.pos().immutable(), sprinkler.fillDay(), sprinkler.days(), sprinkler.radius()));
        setDirty();
    }

    void removeSprinkler(ResourceKey<Level> dimension, BlockPos pos) {
        Map<BlockPos, Sprinkler> map = sprinklers.get(dimension);
        if (map != null && map.remove(pos) != null) {
            setDirty();
        }
    }

    /** Whether a sprinkler waters {@code pos} on {@code day}. */
    public boolean sprinklerWaters(ResourceKey<Level> dimension, BlockPos pos, long day) {
        Map<BlockPos, Sprinkler> map = sprinklers.get(dimension);
        if (map == null) {
            return false;
        }
        for (Sprinkler sprinkler : map.values()) {
            if (sprinkler.waters(day) && sprinkler.covers(pos)) {
                return true;
            }
        }
        return false;
    }
}
