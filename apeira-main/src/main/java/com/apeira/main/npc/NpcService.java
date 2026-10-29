package com.apeira.main.npc;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.decor.placement.PlacementReach;
import com.apeira.main.Apeira;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.realm.RealmService;
import com.apeira.main.realm.RealmPreset;
import com.apeira.main.registry.MainNpcs;
import com.apeira.main.registry.MainSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The server side of realm NPCs: Shift-right-clicking an {@link NpcEntity} opens its settings in the navigator
 * ({@link NavigatorService#openNpc}, the {@code EDIT_NPC} popup) for whoever may edit it; the popup's buttons toggle its
 * visibility ({@link #toggleShown}) and start the anchor placement cursor ({@link #startAnchor}), whose left-click moves
 * it ({@link #anchor}). A plain right-click talks to it ({@link #talk}), and left-clicking it in the dialogue opens its
 * shop, if it has one ({@link #openShop}). {@link #spawnConductor} places a region's balloon conductor by a
 * Lush realm's new starter balloon.
 */
public final class NpcService {
    /** The farthest from the player an anchor may be: the client's cursor reach, and a little more. */
    private static final double ANCHOR_REACH = PlacementReach.SERVER;
    /** Conductor Carlo stands this many blocks in front of the starter balloon's centre ... */
    static final int CONDUCTOR_FORWARD = 3;
    /** ... and this many to its left, as seen looking out of the balloon's front. */
    static final int CONDUCTOR_LEFT = 2;
    /** He faces the balloon turned this far clockwise, seen from above (yaw grows clockwise): -90, a quarter turn counter-clockwise. */
    static final float CONDUCTOR_TURN = -90.0F;
    /** Jeckyl is placed this near the starter balloon, in blocks on the larger axis ... */
    static final int JECKYL_MIN = 4;
    static final int JECKYL_MAX = 10;
    /** ... after at most this many random tries. */
    private static final int JECKYL_TRIES = 48;
    /** Until he has been found in a realm, Jeckyl is placed anywhere within this many blocks of its starting position ... */
    static final int JECKYL_SEARCH_RADIUS = 200;
    /** ... after at most this many random tries, then close by. */
    private static final int JECKYL_SEARCH_TRIES = 32;

    private NpcService() {}

    /** Shift-right-clicking an NPC opens its settings (anyone who may not edit it is told so); a plain right-click talks to it. */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NpcEntity npc) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            if (!player.isShiftKeyDown()) {
                talk(player, npc);
            } else if (npc.canEdit(player)) {
                NavigatorService.openNpc(player, npc.getUUID());
            } else {
                RealmService.fail(player, "npc.not_owner");
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /**
     * {@code player} speaks to {@code npc}: it answers in its voice ({@link MainSounds#NPC_VOICE_MALE1}, heard by everyone
     * near) and the player's client opens its dialogue with the NPC's greeting, {@code dialogue.<namespace>.<npc>.greeting}.
     * A hidden NPC only talks to those who may edit it.
     */
    public static void talk(ServerPlayer player, NpcEntity npc) {
        if (!npc.isShown() && !npc.canEdit(player)) {
            return;
        }
        npc.level().playSound(null, npc.getX(), npc.getEyeY(), npc.getZ(), MainSounds.NPC_VOICE_MALE1.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        ApeiraPackets.sendToPlayer(player, new NpcDialoguePayload(npc.getId(), npc.lineFor(player), npc.shop() != null));
    }

    /**
     * Opens the shop of the NPC with entity id {@code id} for {@code player}, who left-clicked it in its dialogue:
     * only an NPC with a shop, alive, within {@link NpcShopMenu#REACH} blocks and visible to the player.
     */
    public static void openShop(ServerPlayer player, int id) {
        if (!(player.level().getEntity(id) instanceof NpcEntity npc) || !npc.isAlive() || npc.shop() == null
                || npc.distanceToSqr(player) > NpcShopMenu.REACH * NpcShopMenu.REACH || (!npc.isShown() && !npc.canEdit(player))) {
            return;
        }
        NpcShop shop = npc.shop();
        player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) -> new NpcShopMenu(containerId, inventory, shop, npc),
                npc.getDisplayName()));
    }

    /** The NPC with {@code id} in {@code player}'s level, if {@code player} may edit it. */
    public static @Nullable NpcEntity editable(ServerPlayer player, @Nullable UUID id) {
        if (id == null) {
            return null;
        }
        Entity entity = player.level().getEntity(id);
        return entity instanceof NpcEntity npc && npc.isAlive() && npc.canEdit(player) ? npc : null;
    }

    /** Shows or hides the NPC. Returns false when {@code player} may not edit it (or it is gone). */
    public static boolean toggleShown(ServerPlayer player, @Nullable UUID id) {
        NpcEntity npc = editable(player, id);
        if (npc == null) {
            return false;
        }
        npc.setShown(!npc.isShown());
        RealmService.done(player, npc.isShown() ? "npc.shown" : "npc.hidden", npc.getDisplayName());
        return true;
    }

    /** Starts the anchor placement cursor for the NPC. Returns false when {@code player} may not edit it. */
    public static boolean startAnchor(ServerPlayer player, @Nullable UUID id) {
        NpcEntity npc = editable(player, id);
        if (npc == null) {
            return false;
        }
        if (!npc.canAnchor()) {
            RealmService.fail(player, "npc.anchor_locked", npc.getDisplayName());
            return false;
        }
        ApeiraPackets.sendToPlayer(player, new NpcAnchorPayload(npc.getId()));
        return true;
    }

    /**
     * Moves the NPC with entity id {@code id} to stand on {@code anchor}, facing {@code player}, checked again here as the
     * client checked it ({@link NpcEntity#canStandAt}), within reach.
     */
    public static boolean anchor(ServerPlayer player, int id, BlockPos anchor) {
        ServerLevel level = player.level();
        if (!(level.getEntity(id) instanceof NpcEntity npc) || !npc.isAlive() || !npc.canEdit(player) || !npc.canAnchor()) {
            RealmService.fail(player, "npc.not_owner");
            return false;
        }
        if (!anchor.closerToCenterThan(player.position(), ANCHOR_REACH) || !NpcEntity.canStandAt(level, npc.getType(), anchor)) {
            RealmService.fail(player, "npc.anchor_blocked");
            return false;
        }
        npc.anchor(level, anchor, player.getEyePosition());
        RealmService.done(player, "npc.anchored", npc.getDisplayName());
        return true;
    }

    /**
     * Places the region's balloon conductor ({@link MainNpcs#conductor(RealmPreset)}: Carlo, Marco, Gino or Antonio) by a
     * realm's new starter balloon, whose floor's centre is {@code balloon} and whose
     * front faces {@code facing}: {@link #CONDUCTOR_FORWARD} blocks out of its front and {@link #CONDUCTOR_LEFT} to its left, on the ground there (the top block of that column, leaves ignored), facing the balloon turned a quarter turn counter-clockwise ({@link #CONDUCTOR_TURN}). Nothing is placed
     * when that spot has no room for him.
     */
    public static void spawnConductor(ServerLevel level, BlockPos balloon, Direction facing, RealmPreset preset) {
        EntityType<NpcEntity> type = MainNpcs.conductor(preset);
        Direction left = facing.getCounterClockWise();
        int x = balloon.getX() + facing.getStepX() * CONDUCTOR_FORWARD + left.getStepX() * CONDUCTOR_LEFT;
        int z = balloon.getZ() + facing.getStepZ() * CONDUCTOR_FORWARD + left.getStepZ() * CONDUCTOR_LEFT;
        BlockPos anchor = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!NpcEntity.canStandAt(level, type, anchor)) {
            Apeira.LOGGER.warn("No room for {} at {} in {}", EntityType.getKey(type), anchor, level.dimension().identifier());
            return;
        }
        NpcEntity conductor = type.create(level, EntitySpawnReason.EVENT);
        if (conductor == null) {
            return;
        }
        conductor.snapTo(Vec3.atBottomCenterOf(anchor));
        conductor.face(Vec3.atBottomCenterOf(balloon));
        conductor.turn(CONDUCTOR_TURN);
        level.addFreshEntity(conductor);
    }

    /**
     * Places Jeckyl by a realm's new starter balloon (floor centre {@code balloon}). Until he has been {@code found} in
     * that realm, he goes to a random spot on the ground anywhere within {@link #JECKYL_SEARCH_RADIUS} blocks of it and
     * inside the world border ({@link #surfaceFar}), to be searched for; once found (a reset realm), he is placed close
     * by, {@link #JECKYL_MIN} to {@link #JECKYL_MAX} blocks away on each axis' larger offset as Jacko is, already found
     * and standing still. Either spot is the home he wanders round, and a far spot that cannot be found falls back on a
     * close one. Nothing is placed when {@link #JECKYL_TRIES} tries find no room.
     */
    public static void spawnJeckyl(ServerLevel level, BlockPos balloon, boolean found) {
        EntityType<JeckylEntity> type = MainNpcs.JECKYL.get();
        BlockPos spot = found ? null : surfaceFar(level, type, balloon);
        if (spot == null) {
            spot = surfaceNear(level, type, balloon);
        }
        if (spot == null) {
            Apeira.LOGGER.warn("No room for Jeckyl near {} in {}", balloon, level.dimension().identifier());
            return;
        }
        JeckylEntity jeckyl = type.create(level, EntitySpawnReason.EVENT);
        if (jeckyl == null) {
            return;
        }
        jeckyl.snapTo(Vec3.atBottomCenterOf(spot));
        jeckyl.face(Vec3.atBottomCenterOf(balloon));
        jeckyl.setHome(spot);
        if (found) {
            jeckyl.markFound();
        }
        level.addFreshEntity(jeckyl);
    }

    /**
     * A random spot on the ground within {@link #JECKYL_SEARCH_RADIUS} blocks of {@code center}, inside the world border,
     * where {@code type} fits; its chunk is loaded (generated, if it was not) to look. Null after
     * {@link #JECKYL_SEARCH_TRIES} tries.
     */
    static @Nullable BlockPos surfaceFar(ServerLevel level, EntityType<?> type, BlockPos center) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < JECKYL_SEARCH_TRIES; i++) {
            double distance = JECKYL_SEARCH_RADIUS * Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2.0;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            if (!level.getWorldBorder().isWithinBounds(x + 0.5, z + 0.5, -2.0)) {
                continue;
            }
            level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (NpcEntity.canStandAt(level, type, pos)) {
                return pos;
            }
        }
        return null;
    }

    /** A random spot on the ground {@link #JECKYL_MIN} to {@link #JECKYL_MAX} blocks from {@code center} where {@code type} fits. */
    static @Nullable BlockPos surfaceNear(ServerLevel level, EntityType<?> type, BlockPos center) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < JECKYL_TRIES; i++) {
            int dx = random.nextIntBetweenInclusive(-JECKYL_MAX, JECKYL_MAX);
            int dz = random.nextIntBetweenInclusive(-JECKYL_MAX, JECKYL_MAX);
            if (Math.max(Math.abs(dx), Math.abs(dz)) < JECKYL_MIN) {
                continue;
            }
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.isLoaded(pos) && NpcEntity.canStandAt(level, type, pos)) {
                return pos;
            }
        }
        return null;
    }
}
