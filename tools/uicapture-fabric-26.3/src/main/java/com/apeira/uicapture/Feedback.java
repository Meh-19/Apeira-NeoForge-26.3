package com.apeira.uicapture;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Chat lines for the client commands, as Fabric's client command source (the Fabric build's) writes them: feedback plain, errors red. */
final class Feedback {
    private Feedback() {}

    static void ok(Component message) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(message);
    }

    static void error(Component message) {
        ok(message.copy().withStyle(ChatFormatting.RED));
    }
}
