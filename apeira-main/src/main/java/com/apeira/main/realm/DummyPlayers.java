package com.apeira.main.realm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Simulated players for testing the navigator, realms and friends alone: {@code /apeiradev dummy add <name>} makes one
 * where the operator stands, shown as a vanilla mannequin wearing that name's skin and tagged {@code apeira_dummy}.
 * A dummy counts as an online player that has joined: {@link RealmData} knows its name (so commands, invites and
 * friend lists find it), {@link #isOnline} is true, it is "In Realm" in the realm it stands in, and it accepts realm
 * invites and friend requests at once. {@code /apeiradev dummy request <name>} has a dummy ask the operator to be
 * friends, for testing incoming requests. Dummies live in memory and are removed, mannequins too, when the server stops;
 * what they were added to (members, friends) stays, as for a player who logged out.
 */
public final class DummyPlayers {
    private static final String TAG = "apeira_dummy";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Map<UUID, Dummy> DUMMIES = new LinkedHashMap<>();

    private DummyPlayers() {}

    /** A dummy: its offline-mode UUID, name, and the level and spot its mannequin stands at. */
    public record Dummy(UUID id, String name, ResourceKey<Level> level, Vec3 position) {}

    public static boolean isDummy(UUID id) {
        return DUMMIES.containsKey(id);
    }

    public static Collection<Dummy> all() {
        return DUMMIES.values();
    }

    public static Optional<Dummy> byName(String name) {
        return DUMMIES.values().stream().filter(dummy -> dummy.name().equalsIgnoreCase(name)).findFirst();
    }

    /** Whether {@code id} is an online player or a dummy. */
    public static boolean isOnline(MinecraftServer server, UUID id) {
        return server.getPlayerList().getPlayer(id) != null || isDummy(id);
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("apeiradev")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("dummy")
                        .then(Commands.literal("add").then(Commands.argument("name", StringArgumentType.word()).executes(context ->
                                add(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DUMMIES.values().stream().map(Dummy::name), builder))
                                .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("request").then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DUMMIES.values().stream().map(Dummy::name), builder))
                                .executes(context -> request(context.getSource(), StringArgumentType.getString(context, "name")))))));
    }

    private static int add(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        if (!NAME.matcher(name).matches()) {
            source.sendFailure(Component.translatable("message.apeira.dummy.bad_name"));
            return 0;
        }
        UUID id = UUIDUtil.createOfflinePlayerUUID(name);
        if (isDummy(id) || server.getPlayerList().getPlayerByName(name) != null) {
            source.sendFailure(Component.translatable("message.apeira.dummy.exists", name));
            return 0;
        }
        Dummy dummy = new Dummy(id, name, source.getLevel().dimension(), source.getPosition());
        DUMMIES.put(id, dummy);
        RealmData.get(server).seen(id, name);
        server.getCommands().performPrefixedCommand(source.withSuppressedOutput(),
                String.format(java.util.Locale.ROOT, "summon minecraft:mannequin %f %f %f {profile:{name:\"%s\"},CustomName:\"%s\",CustomNameVisible:1b,Tags:[\"%s\",\"%s\"]}",
                        dummy.position().x, dummy.position().y, dummy.position().z, name, name, TAG, tag(dummy)));
        source.sendSuccess(() -> Component.translatable("message.apeira.dummy.added", name), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, String name) {
        Optional<Dummy> dummy = byName(name);
        if (dummy.isEmpty()) {
            source.sendFailure(Component.translatable("message.apeira.dummy.unknown", name));
            return 0;
        }
        discard(source.getServer(), dummy.get());
        DUMMIES.remove(dummy.get().id());
        RealmData.get(source.getServer()).seen(dummy.get().id(), dummy.get().name());
        source.sendSuccess(() -> Component.translatable("message.apeira.dummy.removed", dummy.get().name()), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        String names = String.join(", ", DUMMIES.values().stream().map(Dummy::name).toList());
        source.sendSuccess(() -> Component.translatable("message.apeira.dummy.list", DUMMIES.size(), names.isEmpty() ? "-" : names), false);
        return DUMMIES.size();
    }

    private static int request(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Optional<Dummy> dummy = byName(name);
        if (dummy.isEmpty()) {
            source.sendFailure(Component.translatable("message.apeira.dummy.unknown", name));
            return 0;
        }
        RealmData data = RealmData.get(source.getServer());
        if (data.areFriends(player.getUUID(), dummy.get().id())) {
            RealmService.fail(player, "friend.already", dummy.get().name());
            return 0;
        }
        data.addFriendRequest(dummy.get().id(), player.getUUID());
        RealmService.notifyFriendRequest(player, dummy.get().name());
        return 1;
    }

    /** Called after {@code from} invited dummy {@code target} to their realm: the dummy accepts. */
    static void onInvited(MinecraftServer server, UUID owner, UUID target) {
        if (isDummy(target)) {
            RealmAccess access = RealmData.get(server).access(owner);
            if (access.invites.remove(target) != null) {
                access.members.put(target, new RealmAccess.Member(RealmRole.RESIDENT, System.currentTimeMillis()));
                RealmData.get(server).setDirty();
                ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
                if (ownerPlayer != null) {
                    RealmService.done(ownerPlayer, "realm.accepted", DUMMIES.get(target).name());
                }
            }
        }
    }

    /** The owner of the realm a dummy stands in, if it stands in one. */
    public static Optional<UUID> realmOf(MinecraftServer server, Dummy dummy) {
        return RealmData.get(server).ownerOf(dummy.level().identifier());
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        DUMMIES.values().forEach(dummy -> discard(event.getServer(), dummy));
        DUMMIES.clear();
    }

    private static void discard(MinecraftServer server, Dummy dummy) {
        ServerLevel level = server.getLevel(dummy.level());
        if (level != null) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                    "kill @e[type=minecraft:mannequin,tag=" + tag(dummy) + "]");
        }
    }

    private static String tag(Dummy dummy) {
        return TAG + "_" + dummy.name().toLowerCase(java.util.Locale.ROOT);
    }
}
