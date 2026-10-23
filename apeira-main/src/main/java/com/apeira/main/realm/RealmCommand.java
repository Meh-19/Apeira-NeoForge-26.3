package com.apeira.main.realm;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;

import com.apeira.core.network.ApeiraPackets;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /realm} (Origin Realms' command set, see the project's {@code realms/design-answers.md}) and {@code /friend}
 * ({@code /friend} alone opens the Social page).
 * Any player may use them; every change goes through {@link RealmService}, which only lets a player manage their own
 * realm. Player names may be offline players who have joined before.
 */
public final class RealmCommand {
    private RealmCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("realm")
                .then(Commands.literal("create").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if (RealmData.get(player.level().getServer()).realmOf(player.getUUID()).isPresent()) {
                        NavigatorService.open(player, NavigatorPage.REALM_SETTINGS);
                        return 0;
                    }
                    ApeiraPackets.sendToPlayer(player, new OpenOnboardingPayload());
                    return 1;
                }))
                .then(Commands.literal("home").executes(RealmCommand::home))
                .then(Commands.literal("spawn").executes(RealmCommand::home))
                .then(Commands.literal("setspawn").executes(context -> result(RealmService.setSpawn(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("members").executes(context -> open(context, NavigatorPage.REALM_MEMBERS)))
                .then(Commands.literal("settings").executes(context -> open(context, NavigatorPage.REALM_SETTINGS)))
                .then(Commands.literal("reset").executes(context -> open(context, NavigatorPage.REALM_SETTINGS)))
                .then(Commands.literal("view").executes(context -> {
                    context.getSource().getPlayerOrException().sendSystemMessage(Component.translatable("message.apeira.navigator.unavailable",
                            Component.translatable("navigator.apeira.realm_view")));
                    return 0;
                }))
                .then(Commands.literal("invite")
                        .then(Commands.literal("accept").then(named("owner", RealmService::acceptInvite)))
                        .then(named("player", RealmService::invite)))
                .then(Commands.literal("kick").then(named("player", RealmService::kick)))
                .then(Commands.literal("ban").then(named("player", RealmService::ban)))
                .then(Commands.literal("unban").then(named("player", RealmService::unban)))
                .then(Commands.literal("remove").then(named("player", RealmService::removeMember)))
                .then(Commands.literal("leave").then(named("owner", RealmService::leave)))
                .then(Commands.literal("tp").then(named("owner", RealmService::visit)))
                .then(Commands.literal("member").then(named("player", (player, target) -> {
                    RealmData data = RealmData.get(player.level().getServer());
                    if (data.realmOf(player.getUUID()).isEmpty()) {
                        RealmService.fail(player, "realm.none");
                        return false;
                    }
                    RealmRole role = data.access(player.getUUID()).roleOf(player.getUUID(), target);
                    player.sendSystemMessage(role.canBuild()
                            ? Component.translatable("message.apeira.realm.member", data.nameOf(target), NavigatorService.role(role))
                            : Component.translatable("message.apeira.realm.not_member", data.nameOf(target)));
                    return role.canBuild();
                }))));
        event.getDispatcher().register(Commands.literal("friend")
                .executes(context -> open(context, NavigatorPage.SOCIAL))
                .then(Commands.literal("decline").then(named("player", RealmService::declineFriend)))
                .then(Commands.literal("cancel").then(named("player", RealmService::cancelFriendRequest)))
                .then(Commands.literal("add").then(named("player", RealmService::requestFriend)))
                .then(Commands.literal("accept").then(named("player", RealmService::acceptFriend)))
                .then(Commands.literal("remove").then(named("player", RealmService::removeFriend)))
                .then(Commands.literal("list").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    RealmData data = RealmData.get(player.level().getServer());
                    String names = String.join(", ", data.friendsOf(player.getUUID()).stream().map(data::nameOf).sorted().toList());
                    player.sendSystemMessage(Component.translatable("message.apeira.friend.list", names.isEmpty() ? "-" : names));
                    return 1;
                })));
    }

    private static int home(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!RealmPool.sendHome(player)) {
            player.sendSystemMessage(Component.translatable("message.apeira.realm.none"));
            return 0;
        }
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> context, NavigatorPage page) throws CommandSyntaxException {
        NavigatorService.open(context.getSource().getPlayerOrException(), page);
        return 1;
    }

    private static int result(boolean success) {
        return success ? 1 : 0;
    }

    /** A player-name argument (online names suggested) whose player is passed to {@code action} with the command's player. */
    public static RequiredArgumentBuilder<CommandSourceStack, String> named(String argument,
            BiPredicate<ServerPlayer, UUID> action) {
        return Commands.argument(argument, StringArgumentType.word())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(java.util.stream.Stream.concat(
                        context.getSource().getOnlinePlayerNames().stream(), DummyPlayers.all().stream().map(DummyPlayers.Dummy::name)), builder))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String name = StringArgumentType.getString(context, argument);
                    Optional<UUID> target = resolve(player, name);
                    if (target.isEmpty()) {
                        RealmService.fail(player, "realm.unknown_player", name);
                        return 0;
                    }
                    return result(action.test(player, target.get()));
                });
    }

    private static Optional<UUID> resolve(ServerPlayer player, String name) {
        ServerPlayer online = player.level().getServer().getPlayerList().getPlayerByName(name);
        return online != null ? Optional.of(online.getUUID()) : RealmData.get(player.level().getServer()).byName(name);
    }
}
