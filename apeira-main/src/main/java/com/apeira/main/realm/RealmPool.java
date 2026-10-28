package com.apeira.main.realm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.apeira.main.npc.NpcService;
import com.apeira.main.realm.balloon.BalloonColors;
import com.apeira.main.realm.balloon.StarterBalloon;
import com.apeira.main.realm.balloon.StarterBalloonBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
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
 * a loading ticket, and the server tick only checks whether the batch is done. Only one pool realm generates at a time,
 * and none while a player is on the way to a realm or has just arrived ({@link #QUIET_TICKS}), so the chunk workers
 * and the server thread are free for the chunks that player sees. The last step loads the origin's chunks
 * ({@link #ORIGIN_RADIUS}) and reads the spawn while they are still loaded. A player claiming a realm has the same
 * origin chunks loaded in the background during the onboarding cards ({@link #claim}), so the starter balloon's
 * flat-ground search ({@link StarterBalloon#build}) and the arrival read loaded chunks instead of loading 81 at once on
 * the server thread. Realm dimensions, owned or pooled, are recreated from {@link RealmData} when the server starts,
 * before players join.
 */
public final class RealmPool {
    public static final int READY_PER_PRESET = 1;
    /** Ticks from claiming a realm to arriving in it: the "Realm Created" then "Traveling Home..." cards. */
    private static final int TRAVEL_DELAY = 60;
    private static final int BATCH_RADIUS = 2;
    /**
     * Chunks around 0, 0 loaded before a realm is first visited: the starter balloon's flat-ground search reaches 50
     * blocks from the origin, chunks -4 to 3.
     */
    private static final int ORIGIN_RADIUS = 4;
    private static final ChunkPos ORIGIN = new ChunkPos(0, 0);
    /** Ticks after a player arrives in a realm during which pool generation waits. */
    private static final int QUIET_TICKS = 200;
    private static final List<ChunkPos> BATCHES = batches();

    private static final List<Pregeneration> GENERATING = new ArrayList<>();
    private static final Queue<RealmPreset> REFILLS = new ArrayDeque<>();
    private static final Map<UUID, Integer> TRAVELLERS = new HashMap<>();
    /** The realm whose origin chunks each travelling player has loading, released when they arrive. */
    private static final Map<UUID, ServerLevel> PRELOADS = new HashMap<>();
    private static int quiet;
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
        PRELOADS.clear();
        quiet = 0;
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
        if (quiet > 0) {
            quiet--;
        }
        // Nothing new starts while anybody is travelling to or settling into a realm: no new pool dimension, at most
        // one a tick, and no new batch, one realm at a time (a batch already loading finishes on the workers).
        boolean calm = TRAVELLERS.isEmpty() && quiet == 0;
        RealmPreset refill = calm ? REFILLS.poll() : null;
        if (refill != null) {
            fill(server, refill);
        }
        if (!GENERATING.isEmpty() && calm) {
            Pregeneration job = GENERATING.getFirst();
            if (job.advance()) {
                // The origin's chunks are still loaded here, so reading the spawn loads nothing.
                RealmData.get(server).updatePool(job.realm.withGenerated(surface(job.level)));
                job.release();
                GENERATING.removeFirst();
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
            // The player's own chunk loading holds the area now.
            ServerLevel preloaded = PRELOADS.remove(entry.getKey());
            if (preloaded != null) {
                preloaded.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_LOADING, ORIGIN, ORIGIN_RADIUS);
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
        RealmData.get(server).realmOf(player.getUUID()).ifPresent(realm -> preload(player.getUUID(), RealmLevels.create(server, realm)));
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

    /**
     * Starts loading {@code level}'s origin chunks on the chunk workers while {@code player} watches the onboarding
     * cards, so their arrival (and the starter balloon's first build) finds them loaded.
     */
    private static void preload(UUID player, ServerLevel level) {
        ServerLevel previous = PRELOADS.put(player, level);
        if (previous != null) {
            previous.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_LOADING, ORIGIN, ORIGIN_RADIUS);
        }
        level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_LOADING, ORIGIN, ORIGIN_RADIUS);
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
        quiet = QUIET_TICKS;
        return true;
    }

    /**
     * Where players arrive in {@code realm}: the spawn its owner set (the starter balloon's floor), else the top block at
     * 0, 0. The first arrival builds the starter platform and balloon at the origin ({@link StarterBalloon#build}), which
     * becomes the spawn unless the owner had already set one; the balloon faces {@code facing}, the arriving player's
     * way, which becomes the realm's spawn facing, and is painted its region's colour unless the owner picked one
     * ({@link BalloonColors#forPreset}). A Lush realm also gets Conductor Carlo beside it ({@link NpcService#spawnConductor}).
     */
    static BlockPos spawnOf(ServerLevel level, RealmData.Realm realm, RealmAccess access, Direction facing) {
        if (!access.starterBuilt) {
            access.starterBuilt = true;
            access.spawnFacing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
            BlockPos balloon = StarterBalloon.build(level, realm.preset(), access.spawnFacing);
            access.balloon = Optional.of(balloon);
            if (realm.preset() == RealmPreset.LUSH) {
                NpcService.spawnConductor(level, balloon, access.spawnFacing);
            }
            if (access.spawn.isEmpty()) {
                access.spawn = Optional.of(balloon.above());
            }
            if (access.balloonPaint.isEmpty()) {
                access.balloonPaint = Optional.of(BalloonColors.forPreset(realm.preset()));
            }
            access.balloonPaint.ifPresent(paint -> StarterBalloon.paint(level, balloon, paint));
            RealmData.get(level.getServer()).setDirty();
        }
        return access.spawn().orElseGet(() -> realm.generated() ? realm.spawn() : surface(level));
    }

    /**
     * The teleport to a realm's spawn, whatever way the player looked before. A spawn on the starter balloon puts the
     * player in the middle of its basket, over the balloon block, looking out of its front
     * ({@link StarterBalloonBlock#FACING}); any other spawn is its block, looking along the realm's spawn facing.
     */
    static TeleportTransition arrival(ServerLevel level, BlockPos spawn, RealmAccess access) {
        Direction facing = access.spawnFacing;
        BlockPos at = spawn;
        BlockPos balloon = access.balloon.orElse(null);
        if (balloon != null && StarterBalloon.isFootprint(balloon, spawn.below())) {
            BlockState state = level.getBlockState(balloon);
            if (state.hasProperty(StarterBalloonBlock.FACING)) {
                facing = state.getValue(StarterBalloonBlock.FACING);
            }
            // The basket's centre: one block back from its front cell along the way the player looks.
            at = balloon.above();
        }
        return new TeleportTransition(level, Vec3.atBottomCenterOf(at), Vec3.ZERO, facing.toYRot(), 0.0F,
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

    /**
     * Loads one realm's border area batch by batch, letting each batch go once it is generated, then its origin's
     * chunks, which stay loaded until {@link #release} so the spawn can be read.
     */
    private static final class Pregeneration {
        private final ServerLevel level;
        private final RealmData.Realm realm;
        private int batch = -1;
        private @Nullable CompletableFuture<?> loading;

        Pregeneration(ServerLevel level, RealmData.Realm realm) {
            this.level = level;
            this.realm = realm;
        }

        /** Whether a batch is still loading. */
        boolean loading() {
            return loading != null && !loading.isDone();
        }

        /** Moves on when the current batch is loaded; true once every batch and the origin are. */
        boolean advance() {
            if (loading()) {
                return false;
            }
            if (batch == BATCHES.size()) {
                return true;
            }
            release();
            batch++;
            loading = level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_LOADING, centre(), radius());
            return false;
        }

        /** The current step's centre and radius: a border batch, or the origin after the last batch. */
        private ChunkPos centre() {
            return batch < BATCHES.size() ? BATCHES.get(batch) : ORIGIN;
        }

        private int radius() {
            return batch < BATCHES.size() ? BATCH_RADIUS : ORIGIN_RADIUS;
        }

        boolean cancelIfFor(RealmData.Realm claimed) {
            if (!realm.dimension().equals(claimed.dimension())) {
                return false;
            }
            release();
            return true;
        }

        void release() {
            if (loading != null) {
                level.getChunkSource().removeTicketWithRadius(TicketType.PLAYER_LOADING, centre(), radius());
                loading = null;
            }
        }
    }
}
