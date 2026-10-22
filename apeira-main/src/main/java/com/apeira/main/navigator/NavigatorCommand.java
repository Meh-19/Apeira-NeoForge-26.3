package com.apeira.main.navigator;

import com.apeira.core.network.ApeiraPackets;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /navigator}: opens the navigator's root menu for the player who runs it. Any player may use it. */
public final class NavigatorCommand {
    private NavigatorCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("navigator").executes(context -> {
            ApeiraPackets.sendToPlayer(context.getSource().getPlayerOrException(), new OpenNavigatorPayload(NavigatorPage.MAIN));
            return 1;
        }));
    }
}
