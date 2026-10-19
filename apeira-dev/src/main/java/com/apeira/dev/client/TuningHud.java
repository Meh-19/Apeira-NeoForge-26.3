package com.apeira.dev.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurniturePlacement;
import com.apeira.decor.furniture.FurnitureTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** The dev wand's panel in the top left corner, while it is held: the bound piece's values, the selected one marked. */
final class TuningHud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int AQUA = 0xFF55FFFF;
    private static final int BACKGROUND = 0x99000000;
    private static final int MARGIN = 4;

    private TuningHud() {}

    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DevSession.holdingWand(minecraft.player) || minecraft.gui.screen() != null || minecraft.debugEntries.isOverlayVisible()) {
            return;
        }
        FurnitureBlock block = DevSession.block();
        List<Line> lines = new ArrayList<>();
        if (block == null) {
            lines.add(new Line(Component.translatable("hud.apeira.dev.unbound").getString(), GREY));
        } else {
            FurnitureTuning tuning = DevSession.tuning(block);
            String state = DevSession.tuned(block) ? " (tuned)" : " (catalogue)";
            lines.add(new Line(block.getName().getString() + state, AQUA));
            for (TuningParameter parameter : TuningParameter.values()) {
                if (parameter.appliesTo(block)) {
                    boolean selected = parameter == DevSession.parameter();
                    lines.add(new Line((selected ? "> " : "  ") + parameter.label() + ": " + parameter.value(tuning, DevSession.box()),
                            selected ? YELLOW : WHITE));
                }
            }
            String placements = FurniturePlacement.of(tuning.placements()).stream().map(Enum::name).collect(Collectors.joining(" | "));
            lines.add(new Line("  Placement: " + placements, WHITE));
            lines.add(new Line(Component.translatable("hud.apeira.dev.controls").getString(), GREY));
            lines.add(new Line(Component.translatable("hud.apeira.dev.editor", ApeiraDevClient.OPEN_EDITOR.getTranslatedKeyMessage()).getString(), GREY));
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int width = lines.stream().mapToInt(line -> font.width(line.text)).max().orElse(0);
        int height = lines.size() * (font.lineHeight + 1);
        graphics.fill(MARGIN - 2, MARGIN - 2, MARGIN + width + 2, MARGIN + height + 1, BACKGROUND);
        int y = MARGIN;
        for (Line line : lines) {
            graphics.text(font, line.text, MARGIN, y, line.color, false);
            y += font.lineHeight + 1;
        }
    }

    private record Line(String text, int color) {}
}
