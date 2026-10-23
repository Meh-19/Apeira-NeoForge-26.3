package com.apeira.main.party;

import java.util.function.Predicate;

import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.apeira.main.realm.RealmCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /party} opens the party roster; {@code /party create|leave|disband|chat}, {@code /party invite|kick|transfer
 * <player>} and {@code /party accept|decline <leader>} act through {@link PartyService}. Any player may use them.
 */
public final class PartyCommand {
    private PartyCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("party")
                .executes(context -> {
                    NavigatorService.open(context.getSource().getPlayerOrException(), NavigatorPage.PARTY);
                    return 1;
                })
                .then(self("create", PartyService::create))
                .then(self("leave", PartyService::leave))
                .then(self("disband", PartyService::disband))
                .then(self("chat", PartyService::toggleChat))
                .then(Commands.literal("invite").then(RealmCommand.named("player", PartyService::invite)))
                .then(Commands.literal("kick").then(RealmCommand.named("player", PartyService::kick)))
                .then(Commands.literal("transfer").then(RealmCommand.named("player", PartyService::transfer)))
                .then(Commands.literal("accept").then(RealmCommand.named("leader", PartyService::accept)))
                .then(Commands.literal("decline").then(RealmCommand.named("leader", PartyService::decline))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> self(String name, Predicate<ServerPlayer> action) {
        return Commands.literal(name).executes(context -> action.test(context.getSource().getPlayerOrException()) ? 1 : 0);
    }
}
