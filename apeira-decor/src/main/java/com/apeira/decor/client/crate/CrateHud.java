package com.apeira.decor.client.crate;

import com.apeira.decor.crate.CrateBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The hints shown above the hotbar while opening a crate, ported from CosmoLib's {@code CrateHud}: "Opening crate..."
 * while the items rise, then how to collect and how many rerolls are left.
 */
public final class CrateHud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int GREY = 0xFFAAAAAA;
    /** Black at about 60% opacity. */
    private static final int BACKGROUND = 0x99000000;

    private CrateHud() {}

    public static void onRenderGui(RenderGuiEvent.Post event) {
        BlockPos target = CrateCamera.target();
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || minecraft.level == null || !(minecraft.level.getBlockEntity(target) instanceof CrateBlockEntity crate)) {
            return;
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = graphics.guiWidth();
        // Above the survival bars, which take about the bottom 60 pixels.
        int hintY = graphics.guiHeight() - 85;
        int infoY = graphics.guiHeight() - 70;
        switch (crate.state()) {
            case OPENING, REROLLING -> line(graphics, font, width, hintY, Component.translatable("message.apeira.crate.opening"), WHITE);
            case LOOP -> {
                line(graphics, font, width, hintY, Component.translatable("message.apeira.crate.collect"), GREEN);
                if (crate.rerollsLeft() > 0) {
                    Component left = Component.translatable("message.apeira.crate.rerolls_left", crate.rerollsLeft()).withColor(WHITE);
                    line(graphics, font, width, infoY, Component.translatable("message.apeira.crate.reroll", left), GREY);
                } else {
                    line(graphics, font, width, infoY, Component.translatable("message.apeira.crate.no_rerolls"), GREY);
                }
            }
            default -> {}
        }
    }

    private static void line(GuiGraphicsExtractor graphics, Font font, int screenWidth, int y, Component text, int color) {
        int textWidth = font.width(text);
        int x = (screenWidth - textWidth) / 2;
        graphics.fill(x - 3, y - 2, x + textWidth + 3, y + font.lineHeight + 2, BACKGROUND);
        graphics.text(font, text, x, y, color, false);
    }
}
