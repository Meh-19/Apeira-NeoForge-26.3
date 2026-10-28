package com.apeira.main.npc;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.main.Apeira;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.realm.RealmService;
import com.apeira.main.registry.MainNpcs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The server side of realm NPCs: Shift-right-clicking an {@link NpcEntity} opens its settings in the navigator
 * ({@link NavigatorService#openNpc}, the {@code EDIT_NPC} popup) for whoever may edit it; the popup's buttons toggle its
 * visibility ({@link #toggleShown}) and start the anchor placement cursor ({@link #startAnchor}), whose left-click moves
 * it ({@link #anchor}). {@link #spawnConductor} places Conductor Carlo by a Lush realm's new starter balloon.
 */
public final class NpcService {
    /** The farthest from the player an anchor may be: the client's cursor reach, and a little more. */
    private static final double ANCHOR_REACH = NpcAnchorPayload.REACH + 8.0;
    /** Conductor Carlo stands this many blocks in front of the starter balloon's centre ... */
    static final int CONDUCTOR_FORWARD = 3;
    /** ... and this many to its left, as seen looking out of the balloon's front. */
    static final int CONDUCTOR_LEFT = 2;
    /** He faces the balloon turned this far clockwise, seen from above (yaw grows clockwise). */
    static final float CONDUCTOR_TURN = 90.0F;

    private NpcService() {}

    /** Shift-right-clicking an NPC opens its settings; anyone who may not edit it is told so. */
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NpcEntity npc) || !event.getEntity().isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            if (npc.canEdit(player)) {
                NavigatorService.openNpc(player, npc.getUUID());
            } else {
                RealmService.fail(player, "npc.not_owner");
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
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
        ApeiraPackets.sendToPlayer(player, new NpcAnchorPayload(npc.getId()));
        return true;
    }

    /**
     * Moves the NPC with entity id {@code id} to stand on {@code anchor}, facing {@code player}, checked again here as the
     * client checked it ({@link NpcEntity#canStandAt}), within reach.
     */
    public static boolean anchor(ServerPlayer player, int id, BlockPos anchor) {
        ServerLevel level = player.level();
        if (!(level.getEntity(id) instanceof NpcEntity npc) || !npc.isAlive() || !npc.canEdit(player)) {
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
     * Places Conductor Carlo by a Lush realm's new starter balloon, whose floor's centre is {@code balloon} and whose
     * front faces {@code facing}: {@link #CONDUCTOR_FORWARD} blocks out of its front and {@link #CONDUCTOR_LEFT} to its left, on the ground there (the top block of that column, leaves ignored), facing the balloon turned {@link #CONDUCTOR_TURN} degrees clockwise. Nothing is placed
     * when that spot has no room for him.
     */
    public static void spawnConductor(ServerLevel level, BlockPos balloon, Direction facing) {
        Direction left = facing.getCounterClockWise();
        int x = balloon.getX() + facing.getStepX() * CONDUCTOR_FORWARD + left.getStepX() * CONDUCTOR_LEFT;
        int z = balloon.getZ() + facing.getStepZ() * CONDUCTOR_FORWARD + left.getStepZ() * CONDUCTOR_LEFT;
        BlockPos anchor = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!NpcEntity.canStandAt(level, MainNpcs.CONDUCTOR_CARLO.get(), anchor)) {
            Apeira.LOGGER.warn("No room for Conductor Carlo at {} in {}", anchor, level.dimension().identifier());
            return;
        }
        NpcEntity carlo = MainNpcs.CONDUCTOR_CARLO.get().create(level, EntitySpawnReason.EVENT);
        if (carlo == null) {
            return;
        }
        carlo.snapTo(Vec3.atBottomCenterOf(anchor));
        carlo.face(Vec3.atBottomCenterOf(balloon));
        carlo.turn(CONDUCTOR_TURN);
        level.addFreshEntity(carlo);
    }
}
