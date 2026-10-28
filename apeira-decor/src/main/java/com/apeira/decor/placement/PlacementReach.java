package com.apeira.decor.placement;

/**
 * How far placement cursors ({@code client/placement/PlacementCursor}) reach, one value for every editor (the realm
 * spawn balloon, NPC anchors). The cursor has no proximity cutoff: anything the crosshair hits within {@link #CURSOR}
 * blocks can be picked, and the server accepts a spot within {@link #SERVER} blocks of the player.
 */
public final class PlacementReach {
    /** The cursor's ray length, in blocks. */
    public static final double CURSOR = 32.0;
    /** The server's limit on a confirmed spot's distance from the player: the cursor's reach and a little more. */
    public static final double SERVER = CURSOR + 8.0;

    private PlacementReach() {}
}
