package com.apeira.main.realm;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.main.realm.balloon.BalloonColors;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Every change to a realm's members and settings, and every check of who may enter or act in it. Commands
 * ({@link RealmCommand}), navigator clicks ({@code navigator/NavigatorService}) and the protection events
 * ({@link RealmProtection}) all come through here, on the server, so a client can only ask: each owner action checks
 * that the player asking owns a realm and acts on that realm only.
 */
public final class RealmService {
    /** The player whose menu click is being applied (server thread only), see {@link #quietly}. */
    private static @Nullable UUID quiet;

    private RealmService() {}

    public static RealmData data(MinecraftServer server) {
        return RealmData.get(server);
    }

    /** The owner of the realm {@code level} is, if it is one. */
    public static Optional<UUID> ownerOf(ServerLevel level) {
        return data(level.getServer()).ownerOf(level.dimension().identifier());
    }

    /** {@code player}'s role in {@code level}, or empty when the level is not a realm (no realm rules apply). */
    public static Optional<RealmRole> roleIn(ServerPlayer player, ServerLevel level) {
        return ownerOf(level).map(owner -> data(level.getServer()).access(owner).roleOf(owner, player.getUUID()));
    }

    /** Operators (permission level 2) may go anywhere and do anything in a realm. */
    public static boolean bypasses(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Whether {@code player} may be in {@code owner}'s realm: the owner and members always; banned players never;
     * visitors as the visiting rule says, and only while the owner is online if the realm is not always accessible.
     */
    public static boolean canVisit(MinecraftServer server, UUID owner, ServerPlayer player) {
        if (owner.equals(player.getUUID()) || bypasses(player)) {
            return true;
        }
        RealmData data = data(server);
        RealmAccess access = data.access(owner);
        if (access.isBanned(player.getUUID())) {
            return false;
        }
        if (access.roleOf(owner, player.getUUID()).canBuild()) {
            return access.visitingRule() != VisitingRule.NOBODY;
        }
        if (!access.alwaysAccessible() && !DummyPlayers.isOnline(server, owner)) {
            return false;
        }
        return switch (access.visitingRule()) {
            case EVERYONE -> true;
            case FRIENDS -> data.areFriends(owner, player.getUUID());
            case MEMBERS, NOBODY -> false;
        };
    }

    /** Whether {@code owner}'s realm shows in {@code viewer}'s realm list: public realms and realms they belong to. */
    public static boolean listedFor(MinecraftServer server, UUID owner, ServerPlayer viewer) {
        RealmAccess access = data(server).access(owner);
        return (access.visitingRule() == VisitingRule.EVERYONE || access.roleOf(owner, viewer.getUUID()) != RealmRole.VISITOR)
                && canVisit(server, owner, viewer);
    }

    /** Sends {@code player} to {@code owner}'s realm spawn if they may visit it. */
    public static boolean visit(ServerPlayer player, UUID owner) {
        MinecraftServer server = player.level().getServer();
        RealmData data = data(server);
        RealmData.Realm realm = data.realmOf(owner).orElse(null);
        if (realm == null) {
            fail(player, "realm.no_realm", data.nameOf(owner));
            return false;
        }
        if (!canVisit(server, owner, player)) {
            fail(player, "realm.cannot_visit", data.nameOf(owner));
            return false;
        }
        ServerLevel level = RealmLevels.create(server, realm);
        RealmAccess access = data.access(owner);
        player.teleport(RealmPool.arrival(level, RealmPool.spawnOf(level, realm, access, player.getDirection()), access));
        player.sendSystemMessage(owner.equals(player.getUUID()) ? Component.translatable("message.apeira.realm.home")
                : Component.translatable("message.apeira.realm.visiting", data.nameOf(owner)));
        return true;
    }

    /**
     * Sends {@code player} out of the realm they are in: to their own realm, or without one to their spawn point, and to
     * the world spawn when that spawn point (a bed or anchor) is in the realm they are sent out of.
     */
    public static void sendAway(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        Optional<UUID> here = ownerOf(player.level());
        if (!here.equals(Optional.of(player.getUUID())) && RealmPool.sendHome(player)) {
            return;
        }
        TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        if (here.isPresent() && ownerOf(respawn.newLevel()).equals(here)) {
            respawn = new TeleportTransition(server.findRespawnDimension(), Vec3.atBottomCenterOf(server.getRespawnData().pos()), Vec3.ZERO,
                    player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING);
        }
        player.teleport(respawn);
    }

    /** Sends away everyone in {@code owner}'s realm who may no longer be there. Run every second by {@link RealmProtection}. */
    static void evictUnwelcome(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ownerOf(player.level()).ifPresent(owner -> {
                if (!canVisit(server, owner, player)) {
                    player.sendSystemMessage(Component.translatable("message.apeira.realm.removed", data(server).nameOf(owner))
                            .withStyle(ChatFormatting.RED));
                    sendAway(player);
                }
            });
        }
    }

