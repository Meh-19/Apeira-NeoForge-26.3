package com.apeira.main.realm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.Apeira;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The pre-generated realm pool. The server keeps {@link #READY_PER_PRESET} realm dimensions of each {@link RealmPreset}
 * made and generated ahead of time, so a player who picks a region in the onboarding screen is given one at once
 * ({@link #claim}); the claimed slot is refilled straight away with a new dimension that generates in the background.
 * <p>
 * Generation runs on the chunk workers: each pool dimension loads its border area a 5x5-chunk batch at a time through
 * a loading ticket, and the server tick only checks whether the batch is done. Realm dimensions, owned or pooled, are
 * recreated from {@link RealmData} when the server starts, before players join.
 */
public final class RealmPool {
    public static final int READY_PER_PRESET = 1;
    /** Ticks from claiming a realm to arriving in it: the "Realm Created" then "Traveling Home..." cards. */
    private static final int TRAVEL_DELAY = 60;
    private static final int BATCH_RADIUS = 2;
    private static final List<ChunkPos> BATCHES = batches();

    private static final List<Pregeneration> GENERATING = new ArrayList<>();
    private static final Queue<RealmPreset> REFILLS = new ArrayDeque<>();
    private static final Map<UUID, Integer> TRAVELLERS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private RealmPool() {}

    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        RealmData data = RealmData.get(server);
        data.owned().forEach(realm -> RealmLevels.create(server, realm));
        for (RealmData.Realm realm : data.pool()) {
            ServerLevel level = RealmLevels.create(server, realm);
            if (!realm.generated()) {
                GENERATING.add(new Pregeneration(level, realm));
            }
        }
        for (RealmPreset preset : RealmPreset.values()) {
            for (long count = data.poolCount(preset); count < READY_PER_PRESET; count++) {
                REFILLS.add(preset);
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        GENERATING.clear();
        REFILLS.clear();
        TRAVELLERS.clear();
    }

    /** Players without a realm are shown the onboarding screen when they join. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && RealmData.get(player.level().getServer()).realmOf(player.getUUID()).isEmpty()) {
            ApeiraPackets.sendToPlayer(player, new OpenOnboardingPayload());
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // New dimensions are added here, after the levels have ticked, never while the server iterates them.
        RealmPreset refill;
        while ((refill = REFILLS.poll()) != null) {
            fill(server, refill);
        }
        Iterator<Pregeneration> generating = GENERATING.iterator();
        while (generating.hasNext()) {
            Pregeneration job = generating.next();
            if (job.advance()) {
                RealmData.get(server).updatePool(job.realm.withGenerated(surface(job.level)));
                generating.remove();
                Apeira.LOGGER.info("Realm {} ({}) is generated and ready", job.realm.dimension(), job.realm.preset().getSerializedName());
            }
        }
        TRAVELLERS.replaceAll((uuid, ticks) -> ticks - 1);
        TRAVELLERS.entrySet().removeIf(entry -> {
            if (entry.getValue() > 0) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                sendHome(player);
            }
            return true;
        });
    }

    /**
     * Gives {@code player} a realm of {@code preset}: a ready pool realm if there is one, otherwise a new one that
     * generates as they arrive. A player who already has a realm is only sent home. Either way they travel there after
     * the onboarding screen's cards.
     */
    public static void claim(ServerPlayer player, RealmPreset preset) {
        MinecraftServer server = player.level().getServer();
        if (RealmData.get(server).realmOf(player.getUUID()).isEmpty()) {
            RealmData.Realm realm = take(server, player.getUUID(), preset);
            Apeira.LOGGER.info("{} claimed realm {} ({})", player.getName().getString(), realm.dimension(), preset.getSerializedName());
        }
        ApeiraPackets.sendToPlayer(player, new RealmCreatedPayload());
        TRAVELLERS.put(player.getUUID(), TRAVEL_DELAY);
    }

    /**
     * Gives {@code owner} a realm of {@code preset}, in place of any they had: a pool realm, a generated one first, or a
     * new one that generates as they arrive. The pool slot is refilled. Used by {@link #claim} and by realm resets.
     */
    static RealmData.Realm take(MinecraftServer server, UUID owner, RealmPreset preset) {
        RealmData data = RealmData.get(server);
        RealmData.Realm realm = data.claim(owner, preset).orElseGet(() -> {
            RealmData.Realm fresh = newRealm(server, preset);
            RealmLevels.create(server, fresh);
            data.own(owner, fresh);
            return fresh;
        });
        // A claimed realm still generating keeps loading only as the player walks it.
        GENERATING.removeIf(job -> job.cancelIfFor(realm));
        REFILLS.add(preset);
        return realm;
    }

    /** Teleports {@code player} to their realm's spawn. Returns false when they have none. */
    public static boolean sendHome(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        RealmData.Realm realm = RealmData.get(server).realmOf(player.getUUID()).orElse(null);
        if (realm == null) {
            return false;
        }
        ServerLevel level = RealmLevels.create(server, realm);
        RealmAccess access = RealmData.get(server).access(player.getUUID());
        player.teleport(arrival(level, spawnOf(level, realm, access, player.getDirection()), access));
        player.sendSystemMessage(Component.translatable("message.apeira.realm.home"));
        return true;
    }

    /**
     * Where players arrive in {@code realm}: the spawn its owner set (the starter balloon's floor), else the top block at
     * 0, 0. The first arrival builds the starter platform and balloon at the origin ({@link StarterBalloon#build}), which
     * becomes the spawn unless the owner had already set one; it faces {@code facing}, the arriving player's way, which
     * becomes the realm's spawn facing.
     */
    static BlockPos spawnOf(ServerLevel level, RealmData.Realm realm, RealmAccess access, Direction facing) {
        if (!access.starterBuilt) {
            access.starterBuilt = true;
            access.spawnFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
            BlockPos balloon = StarterBalloon.build(level, realm.preset(), access.spawnFacing);
            access.balloon = Optional.of(balloon);
            if (access.spawn.isEmpty()) {
                access.spawn = Optional.of(balloon.above());
            }
            access.balloonPaint.ifPresent(paint -> StarterBalloon.paint(level, balloon, paint));
            RealmData.get(level.getServer()).setDirty();
        }
        return access.spawn().orElseGet(() -> realm.generated() ? realm.spawn() : surface(level));
    }

    /**
     * The teleport to a realm's spawn: on its block, looking straight ahead along the realm's spawn facing, whatever way
     * the player looked before.
     */
    static TeleportTransition arrival(ServerLevel level, BlockPos spawn, RealmAccess access) {
        return new TeleportTransition(level, Vec3.atBottomCenterOf(spawn), Vec3.ZERO, access.spawnFacing.toYRot(), 0.0F,
                TeleportTransition.DO_NOTHING);
    }

    private static void fill(MinecraftServer server, RealmPreset preset) {
        RealmData.Realm realm = newRealm(server, preset);
        RealmData.get(server).addToPool(realm);
        GENERATING.add(new Pregeneration(RealmLevels.create(server, realm), realm));
    }

    static RealmData.Realm newRealm(MinecraftServer server, RealmPreset preset) {
        String id = "realm/" + preset.getSerializedName() + "_" + Long.toHexString(RANDOM.nextLong() & 0xFFFFFFFFFFFFL);
        return new RealmData.Realm(ApeiraIds.id(id), preset, RealmLevels.landSeed(server, RANDOM), false, BlockPos.ZERO);
    }

    /** Where players arrive: the top block at 0, 0, generating the chunk if it is not yet. */
    static BlockPos surface(ServerLevel level) {
        level.getChunk(0, 0);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        return new BlockPos(0, Math.max(y, level.getSeaLevel() + 1), 0);
    }

    /** Chunk centres whose radius-2 squares cover every chunk inside the border. */
    private static List<ChunkPos> batches() {
        int min = Math.floorDiv(-RealmLevels.BORDER_SIZE / 2, 16);
        int max = Math.floorDiv(RealmLevels.BORDER_SIZE / 2 - 1, 16);
        int step = BATCH_RADIUS * 2 + 1;
        List<ChunkPos> centres = new ArrayList<>();
        for (int x = min + BATCH_RADIUS; x - BATCH_RADIUS <= max; x += step) {
            for (int z = min + BATCH_RADIUS; z - BATCH_RADIUS <= max; z += step) {
                centres.add(new ChunkPos(x, z));
            }
        }
        return centres;
    }

    /** Loads one realm's border area batch by batch, letting each batch go once it is generated. */
    private static final class Pregeneration {
        private final ServerLevel level;
        private final RealmData.Realm realm;
        private int batch = -1;
        private @Nullable CompletableFuture<?> loading;

        Pregeneration(ServerLevel level, RealmData.Realm realm) {
            this.level = level;
            this.realm = realm;
        }

        /** Moves on when the current batch is loaded; true once every batch is. */
        boolean advance() {
            if (loading != null && !loading.isDone()) {
                return false;
            }
            release();
            batch++;
            if (batch >= BATCHES.size()) {
                return true;
            }
            loading = level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_LOADING, BATCHES.get(batch), BATCH_RADIUS);
            return false;
        }

        boolean cancelIfFor(RealmData.Realm claimed) {
            if (!realm.dimension().equals(claimed.dimension())) {
                return false;
            }
            release();
            return true;
        }

        private void release() {
            if (loading != null) {
                level.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_LOADING, BATCHES.get(batch), BATCH_RADIUS);
                loading = null;
            }
        }
    }
}
