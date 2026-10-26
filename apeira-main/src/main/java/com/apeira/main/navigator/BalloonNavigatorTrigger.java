package com.apeira.main.navigator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.apeira.main.realm.RealmProtection;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Opens the navigator ({@link NavigatorPage#MAIN}) for a player who stands in a realm's starter balloon for half a second
 * without a break: feet over its 3x3 footprint, from the basket floor up to {@link #ZONE_HEIGHT} blocks above it. It
 * opens once per stay; leaving the zone (or the realm) starts the count again. Server side, from each player's tick.
 */
public final class BalloonNavigatorTrigger {
    /** Half a second. */
    private static final int DELAY_TICKS = 10;
    /** How many blocks over the basket floor still count as standing in the balloon. */
    private static final int ZONE_HEIGHT = 3;
    /** Ticks each player has stood in the zone; {@link #OPENED} once the navigator opened for this stay. */
    private static final Map<UUID, Integer> STANDING = new HashMap<>();
    private static final int OPENED = -1;

    private BalloonNavigatorTrigger() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isSpectator() || !inZone(player, RealmProtection.balloon(player.level()))) {
            STANDING.remove(player.getUUID());
            return;
        }
        int ticks = STANDING.getOrDefault(player.getUUID(), 0);
        if (ticks == OPENED) {
            return;
        }
        if (++ticks >= DELAY_TICKS) {
            STANDING.put(player.getUUID(), OPENED);
            NavigatorService.open(player, NavigatorPage.MAIN);
        } else {
            STANDING.put(player.getUUID(), ticks);
        }
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
