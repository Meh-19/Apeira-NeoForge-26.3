package com.apeira.main.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.farming.FarmClock;
import com.apeira.main.realm.ChatIcons;
import com.apeira.main.registry.MainNpcs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Jacko the farmer's arrival (overworld saved data {@code apeira:jacko_arrival}). The first purchase from Jeckyl in a
 * realm ({@link #onPurchase}) schedules it for the next farming morning, the next 7:00 AM ({@link FarmClock#day}). From that
 * morning on, a check every {@link #CHECK_TICKS} ticks ({@link #onServerTick}) tells the buyer, once they are online, and
 * everyone in that realm "ꑠ Jacko the farmer has arrived!" in {@link #COLOR}, and puts Jacko ({@link JackoEntity}) on the
 * ground near where Jeckyl stood once that spot's chunk is loaded. Each realm gets one arrival.
 */
public final class JackoArrival extends SavedData {
    /** Origin Realms' golden orange. */
    public static final int COLOR = 0xF49E0B;
    private static final int CHECK_TICKS = 20;

    /** A realm's scheduled arrival: who bought, near where, from which farming day, and what has happened yet. */
    record Arrival(ResourceKey<Level> dimension, BlockPos near, UUID buyer, long day, boolean announced, boolean spawned) {
        static final Codec<Arrival> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Arrival::dimension),
                BlockPos.CODEC.fieldOf("near").forGetter(Arrival::near),
                UUIDUtil.CODEC.fieldOf("buyer").forGetter(Arrival::buyer),
                Codec.LONG.fieldOf("day").forGetter(Arrival::day),
                Codec.BOOL.optionalFieldOf("announced", false).forGetter(Arrival::announced),
                Codec.BOOL.optionalFieldOf("spawned", false).forGetter(Arrival::spawned)).apply(instance, Arrival::new));
    }

    private static final Codec<JackoArrival> CODEC = Arrival.CODEC.listOf().optionalFieldOf("arrivals", List.of())
            .xmap(JackoArrival::new, data -> new ArrayList<>(data.arrivals.values())).codec();
    public static final SavedDataType<JackoArrival> TYPE = new SavedDataType<>(ApeiraIds.id("jacko_arrival"), JackoArrival::new, CODEC);

    private final Map<ResourceKey<Level>, Arrival> arrivals = new HashMap<>();

    private JackoArrival() {}

    private JackoArrival(List<Arrival> arrivals) {
        arrivals.forEach(arrival -> this.arrivals.put(arrival.dimension(), arrival));
    }

    public static JackoArrival get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** A purchase from {@code npc}: the first from Jeckyl in its level schedules Jacko for the next 7:00 AM. */
    static void onPurchase(ServerPlayer player, NpcEntity npc) {
        if (!(npc instanceof JeckylEntity) || !(npc.level() instanceof ServerLevel level)) {
            return;
        }
        JackoArrival data = get(level.getServer());
        if (data.arrivals.containsKey(level.dimension())) {
            return;
        }
        long day = FarmClock.get(level.getServer()).day() + 1;
        data.arrivals.put(level.dimension(), new Arrival(level.dimension(), npc.blockPosition(), player.getUUID(), day, false, false));
        data.setDirty();
    }

    /** "ꑠ Jacko the farmer has arrived!", the icon white and the text golden orange. */
    public static MutableComponent message() {
        return Component.empty().append(ChatIcons.icon(ChatIcons.SUCCESS)).append(Component.literal(" "))
                .append(Component.translatable("message.apeira.npc.jacko_arrived").withStyle(style -> style.withColor(TextColor.fromRgb(COLOR))));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_TICKS != 0) {
            return;
        }
        JackoArrival data = get(server);
        if (data.arrivals.isEmpty()) {
            return;
        }
        long today = FarmClock.get(server).day();
        for (Arrival arrival : List.copyOf(data.arrivals.values())) {
            if (arrival.day() > today || arrival.announced() && arrival.spawned()) {
                continue;
            }
            Arrival updated = arrival;
            ServerLevel level = server.getLevel(arrival.dimension());
            ServerPlayer buyer = server.getPlayerList().getPlayer(arrival.buyer());
            if (!arrival.announced() && buyer != null) {
                buyer.sendSystemMessage(message());
                if (level != null) {
                    level.players().stream().filter(player -> player != buyer).forEach(player -> player.sendSystemMessage(message()));
                }
                updated = new Arrival(updated.dimension(), updated.near(), updated.buyer(), updated.day(), true, updated.spawned());
            }
            if (!arrival.spawned() && level != null && level.isLoaded(arrival.near()) && spawn(level, arrival.near())) {
                updated = new Arrival(updated.dimension(), updated.near(), updated.buyer(), updated.day(), updated.announced(), true);
            }
            if (updated != arrival) {
                data.arrivals.put(arrival.dimension(), updated);
                data.setDirty();
            }
        }
    }

    /** Puts Jacko on the ground near {@code near}, facing it. False when there is no room yet. */
    private static boolean spawn(ServerLevel level, BlockPos near) {
        BlockPos spot = NpcService.surfaceNear(level, MainNpcs.JACKO.get(), near);
        if (spot == null) {
            return false;
        }
        JackoEntity jacko = MainNpcs.JACKO.get().create(level, EntitySpawnReason.EVENT);
        if (jacko == null) {
            return false;
        }
        jacko.snapTo(Vec3.atBottomCenterOf(spot));
        jacko.face(Vec3.atBottomCenterOf(near));
        return level.addFreshEntity(jacko);
    }
}
