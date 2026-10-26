package com.apeira.main.navigator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.apeira.main.realm.RealmProtection;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Opens the navigator ({@link NavigatorPage#MAIN}) for a player who walks into a realm's starter balloon and stands there
 * for half a second without a break: feet over its 3x3 footprint, from the basket floor up to {@link #ZONE_HEIGHT}
 * blocks above it. It opens once per stay; leaving the zone (or the realm) starts the count again. Arriving in the zone
 * opens nothing: a player who spawns, logs in, respawns, changes dimension or is teleported onto the balloon (a move of
 * more than {@link #ARRIVAL_DISTANCE} blocks in a tick) has to step out and back in first. Server side, from each
 * player's tick.
 */
public final class BalloonNavigatorTrigger {
    /** Half a second. */
    private static final int DELAY_TICKS = 10;
    /** How many blocks over the basket floor still count as standing in the balloon. */
    private static final int ZONE_HEIGHT = 3;
    /** A move longer than this in one tick is a teleport, not a walk. */
    private static final double ARRIVAL_DISTANCE = 4.0;
    /** Each player's level, position at their last tick and ticks stood in the zone ({@link #OPENED}: not counting). */
    private static final Map<UUID, Standing> STANDING = new HashMap<>();
    /** Already opened for this stay, or arrived in the zone without walking in. */
    private static final int OPENED = -1;

    private record Standing(ResourceKey<Level> level, Vec3 position, int ticks) {}

    private BalloonNavigatorTrigger() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean inZone = !player.isSpectator() && inZone(player, RealmProtection.balloon(player.level()));
        Standing last = STANDING.get(player.getUUID());
        boolean arrived = last == null || last.level() != player.level().dimension()
                || last.position().distanceToSqr(player.position()) > ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
        int ticks;
        if (!inZone) {
            ticks = 0;
        } else if (arrived || last.ticks() == OPENED) {
            ticks = OPENED;
        } else if (last.ticks() + 1 >= DELAY_TICKS) {
            ticks = OPENED;
            NavigatorService.open(player, NavigatorPage.MAIN);
        } else {
            ticks = last.ticks() + 1;
        }
        STANDING.put(player.getUUID(), new Standing(player.level().dimension(), player.position(), ticks));
    }

    private static boolean inZone(ServerPlayer player, Optional<BlockPos> balloon) {
        if (balloon.isEmpty()) {
            return false;
        }
        BlockPos feet = player.blockPosition();
        int above = feet.getY() - balloon.get().getY();
        return above >= 1 && above <= ZONE_HEIGHT && StarterBalloon.isFootprint(balloon.get(), feet.below(above));
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STANDING.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        STANDING.clear();
    }
}
