package com.apeira.main.party;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A party, as Origin Realms has them: a leader and up to {@link #MAX_SIZE} members in the order they joined (the leader
 * first) and the players invited and when. Parties live in memory ({@link PartyService}) and
 * end when the server stops, as a party is a group for one play session.
 */
public final class Party {
    /** The roster's two rows of seven. */
    public static final int MAX_SIZE = 14;
    /** How long an invite stays open, in milliseconds. */
    public static final long INVITE_MS = 60_000;

    UUID leader;
    final List<UUID> members = new ArrayList<>();
    /** Open invites by invited player, with when they were sent. */
    final Map<UUID, Long> invites = new LinkedHashMap<>();

    Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID leader() {
        return leader;
    }

    /** The members, the leader first. */
    public List<UUID> members() {
        return List.copyOf(members);
    }

    /** The players with an open invite, oldest first. */
    public List<UUID> invites() {
        return List.copyOf(invites.keySet());
    }

    public boolean isLeader(UUID player) {
        return leader.equals(player);
    }

    public boolean isInvited(UUID player) {
        Long sent = invites.get(player);
        return sent != null && System.currentTimeMillis() - sent < INVITE_MS;
    }

    /** Makes {@code player}, a member, the leader and moves them to the front of the roster. */
    void lead(UUID player) {
        members.remove(player);
        members.addFirst(player);
        leader = player;
    }
}
