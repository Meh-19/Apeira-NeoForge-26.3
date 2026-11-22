package com.apeira.dev.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.decor.cosmetic.BackOffsets;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.SeatFurnitureBlock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * The dev wand's panel in the top left corner, while it is held: the bound piece's values in the tuner's mode, or the
 * worn back cosmetic's, the selected one marked. Rows carry Ren's tool icons ({@code textures/gui/dev/tools.png}, 16
 * pixel cells drawn at 8): the page for the mode and editor, add for the seat count, move for seat positions, undo for
 * restoring deleted boxes, the bin for the box deleter; for back cosmetics move, turn, scale, export and reset. In back cosmetic mode Ren's axis gizmo ({@code axis_gizmo.png}) shows which colour is which axis.
 */
final class TuningHud {
    private static final Identifier TOOLS = ApeiraIds.id("textures/gui/dev/tools.png");
    private static final Identifier AXES = ApeiraIds.id("textures/gui/dev/axis_gizmo.png");
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int AQUA = 0xFF55FFFF;
    private static final int RED = 0xFFFF5555;
    private static final int BACKGROUND = 0x99000000;
    private static final int MARGIN = 4;
    private static final int ICON = 8;
    /** The axis gizmo's drawn part: 43x37 of its 64x64 texture, drawn at half size. */
    private static final int AXES_WIDTH = 43;
    private static final int AXES_HEIGHT = 37;

    /** A cell of the tools sheet. */
    private enum Tool {
        ADD(0, 0), MOVE(16, 0), TURN(32, 0), RESET(48, 0), SCALE(0, 16), EDITOR(16, 16), EXPORT(32, 16), UNDO(48, 16);

        final int u;
        final int v;

        Tool(int u, int v) {
            this.u = u;
            this.v = v;
        }
    }

    private TuningHud() {}

    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!DevSession.holdingWand(minecraft.player) || minecraft.gui.screen() != null || minecraft.debugEntries.isOverlayVisible()) {
            return;
        }
        FurnitureBlock block = DevSession.block();
        Item back = DevSession.backCosmetic();
        List<Line> lines = new ArrayList<>();
        if (block != null) {
            furnitureLines(block, lines);
        } else if (back != null) {
            backLines(back, lines);
        } else {
            lines.add(new Line(Component.translatable("hud.apeira.dev.unbound").getString(), GREY, null));
        }
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int textX = MARGIN + ICON + 2;
        int width = ICON + 2 + lines.stream().mapToInt(line -> font.width(line.text)).max().orElse(0);
        int height = lines.size() * (font.lineHeight + 1);
        boolean axes = block == null && back != null;
        int panelWidth = axes ? width + 4 + AXES_WIDTH / 2 + 2 : width;
        graphics.fill(MARGIN - 2, MARGIN - 2, MARGIN + panelWidth + 2, MARGIN + Math.max(height, axes ? AXES_HEIGHT / 2 + 2 : 0) + 1, BACKGROUND);
        int y = MARGIN;
        for (Line line : lines) {
            if (line.tool != null) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TOOLS, MARGIN, y, line.tool.u, line.tool.v, ICON, ICON, 16, 16, 64, 64);
            }
            graphics.text(font, line.text, textX, y, line.color, false);
            y += font.lineHeight + 1;
        }
        if (axes) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, AXES, MARGIN + width + 4, MARGIN, 0.0F, 0.0F, AXES_WIDTH / 2, AXES_HEIGHT / 2,
                    AXES_WIDTH, AXES_HEIGHT, 64, 64);
        }
    }

    private static void furnitureLines(FurnitureBlock block, List<Line> lines) {
        FurnitureTuning tuning = DevSession.tuning(block);
        TunerMode mode = DevSession.mode();
        String state = DevSession.tuned(block) ? " (tuned)" : " (code)";
        lines.add(new Line(block.getName().getString() + state, AQUA, null));
        for (TuningParameter parameter : TuningParameter.shown(mode, block)) {
            boolean selected = parameter == DevSession.parameter();
            lines.add(new Line((selected ? "> " : "  ") + parameter.label() + ": " + parameter.value(mode, block, tuning, DevSession.seat()),
                    selected ? YELLOW : WHITE, tool(parameter)));
        }
        switch (mode) {
            case SEATING -> {
                if (!(block instanceof SeatFurnitureBlock)) {
                    lines.add(new Line(Component.translatable("hud.apeira.dev.not_a_seat").getString(), GREY, null));
                }
            }
            case BOXES -> {
                DevSession.TargetBox target = DevSession.targetBox();
                lines.add(new Line(Component.translatable("hud.apeira.dev.box_deleter").getString(), GREY, Tool.RESET));
                lines.add(new Line(target == null ? "  Looking at: no box" : "  Looking at: " + box(target.removed().box())
                        + (target.removed().diagonal() ? " (diagonal)" : ""), target == null ? GREY : RED, null));
            }
            case LIGHT -> {}
        }
        lines.add(new Line(Component.translatable("hud.apeira.dev.controls").getString(), GREY, null));
        lines.add(new Line(Component.translatable("hud.apeira.dev.editor", ApeiraDevClient.OPEN_EDITOR.getTranslatedKeyMessage()).getString(),
                GREY, Tool.EDITOR));
    }

    private static String box(FurnitureTuning.Box box) {
        return String.join(" ", TuningParameter.number(box.minX()), TuningParameter.number(box.minY()), TuningParameter.number(box.minZ()),
                TuningParameter.number(box.maxX()), TuningParameter.number(box.maxY()), TuningParameter.number(box.maxZ()));
    }

    private static void backLines(Item back, List<Line> lines) {
        BackOffset offset = DevSession.backOffset(back);
        String state = BackOffsets.get(back) != null ? " (tuned)" : " (default)";
        lines.add(new Line(Component.translatable("hud.apeira.dev.back", back.getDefaultInstance().getHoverName()).getString() + state, AQUA, null));
        for (BackParameter parameter : BackParameter.values()) {
            boolean selected = parameter == DevSession.backParameter();
            lines.add(new Line((selected ? "> " : "  ") + parameter.label() + ": " + parameter.value(offset), selected ? YELLOW : WHITE,
                    parameter.isMove() ? Tool.MOVE : parameter.isTurn() ? Tool.TURN : Tool.SCALE));
        }
        lines.add(new Line(Component.translatable("hud.apeira.dev.controls").getString(), GREY, null));
        lines.add(new Line(Component.translatable("hud.apeira.dev.back_export", ApeiraDevClient.OPEN_EDITOR.getTranslatedKeyMessage()).getString(),
                GREY, Tool.EXPORT));
        lines.add(new Line(Component.translatable("hud.apeira.dev.back_reset", ApeiraDevClient.OPEN_EDITOR.getTranslatedKeyMessage()).getString(),
                GREY, Tool.RESET));
    }

    private static @Nullable Tool tool(TuningParameter parameter) {
        return switch (parameter) {
            case MODE -> Tool.EDITOR;
            case SEATS -> Tool.ADD;
            case SEAT_X, SEAT_Y, SEAT_Z -> Tool.MOVE;
            case RESTORE -> Tool.UNDO;
            default -> null;
        };
    }

    private record Line(String text, int color, @Nullable Tool tool) {}
}
