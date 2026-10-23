package com.apeira.main.ruby;

import java.util.Collection;

import com.apeira.main.realm.RealmService;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The ruby commands. {@code /pay <player> <amount>} (any player) moves rubies from the sender's balance to another
 * online player's. {@code /rubies add|remove <players> <amount>} and {@code /rubies reset <players>} (operators) change
 * balances; {@code /rubies} alone shows one's own. Every change goes through {@link Rubies}.
 */
public final class RubyCommand {
    private RubyCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Rubies.MAX))
                                .executes(RubyCommand::pay))));
        event.getDispatcher().register(Commands.literal("rubies")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    RealmService.done(player, "rubies.balance", Rubies.format(Rubies.get(player)));
                    return 1;
                })
                .then(Commands.literal("add").requires(RubyCommand::isOperator)
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1, Rubies.MAX))
                                        .executes(context -> change(context, LongArgumentType.getLong(context, "amount"))))))
                .then(Commands.literal("remove").requires(RubyCommand::isOperator)
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1, Rubies.MAX))
                                        .executes(context -> change(context, -LongArgumentType.getLong(context, "amount"))))))
                .then(Commands.literal("reset").requires(RubyCommand::isOperator)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(context -> change(context, 0L)))));
    }

    private static boolean isOperator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static int pay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        if (target == sender) {
            RealmService.fail(sender, "rubies.pay.self");
            return 0;
        }
        if (Rubies.get(target) + amount > Rubies.MAX) {
            RealmService.fail(sender, "rubies.pay.full", target.getDisplayName());
            return 0;
        }
        if (!Rubies.take(sender, amount)) {
            RealmService.fail(sender, "rubies.pay.not_enough", Rubies.format(amount), Rubies.format(Rubies.get(sender)));
            return 0;
        }
        Rubies.give(target, amount);
        RealmService.done(sender, "rubies.pay.sent", Rubies.format(amount), target.getDisplayName());
        RealmService.done(target, "rubies.pay.received", sender.getDisplayName(), Rubies.format(amount));
        return 1;
    }

    /**
     * Adds {@code delta} rubies to each player's balance (removes them when negative, as far as 0), or resets it to 0
     * when {@code delta} is 0, and reports each new balance to the operator.
     */
    private static int change(CommandContext<CommandSourceStack> context, long delta) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
        CommandSourceStack source = context.getSource();
        for (ServerPlayer player : players) {
            if (delta > 0) {
                Rubies.give(player, delta);
            } else {
                Rubies.set(player, delta == 0 ? 0L : Rubies.get(player) + delta);
            }
            String balance = Rubies.format(Rubies.get(player));
            source.sendSuccess(() -> Component.translatable("message.apeira.rubies.set", player.getDisplayName(), balance), true);
        }
        return players.size();
    }
}
