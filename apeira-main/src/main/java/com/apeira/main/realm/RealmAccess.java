package com.apeira.main.realm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/**
 * Who may do what in one player's realm, and its settings: members and their roles, banned players, open invites,
 * the visiting rule, the chat and offline-access toggles and the spawn point. Saved in {@link RealmData} by owner and
 * changed only through {@link RealmService}, which checks that the player asking is the owner.
 */
public final class RealmAccess {
    public static final Codec<RealmAccess> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Member.CODEC).optionalFieldOf("members", Map.of()).forGetter(access -> access.members),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("banned", Map.of()).forGetter(access -> access.banned),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("invites", Map.of()).forGetter(access -> access.invites),
            VisitingRule.CODEC.optionalFieldOf("visiting_rule", VisitingRule.EVERYONE).forGetter(access -> access.visitingRule),
            Codec.BOOL.optionalFieldOf("private_chat", false).forGetter(access -> access.privateChat),
            Codec.BOOL.optionalFieldOf("always_accessible", false).forGetter(access -> access.alwaysAccessible),
            BlockPos.CODEC.optionalFieldOf("spawn").forGetter(access -> access.spawn),
            Codec.LONG.optionalFieldOf("created", 0L).forGetter(access -> access.created)).apply(instance, RealmAccess::new));

    final Map<UUID, Member> members = new HashMap<>();
    final Map<UUID, Long> banned = new HashMap<>();
    final Map<UUID, Long> invites = new HashMap<>();
    VisitingRule visitingRule;
    boolean privateChat;
    boolean alwaysAccessible;
    Optional<BlockPos> spawn;
    long created;

    RealmAccess(long created) {
        this(Map.of(), Map.of(), Map.of(), VisitingRule.EVERYONE, false, false, Optional.empty(), created);
    }

    private RealmAccess(Map<UUID, Member> members, Map<UUID, Long> banned, Map<UUID, Long> invites, VisitingRule visitingRule,
            boolean privateChat, boolean alwaysAccessible, Optional<BlockPos> spawn, long created) {
        this.members.putAll(members);
        this.banned.putAll(banned);
        this.invites.putAll(invites);
        this.visitingRule = visitingRule;
        this.privateChat = privateChat;
        this.alwaysAccessible = alwaysAccessible;
        this.spawn = spawn;
        this.created = created;
    }

    /** {@code player}'s role here, given the realm's {@code owner}. Banned players are visitors (and cannot enter). */
    public RealmRole roleOf(UUID owner, UUID player) {
        if (owner.equals(player)) {
            return RealmRole.OWNER;
        }
        Member member = members.get(player);
        return member == null ? RealmRole.VISITOR : member.role();
    }

    public Map<UUID, Member> members() {
        return Map.copyOf(members);
    }

    public Map<UUID, Long> banned() {
        return Map.copyOf(banned);
    }

    public boolean isBanned(UUID player) {
        return banned.containsKey(player);
    }

    public boolean isInvited(UUID player) {
        return invites.containsKey(player);
    }

    public VisitingRule visitingRule() {
        return visitingRule;
    }

    public boolean privateChat() {
        return privateChat;
    }

    public boolean alwaysAccessible() {
        return alwaysAccessible;
    }

    public Optional<BlockPos> spawn() {
        return spawn;
    }

    public long created() {
        return created;
    }

    /** A member's role and when they joined, in epoch milliseconds. */
    public record Member(RealmRole role, long joined) {
        public static final Codec<Member> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RealmRole.CODEC.fieldOf("role").forGetter(Member::role),
                Codec.LONG.fieldOf("joined").forGetter(Member::joined)).apply(instance, Member::new));
    }
}
