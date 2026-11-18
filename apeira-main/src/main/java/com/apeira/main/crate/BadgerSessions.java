package com.apeira.main.crate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Every running {@link BadgerSession}, by player. Sessions tick after the server's levels ({@code ServerTickEvent.Post}),
 * which is also when instances are added and removed. A player who logs out mid-opening gets the selected reward in
 * their inventory and is sent back first, so they never log in inside a deleted instance; the server stopping does the
 * same for everyone.
 */
public final class BadgerSessions {
    private static final Map<UUID, BadgerSession> SESSIONS = new HashMap<>();

    private BadgerSessions() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(BadgerSessions::onServerTick);
        gameBus.addListener(BadgerSessions::onServerStarting);
        gameBus.addListener(BadgerSessions::onServerStopping);
        gameBus.addListener(BadgerSessions::onLoggedOut);
        gameBus.addListener(BadgerSessions::onLoggedIn);
        gameBus.addListener(BadgerLoot.Manager::addListener);
    }

    public static boolean isOpening(Player player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    /** Starts an opening; false if the player is already opening one. */
    public static boolean start(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID())) {
            return false;
        }
        SESSIONS.put(player.getUUID(), new BadgerSession(player, BadgerLoot.get().roll(player.getRandom())));
        return true;
    }

    public static void input(ServerPlayer player, int action, int value) {
        BadgerSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            session.input(player, action, value);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Iterator<BadgerSession> sessions = SESSIONS.values().iterator();
        while (sessions.hasNext()) {
            if (!sessions.next().tick(server)) {
                sessions.remove();
            }
        }
    }

    private static void onServerStarting(ServerStartingEvent event) {
        SESSIONS.clear();
        BadgerInstances.deleteLeftovers(event.getServer());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        SESSIONS.values().forEach(session -> {
            ServerPlayer player = server.getPlayerList().getPlayer(session.player);
            if (player != null) {
                session.finish(server, player, false);
            }
        });
        SESSIONS.clear();
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BadgerSession session = SESSIONS.remove(event.getEntity().getUUID());
        if (session != null && event.getEntity() instanceof ServerPlayer player) {
            session.finish(player.level().getServer(), player, false);
        }
    }

    /** Undoes an opening a crash cut short: back to the respawn point, in the game mode the player had. */
    private static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.getPersistentData().contains(BadgerSession.MODE_KEY)) {
            return;
        }
        GameType mode = GameType.byName(player.getPersistentData().getStringOr(BadgerSession.MODE_KEY, "survival"));
        player.getPersistentData().remove(BadgerSession.MODE_KEY);
        if (BadgerInstances.isInstance(player.level().dimension()) || player.isSpectator()) {
            player.teleport(player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING));
        }
        player.setGameMode(mode);
    }
}
