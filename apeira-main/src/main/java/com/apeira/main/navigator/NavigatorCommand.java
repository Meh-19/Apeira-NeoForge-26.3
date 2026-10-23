package com.apeira.main.navigator;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /navigator} opens the navigator's root menu for the player who runs it, and {@code /realms} opens Realms home
 * straight away, with its Back button greyed out. Any player may use them.
 */
public final class NavigatorCommand {
    private NavigatorCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("navigator").executes(context -> {
            NavigatorService.open(context.getSource().getPlayerOrException(), NavigatorPage.MAIN);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("realms").executes(context -> {
            NavigatorService.openRealms(context.getSource().getPlayerOrException());
            return 1;
        }));
    }
}
