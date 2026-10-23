package com.apeira.main.realm;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.apeira.main.party.PartyService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.Entity;
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
 * friends, for testing incoming requests. The realm actions ({@code create_realm}, {@code invite}, {@code set_role},
 * {@code kick}, {@code ban}, {@code reset_realm}) have a dummy act as a realm owner towards the operator. Dummies live in memory and are removed, mannequins too, when the server stops;
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

    /** The dummy a mannequin shows, found by its {@code apeira_dummy_<name>} tag. */
    public static Optional<Dummy> ofEntity(Entity entity) {
        return entity.entityTags().contains(TAG) ? DUMMIES.values().stream().filter(dummy -> entity.entityTags().contains(tag(dummy))).findFirst()
                : Optional.empty();
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
                        .then(dummyAction("request", DummyPlayers::request))
                        .then(dummyAction("create_realm", (source, dummy) -> createRealm(source, dummy, null))
                                .then(presetArgument((source, dummy, preset) -> createRealm(source, dummy, preset))))
                        .then(dummyAction("invite", DummyPlayers::invite))
                        .then(Commands.literal("set_role").then(dummyArgument()
                                .then(Commands.literal("trusted").executes(context -> run(context, (source, dummy) -> setRole(source, dummy, RealmRole.TRUSTED))))
                                .then(Commands.literal("resident").executes(context -> run(context, (source, dummy) -> setRole(source, dummy, RealmRole.RESIDENT))))))
                        .then(dummyAction("kick", DummyPlayers::kick))
                        .then(dummyAction("ban", DummyPlayers::ban))
                        .then(dummyAction("reset_realm", (source, dummy) -> resetRealm(source, dummy, null))
                                .then(presetArgument((source, dummy, preset) -> resetRealm(source, dummy, preset))))));
    }

    /** What a dummy does to the operator who runs the command. */
    @FunctionalInterface
    private interface DummyAction {
        int run(ServerPlayer player, Dummy dummy);
    }

    @FunctionalInterface
    private interface PresetAction {
        int run(ServerPlayer player, Dummy dummy, RealmPreset preset);
    }

    /** {@code <literal> <dummy>}, running {@code action} with the operator and the named dummy. */
    private static LiteralArgumentBuilder<CommandSourceStack> dummyAction(String literal, DummyAction action) {
        return Commands.literal(literal).then(dummyArgument().executes(context -> run(context, action)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> dummyArgument() {
        return Commands.argument("name", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(DUMMIES.values().stream().map(Dummy::name), builder));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> presetArgument(PresetAction action) {
        return Commands.argument("preset", StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(RealmPreset.values()).map(RealmPreset::getSerializedName), builder))
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "preset");
                    Optional<RealmPreset> preset = Arrays.stream(RealmPreset.values()).filter(p -> p.getSerializedName().equals(name)).findFirst();
                    if (preset.isEmpty()) {
                        context.getSource().sendFailure(Component.translatable("message.apeira.dummy.bad_preset", name));
                        return 0;
                    }
                    return run(context, (player, dummy) -> action.run(player, dummy, preset.get()));
                });
    }

    private static int run(CommandContext<CommandSourceStack> context, DummyAction action) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(context, "name");
        Optional<Dummy> dummy = byName(name);
        if (dummy.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("message.apeira.dummy.unknown", name));
            return 0;
        }
        return action.run(player, dummy.get());
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
        summon(source, dummy);
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
        PartyService.remove(source.getServer(), dummy.get().id());
        RealmData.get(source.getServer()).seen(dummy.get().id(), dummy.get().name());
        source.sendSuccess(() -> Component.translatable("message.apeira.dummy.removed", dummy.get().name()), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        String names = String.join(", ", DUMMIES.values().stream().map(Dummy::name).toList());
        source.sendSuccess(() -> Component.translatable("message.apeira.dummy.list", DUMMIES.size(), names.isEmpty() ? "-" : names), false);
        return DUMMIES.size();
    }

    private static int request(ServerPlayer player, Dummy dummy) {
        RealmData data = RealmData.get(player.level().getServer());
        if (data.areFriends(player.getUUID(), dummy.id())) {
            RealmService.fail(player, "friend.already", dummy.name());
            return 0;
        }
        data.addFriendRequest(dummy.id(), player.getUUID());
        RealmService.notifyFriendRequest(player, dummy.name());
        return 1;
    }

    // Realm actions a dummy takes as an owner, aimed at the operator, so the receiving end of each can be tested alone.

    /** The dummy claims a pool realm, already generated when one is ready; a random region without {@code preset}. */
    private static int createRealm(ServerPlayer player, Dummy dummy, @Nullable RealmPreset preset) {
        MinecraftServer server = player.level().getServer();
        if (RealmData.get(server).realmOf(dummy.id()).isPresent()) {
            RealmService.fail(player, "dummy.has_realm", dummy.name());
            return 0;
        }
        RealmPreset picked = preset != null ? preset : RealmPreset.values()[player.getRandom().nextInt(RealmPreset.values().length)];
        RealmData.Realm realm = RealmPool.take(server, dummy.id(), picked);
        RealmService.done(player, "dummy.realm_created", dummy.name(), Component.translatable("navigator.apeira.preset." + picked.getSerializedName()),
                realm.dimension().toString());
        return 1;
    }

    /** The dummy invites the operator to its realm, with the same [ACCEPT] line a player's invite sends. */
    private static int invite(ServerPlayer player, Dummy dummy) {
        RealmAccess access = dummyRealm(player, dummy);
        if (access == null) {
            return 0;
        }
        if (access.members.containsKey(player.getUUID())) {
            RealmService.fail(player, "dummy.already_member", dummy.name());
            return 0;
        }
        access.banned.remove(player.getUUID());
        access.invites.put(player.getUUID(), System.currentTimeMillis());
        RealmData.get(player.level().getServer()).setDirty();
        player.sendSystemMessage(RealmService.inviteMessage(dummy.name()));
        return 1;
    }

    /** The dummy makes the operator, a member of its realm, Trusted or Resident. */
    private static int setRole(ServerPlayer player, Dummy dummy, RealmRole role) {
        RealmAccess access = dummyRealm(player, dummy);
        if (access == null) {
            return 0;
        }
        RealmAccess.Member member = access.members.get(player.getUUID());
        if (member == null) {
            RealmService.fail(player, "dummy.not_member", dummy.name());
            return 0;
        }
        access.members.put(player.getUUID(), new RealmAccess.Member(role, member.joined()));
        RealmData.get(player.level().getServer()).setDirty();
        RealmService.done(player, "dummy.role", dummy.name(), Component.translatable("navigator.apeira.role." + role.getSerializedName()));
        return 1;
    }

    /** The dummy sends the operator out of its realm, as an owner's kick does; they may come back. */
    private static int kick(ServerPlayer player, Dummy dummy) {
        RealmAccess access = dummyRealm(player, dummy);
        if (access == null) {
            return 0;
        }
        if (!RealmService.ownerOf(player.level()).equals(Optional.of(dummy.id()))) {
            RealmService.fail(player, "dummy.not_in_realm", dummy.name());
            return 0;
        }
        player.sendSystemMessage(Component.translatable("message.apeira.realm.kicked", dummy.name()).withStyle(ChatFormatting.RED));
        RealmService.sendAway(player);
        return 1;
    }

    /** The dummy bans the operator from its realm: membership and invite go, and they are sent out if inside. */
    private static int ban(ServerPlayer player, Dummy dummy) {
        RealmAccess access = dummyRealm(player, dummy);
        if (access == null) {
            return 0;
        }
        access.members.remove(player.getUUID());
        access.invites.remove(player.getUUID());
        access.banned.put(player.getUUID(), System.currentTimeMillis());
        RealmData.get(player.level().getServer()).setDirty();
        player.sendSystemMessage(Component.translatable("message.apeira.realm.banned", dummy.name()).withStyle(ChatFormatting.RED));
        if (RealmService.ownerOf(player.level()).equals(Optional.of(dummy.id()))) {
            RealmService.sendAway(player);
        }
        return 1;
    }

    /**
     * The dummy resets its realm's overworld as {@link RealmService#resetOverworld} does: a new pool realm, the same
     * members and settings, no spawn point, and everyone who was inside sent away.
     */
    private static int resetRealm(ServerPlayer player, Dummy dummy, @Nullable RealmPreset preset) {
        MinecraftServer server = player.level().getServer();
        RealmAccess access = dummyRealm(player, dummy);
        if (access == null) {
            return 0;
        }
        RealmData.Realm old = RealmData.get(server).realmOf(dummy.id()).orElseThrow();
        RealmPreset picked = preset != null ? preset : old.preset();
        RealmPool.take(server, dummy.id(), picked);
        access.spawn = Optional.empty();
        RealmData.get(server).setDirty();
        ServerLevel oldLevel = server.getLevel(old.levelKey());
        if (oldLevel != null) {
            for (ServerPlayer inside : oldLevel.players().toArray(ServerPlayer[]::new)) {
                inside.sendSystemMessage(Component.translatable("message.apeira.realm.reset.moved").withStyle(ChatFormatting.RED));
                RealmService.sendAway(inside);
            }
        }
        RealmService.done(player, "dummy.realm_reset", dummy.name(), Component.translatable("navigator.apeira.preset." + picked.getSerializedName()));
        return 1;
    }

    /** The dummy's realm settings, or null after telling the operator it has no realm yet. */
    private static @Nullable RealmAccess dummyRealm(ServerPlayer player, Dummy dummy) {
        RealmData data = RealmData.get(player.level().getServer());
        if (data.realmOf(dummy.id()).isEmpty()) {
            RealmService.fail(player, "dummy.no_realm", dummy.name(), dummy.name());
            return null;
        }
        return data.access(dummy.id());
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

    /**
     * Sends {@code dummy} out of the realm it stands in, as {@link RealmService#sendAway} does a player: its mannequin
     * moves to the dummy's own realm, or to the world spawn without one.
     */
    static void sendAway(MinecraftServer server, Dummy dummy) {
        RealmData data = RealmData.get(server);
        Optional<RealmData.Realm> own = data.realmOf(dummy.id()).filter(realm -> !realm.levelKey().equals(dummy.level()));
        ServerLevel level = own.map(realm -> RealmLevels.create(server, realm)).orElseGet(server::findRespawnDimension);
        Vec3 position = Vec3.atBottomCenterOf(own.map(realm -> RealmPool.spawnOf(level, realm, data.access(dummy.id())))
                .orElseGet(() -> server.getRespawnData().pos()));
        discard(server, dummy);
        Dummy moved = new Dummy(dummy.id(), dummy.name(), level.dimension(), position);
        DUMMIES.put(dummy.id(), moved);
        summon(server.createCommandSourceStack().withLevel(level), moved);
    }

    /** Places {@code dummy}'s mannequin, wearing its name's skin, at its spot in {@code source}'s level. */
    private static void summon(CommandSourceStack source, Dummy dummy) {
        source.getServer().getCommands().performPrefixedCommand(source.withSuppressedOutput(),
                String.format(java.util.Locale.ROOT, "summon minecraft:mannequin %f %f %f {profile:{name:\"%s\"},CustomName:\"%s\",CustomNameVisible:1b,Tags:[\"%s\",\"%s\"]}",
                        dummy.position().x, dummy.position().y, dummy.position().z, dummy.name(), dummy.name(), TAG, tag(dummy)));
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
