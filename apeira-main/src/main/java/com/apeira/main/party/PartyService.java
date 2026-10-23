package com.apeira.main.party;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.main.realm.DummyPlayers;
import com.apeira.main.realm.RealmData;
import com.apeira.main.realm.RealmService;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Every party rule: creating a party, inviting (an invite is a chat line with an [ACCEPT] button running
 * {@code /party accept <leader>}, open for a minute), accepting or declining, leaving (the next member leads when the
 * leader leaves), kicking, handing the lead over, disbanding, and party chat. Only the leader invites, kicks, hands
 * over the lead and disbands. Members are online players or dummy players ({@link DummyPlayers}, who accept invites at
 * once); a player who logs out leaves their party. The navigator's party pages and {@link PartyCommand} go through here.
 */
public final class PartyService {
    private static final Map<UUID, Party> PARTIES = new HashMap<>();
    /** Players whose chat goes to their party only. */
    private static final Set<UUID> PARTY_CHAT = new HashSet<>();

    private PartyService() {}

    /** The party {@code player} is a member of. */
    public static Optional<Party> partyOf(UUID player) {
        return Optional.ofNullable(PARTIES.get(player));
    }

    public static boolean chatOn(UUID player) {
        return PARTY_CHAT.contains(player) && PARTIES.containsKey(player);
    }

    public static boolean create(ServerPlayer player) {
        if (PARTIES.containsKey(player.getUUID())) {
            RealmService.fail(player, "party.already");
            return false;
        }
        PARTIES.put(player.getUUID(), new Party(player.getUUID()));
        RealmService.done(player, "party.created");
        return true;
    }

    /** {@code leader} invites {@code target}, making a party first if they have none. */
    public static boolean invite(ServerPlayer leader, UUID target) {
        MinecraftServer server = leader.level().getServer();
        RealmData data = RealmService.data(server);
        if (target.equals(leader.getUUID())) {
            return false;
        }
        Party party = PARTIES.get(leader.getUUID());
        if (party != null && !party.isLeader(leader.getUUID())) {
            RealmService.fail(leader, "party.not_leader");
            return false;
        }
        if (PARTIES.containsKey(target)) {
            RealmService.fail(leader, party != null && party.members.contains(target) ? "party.already_member" : "party.target_in_party",
                    data.nameOf(target));
            return false;
        }
        if (!DummyPlayers.isOnline(server, target)) {
            RealmService.fail(leader, "party.offline", data.nameOf(target));
            return false;
        }
        if (party == null) {
            party = new Party(leader.getUUID());
            PARTIES.put(leader.getUUID(), party);
            RealmService.done(leader, "party.created");
        }
        prune(party);
        if (party.members.size() + party.invites.size() >= Party.MAX_SIZE) {
            RealmService.fail(leader, "party.full");
            return false;
        }
        party.invites.put(target, System.currentTimeMillis());
        RealmService.done(leader, "party.invite", data.nameOf(target));
        if (DummyPlayers.isDummy(target)) {
            join(server, party, target);
            return true;
        }
        ServerPlayer invited = server.getPlayerList().getPlayer(target);
        if (invited != null) {
            String name = leader.getName().getString();
            Component accept = Component.translatable("message.apeira.realm.invite.accept").withStyle(style -> style
                    .withColor(ChatFormatting.GREEN).withBold(true)
                    .withClickEvent(new ClickEvent.RunCommand("/party accept " + name))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("message.apeira.party.invited.hover"))));
            invited.sendSystemMessage(Component.translatable("message.apeira.party.invited", name, accept).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return true;
    }

    public static boolean accept(ServerPlayer player, UUID leader) {
        Party party = PARTIES.get(leader);
        RealmData data = RealmService.data(player.level().getServer());
        if (party == null || !party.isInvited(player.getUUID())) {
            RealmService.fail(player, "party.no_invite", data.nameOf(leader));
            return false;
        }
        if (PARTIES.containsKey(player.getUUID())) {
            RealmService.fail(player, "party.already");
            return false;
        }
        join(player.level().getServer(), party, player.getUUID());
        return true;
    }

    public static boolean decline(ServerPlayer player, UUID leader) {
        Party party = PARTIES.get(leader);
        RealmData data = RealmService.data(player.level().getServer());
        if (party == null || party.invites.remove(player.getUUID()) == null) {
            RealmService.fail(player, "party.no_invite", data.nameOf(leader));
            return false;
        }
        RealmService.done(player, "party.declined", data.nameOf(leader));
        tell(player.level().getServer(), party, "party.declined_by", data.nameOf(player.getUUID()));
        return true;
    }

    /** The leader takes back the invite to {@code target}. */
    public static boolean cancelInvite(ServerPlayer leader, UUID target) {
        Party party = led(leader);
        if (party == null || party.invites.remove(target) == null) {
            return false;
        }
        RealmService.done(leader, "party.invite_cancelled", RealmService.data(leader.level().getServer()).nameOf(target));
        return true;
    }

