package com.apeira.main.home;

import java.util.List;

import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.navigator.NavigatorService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /homes} opens the Homes page; {@code /home [name]} travels to a home (the first without a name),
 * {@code /sethome [name]}, {@code /delhome <name>} and {@code /home rename <name> <new name>} change them. All go
 * through {@link HomeService}.
 */
public final class HomeCommand {
    private static final SuggestionProvider<CommandSourceStack> HOMES = (context, builder) -> SharedSuggestionProvider.suggest(
            HomeService.homesOf(context.getSource().getPlayerOrException()).stream().map(HomeData.Home::name), builder);

    private HomeCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("homes").executes(context -> {
            NavigatorService.open(context.getSource().getPlayerOrException(), NavigatorPage.HOMES);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("home")
                .executes(context -> result(HomeService.teleport(context.getSource().getPlayerOrException(), 0)))
                .then(Commands.literal("rename").then(Commands.argument("name", StringArgumentType.word()).suggests(HOMES)
                        .then(Commands.argument("new_name", StringArgumentType.word()).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return result(HomeService.rename(player, index(context, player),
                                    StringArgumentType.getString(context, "new_name")));
                        }))))
                .then(Commands.argument("name", StringArgumentType.word()).suggests(HOMES).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return result(HomeService.teleport(player, index(context, player)));
                })));
        event.getDispatcher().register(Commands.literal("sethome")
                .executes(context -> result(HomeService.setNext(context.getSource().getPlayerOrException())))
                .then(Commands.argument("name", StringArgumentType.word()).executes(context -> result(
                        HomeService.set(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "name"))))));
        event.getDispatcher().register(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.word()).suggests(HOMES).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return result(HomeService.delete(player, index(context, player)));
                })));
    }

    private static int index(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        List<HomeData.Home> homes = HomeService.homesOf(player);
        for (int i = 0; i < homes.size(); i++) {
            if (homes.get(i).name().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int result(boolean success) {
        return success ? 1 : 0;
    }
}
