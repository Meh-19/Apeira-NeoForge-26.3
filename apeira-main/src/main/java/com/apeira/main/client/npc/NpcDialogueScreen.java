package com.apeira.main.client.npc;

import java.util.List;

import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcDialoguePayload;
import com.apeira.main.npc.NpcEntity;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * An NPC's dialogue, after Undertale's and Deltarune's text boxes: a black box along the bottom of the screen in the
 * framed border ({@code textures/gui/dialogue/border.png}, a nine-slice with {@link #EDGE}-pixel edges), the NPC's
 * portrait ({@code textures/gui/dialogue/portrait/<npc>.png}, 42x42) on its left, and beside it the NPC's name in bold
 * white after the speech glyph ꢔ of the {@code apeira:dialogue} font, over its line, typed out
 * {@link #MILLIS_PER_CHAR} a character. A click, Space, Enter or F shows the whole line at once, then closes; Escape
 * closes. The game keeps running behind it, and {@link NpcDialogueCamera} keeps the view on the NPC while it is open.
 */
public class NpcDialogueScreen extends Screen {
    private static final Identifier BORDER = Apeira.REGISTRAR.id("textures/gui/dialogue/border.png");
    private static final int BORDER_SIZE = 50;
    private static final int EDGE = 4;
    private static final FontDescription DIALOGUE_FONT = new FontDescription.Resource(Apeira.REGISTRAR.id("dialogue"));
    private static final int PORTRAIT_SIZE = 42;
    private static final int BOX_HEIGHT = 66;
    private static final int MAX_WIDTH = 340;
    private static final int MARGIN = 12;
    private static final int PADDING = 12;
    private static final int LINE_HEIGHT = 11;
    private static final long MILLIS_PER_CHAR = 30;
    private static final int FILL = 0xF0000000;
    private static final int WHITE = 0xFFFFFFFF;

    private final int npc;
    private final Component name;
    private final Identifier portrait;
    private final Component line;
    private List<String> rows = List.of();
    private int length;
    private final long opened = Util.getMillis();
    private boolean skipped;

    public NpcDialogueScreen(NpcEntity npc, Component line) {
        super(npc.getDisplayName());
        this.npc = npc.getId();
        this.name = npc.getType().getDescription();
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType());
        this.portrait = Identifier.fromNamespaceAndPath(type.getNamespace(), "textures/gui/dialogue/portrait/" + type.getPath() + ".png");
        this.line = line;
    }

    public static void onOpen(NpcDialoguePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.npc()) instanceof NpcEntity entity) {
            minecraft.gui.setScreen(new NpcDialogueScreen(entity, Component.translatable(payload.line())));
            NpcDialogueCamera.focus(payload.npc());
        }
    }

    @Override
    protected void init() {
        // The line is wrapped once to the text's width, then typed out row by row.
        rows = font.splitIgnoringLanguage(line, boxWidth() - PADDING * 2 - PORTRAIT_SIZE - PADDING).stream().map(FormattedText::getString).toList();
        length = rows.stream().mapToInt(String::length).sum();
    }

    @Override
    public void tick() {
        if (NpcDialogueCamera.target() == null || NpcDialogueCamera.target().getId() != npc) {
            onClose();
        }
    }

    private int boxWidth() {
        return Math.min(width - MARGIN * 2, MAX_WIDTH);
    }

    private int shown() {
        return skipped ? length : (int) Math.min(length, (Util.getMillis() - opened) / MILLIS_PER_CHAR);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // No dimming or blur: the world, and the NPC, stay in view.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int w = boxWidth();
        int x = (width - w) / 2;
        int y = height - BOX_HEIGHT - MARGIN;
        graphics.fill(x + EDGE, y + EDGE, x + w - EDGE, y + BOX_HEIGHT - EDGE, FILL);
        border(graphics, x, y, w, BOX_HEIGHT);
        int portraitY = y + (BOX_HEIGHT - PORTRAIT_SIZE) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, portrait, x + PADDING, portraitY, 0.0F, 0.0F, PORTRAIT_SIZE, PORTRAIT_SIZE, PORTRAIT_SIZE,
                PORTRAIT_SIZE);
        int textX = x + PADDING + PORTRAIT_SIZE + PADDING;
        Component title = Component.literal("ꢔ").withStyle(style -> style.withFont(DIALOGUE_FONT))
                .append(Component.literal(" ").append(name.copy()).withStyle(ChatFormatting.BOLD));
        graphics.text(font, title, textX, y + PADDING, WHITE, true);
        int left = shown();
        int rowY = y + PADDING + LINE_HEIGHT + 3;
        for (String row : rows) {
            if (left <= 0) {
                break;
            }
            graphics.text(font, row.substring(0, Math.min(row.length(), left)), textX, rowY, WHITE, true);
            left -= row.length();
            rowY += LINE_HEIGHT;
        }
        if (shown() >= length && (Util.getMillis() / 400) % 2 == 0) {
            // A small blinking arrow: the line is done, press on.
            int ax = x + w - PADDING - 5;
            int ay = y + BOX_HEIGHT - PADDING - 3;
            graphics.fill(ax, ay, ax + 5, ay + 1, WHITE);
            graphics.fill(ax + 1, ay + 1, ax + 4, ay + 2, WHITE);
            graphics.fill(ax + 2, ay + 2, ax + 3, ay + 3, WHITE);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    /** The nine-slice frame: corners as they are, edges stretched along the box. */
    private static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        int mid = BORDER_SIZE - EDGE * 2;
        int[][] columns = {{x, 0, EDGE}, {x + EDGE, EDGE, w - EDGE * 2}, {x + w - EDGE, BORDER_SIZE - EDGE, EDGE}};
        int[][] rows = {{y, 0, EDGE}, {y + EDGE, EDGE, h - EDGE * 2}, {y + h - EDGE, BORDER_SIZE - EDGE, EDGE}};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) {
                    continue;
                }
                int[] column = columns[i];
                int[] row = rows[j];
                graphics.blit(RenderPipelines.GUI_TEXTURED, BORDER, column[0], row[0], column[1], row[1], column[2], row[2], i == 1 ? mid : EDGE,
                        j == 1 ? mid : EDGE, BORDER_SIZE, BORDER_SIZE);
            }
        }
    }

    /** Shows the rest of the line, or closes once it is all shown. */
    private void advance() {
        if (shown() < length) {
            skipped = true;
        } else {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_RETURN || key == InputConstants.KEY_F) {
            advance();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        NpcDialogueCamera.release();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
