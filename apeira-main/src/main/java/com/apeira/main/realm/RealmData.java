package com.apeira.main.realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.apeira.core.registry.ApeiraIds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Every realm dimension the server has made, saved with the overworld ({@code data/apeira/realms.dat}): the realms
 * players own, by owner, and the pre-generated pool waiting to be claimed ({@link RealmPool}); each realm's
 * {@link RealmAccess} (members, bans, invites, settings); the names and last-seen times of players who have joined, so
 * menus can list offline members; and friendships and friend requests, which the {@link VisitingRule#FRIENDS} rule uses.
 */
public final class RealmData extends SavedData {
    private static final Codec<RealmData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Realm.CODEC).fieldOf("owners").forGetter(data -> data.owners),
            Realm.CODEC.listOf().fieldOf("pool").forGetter(data -> data.pool),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, RealmAccess.CODEC).optionalFieldOf("access", Map.of()).forGetter(data -> data.access),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, KnownPlayer.CODEC).optionalFieldOf("players", Map.of()).forGetter(data -> data.players),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC.listOf()).optionalFieldOf("friends", Map.of())
                    .forGetter(data -> lists(data.friends)),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC.listOf()).optionalFieldOf("friend_requests", Map.of())
                    .forGetter(data -> lists(data.friendRequests))).apply(instance, RealmData::new));
    public static final SavedDataType<RealmData> TYPE = new SavedDataType<>(ApeiraIds.id("realms"), RealmData::new, CODEC);

    private final Map<UUID, Realm> owners = new HashMap<>();
    private final List<Realm> pool = new ArrayList<>();
    private final Map<UUID, RealmAccess> access = new HashMap<>();
    private final Map<UUID, KnownPlayer> players = new HashMap<>();
    /** Each player's friends; both sides of a friendship are stored. */
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();
    /** Open friend requests, by the player asked: who asked them. */
    private final Map<UUID, Set<UUID>> friendRequests = new HashMap<>();

    private RealmData() {}

    private RealmData(Map<UUID, Realm> owners, List<Realm> pool, Map<UUID, RealmAccess> access, Map<UUID, KnownPlayer> players,
            Map<UUID, List<UUID>> friends, Map<UUID, List<UUID>> friendRequests) {
        this.owners.putAll(owners);
        this.pool.addAll(pool);
        this.access.putAll(access);
        this.players.putAll(players);
        friends.forEach((player, list) -> this.friends.put(player, new HashSet<>(list)));
        friendRequests.forEach((player, list) -> this.friendRequests.put(player, new HashSet<>(list)));
    }

    private static Map<UUID, List<UUID>> lists(Map<UUID, Set<UUID>> sets) {
        Map<UUID, List<UUID>> lists = new HashMap<>();
        sets.forEach((key, set) -> {
            if (!set.isEmpty()) {
                lists.put(key, List.copyOf(set));
            }
        });
        return lists;
    }

    public static RealmData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Realm> realmOf(UUID owner) {
        return Optional.ofNullable(owners.get(owner));
    }

    public Iterable<Realm> owned() {
        return List.copyOf(owners.values());
    }

    public Map<UUID, Realm> owners() {
        return Map.copyOf(owners);
    }

    /** The owner of the realm whose overworld is {@code dimension}, if it is one. */
    public Optional<UUID> ownerOf(Identifier dimension) {
        return owners.entrySet().stream().filter(entry -> entry.getValue().dimension().equals(dimension)).map(Map.Entry::getKey).findFirst();
    }

    /** {@code owner}'s realm's members and settings, made with defaults the first time they are asked for. */
    public RealmAccess access(UUID owner) {
        return access.computeIfAbsent(owner, uuid -> {
            setDirty();
            return new RealmAccess(System.currentTimeMillis());
        });
    }

    /** Swaps {@code owner}'s realm for {@code realm}, keeping its members and settings (a reset). */
    public void replace(UUID owner, Realm realm) {
        owners.put(owner, realm);
        setDirty();
    }

    /** Records that {@code player} is online now under {@code name}. */
    public void seen(UUID player, String name) {
        long now = System.currentTimeMillis();
        KnownPlayer before = players.get(player);
        players.put(player, new KnownPlayer(name, now, before == null || before.firstSeen() == 0 ? now : before.firstSeen()));
        setDirty();
    }

    public Optional<KnownPlayer> known(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    public String nameOf(UUID player) {
        KnownPlayer known = players.get(player);
        return known == null ? player.toString().substring(0, 8) : known.name();
    }

    /** A player who has joined, found by name, ignoring case. */
    public Optional<UUID> byName(String name) {
        return players.entrySet().stream().filter(entry -> entry.getValue().name().equalsIgnoreCase(name)).map(Map.Entry::getKey).findFirst();
    }

    public boolean areFriends(UUID a, UUID b) {
        return friends.getOrDefault(a, Set.of()).contains(b);
    }

    public Set<UUID> friendsOf(UUID player) {
        return Set.copyOf(friends.getOrDefault(player, Set.of()));
    }

    /** Whether {@code from} has asked {@code to} to be friends. */
    public boolean hasFriendRequest(UUID from, UUID to) {
        return friendRequests.getOrDefault(to, Set.of()).contains(from);
    }

    public void addFriendRequest(UUID from, UUID to) {
        friendRequests.computeIfAbsent(to, uuid -> new HashSet<>()).add(from);
        setDirty();
    }

    /** Drops {@code from}'s request to {@code to}; false when there was none. */
    public boolean removeFriendRequest(UUID from, UUID to) {
        boolean removed = friendRequests.getOrDefault(to, new HashSet<>()).remove(from);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /** Who has asked {@code player} to be friends. */
    public Set<UUID> requestsTo(UUID player) {
        return Set.copyOf(friendRequests.getOrDefault(player, Set.of()));
    }

    /** Whom {@code player} has asked to be friends. */
    public Set<UUID> requestsFrom(UUID player) {
        Set<UUID> asked = new HashSet<>();
        friendRequests.forEach((to, from) -> {
            if (from.contains(player)) {
                asked.add(to);
            }
        });
        return asked;
    }

    /** Makes {@code a} and {@code b} friends, dropping any requests between them. */
    public void befriend(UUID a, UUID b) {
        friends.computeIfAbsent(a, uuid -> new HashSet<>()).add(b);
        friends.computeIfAbsent(b, uuid -> new HashSet<>()).add(a);
        friendRequests.getOrDefault(a, new HashSet<>()).remove(b);
        friendRequests.getOrDefault(b, new HashSet<>()).remove(a);
        setDirty();
    }

    public void unfriend(UUID a, UUID b) {
        friends.getOrDefault(a, new HashSet<>()).remove(b);
        friends.getOrDefault(b, new HashSet<>()).remove(a);
        setDirty();
    }

    public List<Realm> pool() {
        return List.copyOf(pool);
    }

    public long poolCount(RealmPreset preset) {
        return pool.stream().filter(realm -> realm.preset() == preset).count();
    }

    public void addToPool(Realm realm) {
        pool.add(realm);
        setDirty();
    }

    /** Swaps a pool realm for {@code updated}, the same dimension (for example now generated). */
    public void updatePool(Realm updated) {
        pool.replaceAll(realm -> realm.dimension().equals(updated.dimension()) ? updated : realm);
        setDirty();
    }

    /** Takes a pool realm of {@code preset}, a generated one first, and gives it to {@code owner}. */
    public Optional<Realm> claim(UUID owner, RealmPreset preset) {
        Optional<Realm> claimed = pool.stream().filter(realm -> realm.preset() == preset)
                .sorted((a, b) -> Boolean.compare(b.generated(), a.generated())).findFirst();
        claimed.ifPresent(realm -> {
            pool.remove(realm);
            owners.put(owner, realm);
            setDirty();
        });
        return claimed;
    }

    public void own(UUID owner, Realm realm) {
        owners.put(owner, realm);
        setDirty();
    }

    /** A player's last known name and when they were last online, in epoch milliseconds. */
    /** {@code firstSeen} is when they first joined, the profile's join date; 0 for players saved before it was kept. */
    public record KnownPlayer(String name, long lastSeen, long firstSeen) {
        public static final Codec<KnownPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(KnownPlayer::name),
                Codec.LONG.fieldOf("last_seen").forGetter(KnownPlayer::lastSeen),
                Codec.LONG.optionalFieldOf("first_seen", 0L).forGetter(KnownPlayer::firstSeen)).apply(instance, KnownPlayer::new));
    }

    /**
     * One realm dimension: its level key's id, its preset, the seed its terrain was made from, whether the area inside
     * its border has been generated, and where players arrive.
     */
    public record Realm(Identifier dimension, RealmPreset preset, long seed, boolean generated, BlockPos spawn) {
        public static final Codec<Realm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("dimension").forGetter(Realm::dimension),
                RealmPreset.CODEC.fieldOf("preset").forGetter(Realm::preset),
                Codec.LONG.fieldOf("seed").forGetter(Realm::seed),
                Codec.BOOL.fieldOf("generated").forGetter(Realm::generated),
                BlockPos.CODEC.fieldOf("spawn").forGetter(Realm::spawn)).apply(instance, Realm::new));

        public ResourceKey<Level> levelKey() {
            return ResourceKey.create(Registries.DIMENSION, dimension);
        }

        public Realm withGenerated(BlockPos spawn) {
            return new Realm(dimension, preset, seed, true, spawn);
        }
    }
}