    public static boolean leave(ServerPlayer player) {
        if (!PARTIES.containsKey(player.getUUID())) {
            RealmService.fail(player, "party.none");
            return false;
        }
        remove(player.level().getServer(), player.getUUID());
        RealmService.done(player, "party.left_self");
        return true;
    }

    public static boolean disband(ServerPlayer leader) {
        Party party = led(leader);
        if (party == null) {
            return false;
        }
        tell(leader.level().getServer(), party, "party.disbanded", leader.getName().getString());
        party.members.forEach(member -> {
            PARTIES.remove(member);
            PARTY_CHAT.remove(member);
        });
        return true;
    }

    public static boolean kick(ServerPlayer leader, UUID target) {
        Party party = led(leader);
        if (party == null || target.equals(leader.getUUID()) || !party.members.contains(target)) {
            return false;
        }
        MinecraftServer server = leader.level().getServer();
        String name = RealmService.data(server).nameOf(target);
        tell(server, party, "party.kicked", name);
        party.members.remove(target);
        PARTIES.remove(target);
        PARTY_CHAT.remove(target);
        return true;
    }

    /** The leader hands the lead to {@code target}, a member. */
    public static boolean transfer(ServerPlayer leader, UUID target) {
        Party party = led(leader);
        if (party == null || target.equals(leader.getUUID()) || !party.members.contains(target)) {
            return false;
        }
        party.lead(target);
        MinecraftServer server = leader.level().getServer();
        tell(server, party, "party.new_leader", RealmService.data(server).nameOf(target));
        return true;
    }

    public static boolean toggleChat(ServerPlayer player) {
        if (!PARTIES.containsKey(player.getUUID())) {
            RealmService.fail(player, "party.none");
            return false;
        }
        boolean on = PARTY_CHAT.add(player.getUUID());
        if (!on) {
            PARTY_CHAT.remove(player.getUUID());
        }
        RealmService.done(player, on ? "party.chat_on" : "party.chat_off");
        return true;
    }

    /** {@code player} leaves their party; the next member leads if they led it, and a party left empty ends. */
    public static void remove(MinecraftServer server, UUID player) {
        Party party = PARTIES.remove(player);
        PARTY_CHAT.remove(player);
        if (party == null) {
            return;
        }
        party.members.remove(player);
        if (party.members.isEmpty()) {
            return;
        }
        RealmData data = RealmService.data(server);
        tell(server, party, "party.left", data.nameOf(player));
        if (party.isLeader(player)) {
            party.lead(party.members.getFirst());
            tell(server, party, "party.new_leader", data.nameOf(party.leader));
        }
    }

    /** The party {@code player} leads, or null after telling them they don't lead one. */
    private static @Nullable Party led(ServerPlayer player) {
        Party party = PARTIES.get(player.getUUID());
        if (party == null) {
            RealmService.fail(player, "party.none");
            return null;
        }
        if (!party.isLeader(player.getUUID())) {
            RealmService.fail(player, "party.not_leader");
            return null;
        }
        return party;
    }

    private static void join(MinecraftServer server, Party party, UUID player) {
        party.invites.remove(player);
        party.members.add(player);
        PARTIES.put(player, party);
        tell(server, party, "party.joined", RealmService.data(server).nameOf(player));
    }

    /** Drops invites older than {@link Party#INVITE_MS}. */
    private static void prune(Party party) {
        party.invites.keySet().removeIf(invited -> !party.isInvited(invited));
    }

    /** The open invites of {@code party}, oldest first. */
    public static List<UUID> openInvites(Party party) {
        prune(party);
        return party.invites();
    }

    private static void tell(MinecraftServer server, Party party, String key, Object... args) {
        Component line = Component.translatable("message.apeira." + key, args).withStyle(ChatFormatting.LIGHT_PURPLE);
        for (UUID member : party.members) {
            ServerPlayer player = server.getPlayerList().getPlayer(member);
            if (player != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    /** Party chat: a chatting member's messages go to the party's online members only, as {@code [Party] <name> message}. */
    public static void onChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        Party party = PARTIES.get(sender.getUUID());
        if (party == null || !PARTY_CHAT.contains(sender.getUUID())) {
            return;
        }
        event.setCanceled(true);
        Component line = Component.translatable("message.apeira.party.chat", sender.getDisplayName(), event.getMessage());
        for (UUID member : party.members) {
            ServerPlayer player = sender.level().getServer().getPlayerList().getPlayer(member);
            if (player != null) {
                player.sendSystemMessage(line);
            }
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            remove(player.level().getServer(), player.getUUID());
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        PARTIES.clear();
        PARTY_CHAT.clear();
    }
}