    // Owner actions. Each returns false, after telling the owner why, when nothing changed.

    /** The realm {@code owner} manages, or null after telling them they have none. */
    private static @Nullable RealmAccess owned(ServerPlayer owner) {
        RealmData data = data(owner.level().getServer());
        if (data.realmOf(owner.getUUID()).isEmpty()) {
            fail(owner, "realm.none");
            return null;
        }
        return data.access(owner.getUUID());
    }

    /**
     * Sends {@code target} out of {@code owner}'s realm once; they may come back if the rules let them. A player goes
     * to their own realm ({@link #sendAway}); a dummy standing in the realm has its mannequin moved out the same way
     * ({@link DummyPlayers#sendAway}).
     */
    public static boolean kick(ServerPlayer owner, UUID target) {
        if (owned(owner) == null || target.equals(owner.getUUID())) {
            return false;
        }
        MinecraftServer server = owner.level().getServer();
        Optional<DummyPlayers.Dummy> dummy = DummyPlayers.all().stream().filter(candidate -> candidate.id().equals(target)).findFirst();
        if (dummy.isPresent()) {
            if (!DummyPlayers.realmOf(server, dummy.get()).equals(Optional.of(owner.getUUID()))) {
                fail(owner, "realm.not_here", dummy.get().name());
                return false;
            }
            DummyPlayers.sendAway(server, dummy.get());
            done(owner, "realm.kick", Component.literal(dummy.get().name()));
            return true;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(target);
        if (player == null || !ownerOf(player.level()).equals(Optional.of(owner.getUUID()))) {
            fail(owner, "realm.not_here", data(owner.level().getServer()).nameOf(target));
            return false;
        }
        player.sendSystemMessage(Component.translatable("message.apeira.realm.kicked", owner.getName()).withStyle(ChatFormatting.RED));
        sendAway(player);
        done(owner, "realm.kick", player.getName());
        return true;
    }

    public static boolean ban(ServerPlayer owner, UUID target) {
        RealmAccess access = owned(owner);
        if (access == null || target.equals(owner.getUUID())) {
            return false;
        }
        RealmData data = data(owner.level().getServer());
        access.members.remove(target);
        access.invites.remove(target);
        access.banned.put(target, System.currentTimeMillis());
        data.setDirty();
        ServerPlayer player = owner.level().getServer().getPlayerList().getPlayer(target);
        if (player != null && ownerOf(player.level()).equals(Optional.of(owner.getUUID()))) {
            player.sendSystemMessage(Component.translatable("message.apeira.realm.banned", owner.getName()).withStyle(ChatFormatting.RED));
            sendAway(player);
        }
        done(owner, "realm.ban", data.nameOf(target));
        return true;
    }

    public static boolean unban(ServerPlayer owner, UUID target) {
        RealmAccess access = owned(owner);
        if (access == null || access.banned.remove(target) == null) {
            return false;
        }
        RealmData data = data(owner.level().getServer());
        data.setDirty();
        done(owner, "realm.unban", data.nameOf(target));
        return true;
    }

    /**
     * Invites {@code target}; they get a chat line with an [ACCEPT] button that runs {@code /realm invite accept}.
     * Invites stay open, so an offline or busy player can accept later.
     */
    public static boolean invite(ServerPlayer owner, UUID target) {
        RealmAccess access = owned(owner);
        if (access == null || target.equals(owner.getUUID())) {
            return false;
        }
        RealmData data = data(owner.level().getServer());
        if (access.members.containsKey(target)) {
            fail(owner, "realm.already_member", data.nameOf(target));
            return false;
        }
        access.banned.remove(target);
        access.invites.put(target, System.currentTimeMillis());
        data.setDirty();
        ServerPlayer player = owner.level().getServer().getPlayerList().getPlayer(target);
        if (player != null) {
            player.sendSystemMessage(inviteMessage(owner.getName().getString()));
        }
        done(owner, "realm.invite", data.nameOf(target));
        DummyPlayers.onInvited(owner.level().getServer(), owner.getUUID(), target);
        return true;
    }

    /** "! <Player> invited you to join their Realm! [ACCEPT] or use /navigator to view in Realm Menu" */
    static Component inviteMessage(String ownerName) {
        Component accept = Component.translatable("message.apeira.realm.invite.accept").withStyle(style -> style
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/realm invite accept " + ownerName))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("message.apeira.realm.invite.accept.hover"))));
        return Component.translatable("message.apeira.realm.invited", ownerName, accept).withStyle(ChatFormatting.YELLOW);
    }

    /** Sends every open invite again, for a player who was offline when invited. */
    public static void remindInvites(ServerPlayer player) {
        RealmData data = data(player.level().getServer());
        data.owners().keySet().forEach(owner -> {
            if (data.access(owner).isInvited(player.getUUID())) {
                player.sendSystemMessage(inviteMessage(data.nameOf(owner)));
            }
        });
    }

    /** {@code player} accepts {@code owner}'s invite and becomes a Resident. */
    public static boolean acceptInvite(ServerPlayer player, UUID owner) {
        RealmData data = data(player.level().getServer());
        RealmAccess access = data.access(owner);
        if (owner.equals(player.getUUID()) || access.invites.remove(player.getUUID()) == null) {
            fail(player, "realm.no_invite", data.nameOf(owner));
            return false;
        }
        access.members.put(player.getUUID(), new RealmAccess.Member(RealmRole.RESIDENT, System.currentTimeMillis()));
        data.setDirty();
        done(player, "realm.joined", data.nameOf(owner));
        ServerPlayer ownerPlayer = player.level().getServer().getPlayerList().getPlayer(owner);
        if (ownerPlayer != null) {
            ownerPlayer.sendSystemMessage(Component.translatable("message.apeira.realm.accepted", player.getName()).withStyle(ChatFormatting.GREEN));
        }
        return true;
    }

    public static boolean setRole(ServerPlayer owner, UUID target, RealmRole role) {
        RealmAccess access = owned(owner);
        if (access == null || role == RealmRole.OWNER || role == RealmRole.VISITOR) {
            return false;
        }
        RealmAccess.Member member = access.members.get(target);
        RealmData data = data(owner.level().getServer());
        if (member == null) {
            fail(owner, "realm.not_member", data.nameOf(target));
            return false;
        }
        access.members.put(target, new RealmAccess.Member(role, member.joined()));
        data.setDirty();
        done(owner, "realm.role", data.nameOf(target), Component.translatable("navigator.apeira.role." + role.getSerializedName()));
        return true;
    }

    /** Takes {@code target}'s membership away (Origin Realms' "Kick member from Realm" and {@code /realm remove}). */
    public static boolean removeMember(ServerPlayer owner, UUID target) {
        RealmAccess access = owned(owner);
        RealmData data = data(owner.level().getServer());
        if (access == null || access.members.remove(target) == null) {
            if (access != null) {
                fail(owner, "realm.not_member", data.nameOf(target));
            }
            return false;
        }
        data.setDirty();
        done(owner, "realm.remove", data.nameOf(target));
        return true;
    }

    /** {@code player} gives up their membership of {@code owner}'s realm. */
    public static boolean leave(ServerPlayer player, UUID owner) {
        RealmData data = data(player.level().getServer());
        if (data.access(owner).members.remove(player.getUUID()) == null) {
            fail(player, "realm.not_member_of", data.nameOf(owner));
            return false;
        }
        data.setDirty();
        done(player, "realm.left", data.nameOf(owner));
        return true;
    }

    public static void setVisitingRule(ServerPlayer owner, VisitingRule rule) {
        RealmAccess access = owned(owner);
        if (access != null && access.visitingRule != rule) {
            access.visitingRule = rule;
            data(owner.level().getServer()).setDirty();
            evictUnwelcome(owner.level().getServer());
        }
    }

    public static void togglePrivateChat(ServerPlayer owner) {
        RealmAccess access = owned(owner);
        if (access != null) {
            access.privateChat = !access.privateChat;
            data(owner.level().getServer()).setDirty();
        }
    }

    public static void toggleAlwaysAccessible(ServerPlayer owner) {
        RealmAccess access = owned(owner);
        if (access != null && access.visitingRule != VisitingRule.NOBODY) {
            access.alwaysAccessible = !access.alwaysAccessible;
            data(owner.level().getServer()).setDirty();
        }
    }

    /**
     * Starts {@code /realm setspawn}: the owner, standing in their realm, gets the client's 3x3x2 placement cursor
     * ({@link SpawnPlacementPayload}) and confirms a spot with a left-click, which comes back as {@link #moveSpawn}.
     */
    public static boolean setSpawn(ServerPlayer owner) {
        RealmAccess access = owned(owner);
        if (access == null) {
            return false;
        }
        if (!ownerOf(owner.level()).equals(Optional.of(owner.getUUID()))) {
            fail(owner, "realm.not_in_realm");
            return false;
        }
        ApeiraPackets.sendToPlayer(owner, new SpawnPlacementPayload(access.balloon));
        return true;
    }

    /**
     * Sets the realm's spawn where the owner stands, facing the way they look, and takes the starter balloon and its
     * barriers away: the settings' Set Spawn right-click ("set without balloon") and {@code /realm setspawn here}. Setting
     * the spawn with the balloon again ({@link #moveSpawn}) places a new one.
     */
    public static boolean setSpawnHere(ServerPlayer owner) {
        RealmAccess access = owned(owner);
        if (access == null) {
            return false;
        }
        if (!ownerOf(owner.level()).equals(Optional.of(owner.getUUID()))) {
            fail(owner, "realm.not_in_realm");
            return false;
        }
        ServerLevel level = owner.level();
        access.balloon.ifPresent(balloon -> StarterBalloon.remove(level, balloon));
        access.balloon = Optional.empty();
        access.starterBuilt = true;
        access.spawn = Optional.of(owner.blockPosition());
        access.spawnFacing = owner.getDirection();
        data(level.getServer()).setDirty();
        RealmHudPayload.syncLevel(level);
        done(owner, "realm.spawn_set_here");
        return true;
    }

    /** The farthest from the owner a placement may be: the client's cursor reach, and a little more. */
    private static final double SPAWN_REACH = SpawnPlacementPayload.REACH + 8.0;

    /**
     * Moves the starter balloon, and the realm's spawn with it, so that its floor's centre is {@code balloon}: checked
     * again here as the client checked it ({@link StarterBalloon#canPlace}), within reach and inside the border.
     */
    public static boolean moveSpawn(ServerPlayer owner, BlockPos balloon) {
        RealmAccess access = owned(owner);
        if (access == null) {
            return false;
        }
        ServerLevel level = owner.level();
        if (!ownerOf(level).equals(Optional.of(owner.getUUID()))) {
            fail(owner, "realm.not_in_realm");
            return false;
        }
        if (!balloon.closerToCenterThan(owner.position(), SPAWN_REACH)
                || !StarterBalloon.canPlace(level, balloon, access.balloon.orElse(null))) {
            fail(owner, "realm.setspawn.blocked");
            return false;
        }
        // Arriving players look the way the owner looked when placing it.
        access.spawnFacing = owner.getDirection();
        StarterBalloon.move(level, access.balloon.orElse(null), balloon, access.spawnFacing, access.balloonPaint.orElse(null));
        access.balloon = Optional.of(balloon);
        access.starterBuilt = true;
        access.spawn = Optional.of(balloon.above());
        data(level.getServer()).setDirty();
        RealmHudPayload.syncLevel(level);
        done(owner, "realm.spawn_set");
        return true;
    }

    /**
     * Paints the owner's starter balloon, or clears its paint with {@code null}, wherever the owner is: the realm
     * settings' balloon colour and {@code /realm balloon}.
     */
    public static boolean setBalloonPaint(ServerPlayer owner, @Nullable Integer paint) {
        RealmAccess access = owned(owner);
        if (access == null) {
            return false;
        }
        MinecraftServer server = owner.level().getServer();
        access.balloonPaint = Optional.ofNullable(paint).map(value -> value & 0xFFFFFF);
        data(server).setDirty();
        Optional<RealmData.Realm> realm = data(server).realmOf(owner.getUUID());
        if (realm.isPresent() && access.balloon.isPresent()) {
            StarterBalloon.paint(RealmLevels.create(server, realm.get()), access.balloon.get(), access.balloonPaint.orElse(null));
        }
        done(owner, "realm.balloon.painted", BalloonColors.name(access.balloonPaint.orElse(null)));
        return true;
    }

    /** Whether {@code owner} stands in their realm's overworld, which the overworld reset button needs. */
    public static boolean canResetOverworld(ServerPlayer owner) {
        return ownerOf(owner.level()).equals(Optional.of(owner.getUUID()));
    }

    /**
     * Gives {@code owner} a fresh realm of {@code preset} in place of their overworld, keeping its members and settings
     * but not its spawn point. Everyone inside is sent to their own realm or spawn; the owner goes to the new realm.
     * The old dimension stops being loaded at the next restart; its files stay on disk.
     */
    public static boolean resetOverworld(ServerPlayer owner, RealmPreset preset) {
        RealmAccess access = owned(owner);
        if (access == null) {
            return false;
        }
        if (!canResetOverworld(owner)) {
            fail(owner, "realm.reset.stand_in");
            return false;
        }
        MinecraftServer server = owner.level().getServer();
        ServerLevel old = owner.level();
        RealmPool.take(server, owner.getUUID(), preset);
        access.spawn = Optional.empty();
        // The new realm gets its own starter platform and balloon when the owner arrives.
        access.balloon = Optional.empty();
        access.starterBuilt = false;
        data(server).setDirty();
        for (ServerPlayer player : old.players().toArray(ServerPlayer[]::new)) {
            if (player != owner) {
                player.sendSystemMessage(Component.translatable("message.apeira.realm.reset.moved").withStyle(ChatFormatting.RED));
                sendAway(player);
            }
        }
        RealmPool.sendHome(owner);
        done(owner, "realm.reset", Component.translatable("navigator.apeira.preset." + preset.getSerializedName()));
        return true;
    }

    // Friends.

    public static boolean requestFriend(ServerPlayer player, UUID target) {
        RealmData data = data(player.level().getServer());
        if (target.equals(player.getUUID())) {
            return false;
        }
        if (data.areFriends(player.getUUID(), target)) {
            fail(player, "friend.already", data.nameOf(target));
            return false;
        }
        // A dummy player (DummyPlayers) says yes at once.
        if (data.hasFriendRequest(target, player.getUUID()) || DummyPlayers.isDummy(target)) {
            data.befriend(player.getUUID(), target);
            done(player, "friend.added", data.nameOf(target));
            return true;
        }
        data.addFriendRequest(player.getUUID(), target);
        done(player, "friend.requested", data.nameOf(target));
        ServerPlayer other = player.level().getServer().getPlayerList().getPlayer(target);
        if (other != null) {
            notifyFriendRequest(other, player.getName().getString());
        }
        return true;
    }

    /** Tells {@code player} that {@code from} asked to be friends, with an [ACCEPT] button running {@code /friend accept}. */
    public static void notifyFriendRequest(ServerPlayer player, String from) {
        Component accept = Component.translatable("message.apeira.realm.invite.accept").withStyle(style -> style
                .withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/friend accept " + from)));
        player.sendSystemMessage(Component.translatable("message.apeira.friend.request", from, accept).withStyle(ChatFormatting.YELLOW));
    }

    /** {@code player} turns down {@code from}'s friend request. */
    public static boolean declineFriend(ServerPlayer player, UUID from) {
        RealmData data = data(player.level().getServer());
        if (!data.removeFriendRequest(from, player.getUUID())) {
            fail(player, "friend.no_request", data.nameOf(from));
            return false;
        }
        done(player, "friend.declined", data.nameOf(from));
        return true;
    }

    /** {@code player} takes back their friend request to {@code target}. */
    public static boolean cancelFriendRequest(ServerPlayer player, UUID target) {
        RealmData data = data(player.level().getServer());
        if (!data.removeFriendRequest(player.getUUID(), target)) {
            fail(player, "friend.no_sent_request", data.nameOf(target));
            return false;
        }
        done(player, "friend.cancelled", data.nameOf(target));
        return true;
    }

    public static boolean acceptFriend(ServerPlayer player, UUID from) {
        RealmData data = data(player.level().getServer());
        if (!data.hasFriendRequest(from, player.getUUID())) {
            fail(player, "friend.no_request", data.nameOf(from));
            return false;
        }
        data.befriend(player.getUUID(), from);
        done(player, "friend.added", data.nameOf(from));
        return true;
    }

    public static boolean removeFriend(ServerPlayer player, UUID target) {
        RealmData data = data(player.level().getServer());
        if (!data.areFriends(player.getUUID(), target)) {
            fail(player, "friend.not_friends", data.nameOf(target));
            return false;
        }
        data.unfriend(player.getUUID(), target);
        evictUnwelcome(player.level().getServer());
        done(player, "friend.removed", data.nameOf(target));
        return true;
    }

    /**
     * Runs {@code action}, a menu click of {@code player}'s, without telling them it worked: the menu they clicked in
     * already shows the change, so {@link #done} skips them while it runs. Errors ({@link #fail}) still reach them.
     */
    public static void quietly(ServerPlayer player, Runnable action) {
        UUID previous = quiet;
        quiet = player.getUUID();
        try {
            action.run();
        } finally {
            quiet = previous;
        }
    }

    /** Whether {@code player} is the one whose menu click is being applied, see {@link #quietly}. */
    public static boolean isQuiet(ServerPlayer player) {
        return player.getUUID().equals(quiet);
    }

    /** A success line, green, as Origin Realms writes one; skipped for a menu click's own player ({@link #quietly}). */
    public static void done(ServerPlayer player, String key, Object... args) {
        if (!isQuiet(player)) {
            player.sendSystemMessage(Component.translatable("message.apeira." + key, args).withStyle(ChatFormatting.GREEN));
        }
    }

    /** An error line, red, as Origin Realms writes one; always sent. */

    public static void fail(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable("message.apeira." + key, args).withStyle(ChatFormatting.RED));
    }
}
