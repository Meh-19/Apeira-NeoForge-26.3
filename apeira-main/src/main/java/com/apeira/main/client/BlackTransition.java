package com.apeira.main.client;

/**
 * The dimension transition: the loading screen vanilla opens while the client changes dimension (a respawn packet:
 * portals, realm visits, teleports between dimensions, respawning after death) is drawn as a plain black screen instead
 * of "Loading terrain..." over the panorama, the portal or the end sky. The first join keeps vanilla's screen.
 *
 * <p>{@code mixin/client/ClientPacketListenerMixin} marks the respawn packet's handling and
 * {@code mixin/client/LevelLoadingScreenMixin} makes a loading screen opened during it black. Both run on the client's
 * main thread.
 */
public final class BlackTransition {
    private static boolean respawning;

    private BlackTransition() {}

    /** Whether the client is handling a respawn packet, so a loading screen opened now is a dimension transition. */
    public static boolean respawning() {
        return respawning;
    }

    public static void setRespawning(boolean value) {
        respawning = value;
    }
}
