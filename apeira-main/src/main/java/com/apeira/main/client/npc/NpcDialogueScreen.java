package com.apeira.main.client.npc;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcDialoguePayload;
import com.apeira.main.npc.NpcEntity;
import com.apeira.main.npc.OpenNpcShopPayload;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * An NPC's dialogue, after Undertale's and Deltarune's text boxes: a black box with a 1-pixel white outline along the
 * bottom of the screen ({@link #MAX_WIDTH} by {@link #BOX_HEIGHT} GUI pixels at most), the NPC's portrait
 * ({@code textures/gui/dialogue/portrait/<npc>.png}, 42x42) on its left inside the framed border
 * ({@code textures/gui/dialogue/border.png}, 50x50 drawn 1:1, its {@link #EDGE}-pixel band round the portrait), and
 * beside it the NPC's name in bold white after the speech glyph ꢔ (U+A894, added to {@code minecraft:default}), over its line,
 * typed out {@link #MILLIS_PER_CHAR} a character. A left click, Space, Enter or F shows the whole line at once, then
 * closes; Escape or the sneak key closes it at any time. For an NPC with a shop, a right click (clicking the NPC again)
 * asks the server to open the shop ({@link OpenNpcShopPayload}), and a grey hint over the box's right end says so. The game keeps
 * running behind it, and {@link NpcDialogueCamera} keeps the view on the NPC while it is open.
 */
public class NpcDialogueScreen extends Screen {
    private static final Identifier BORDER = Apeira.REGISTRAR.id("textures/gui/dialogue/border.png");
    private static final int BORDER_SIZE = 50;
    private static final int EDGE = 4;
    private static final int PORTRAIT_SIZE = 42;
    private static final int BOX_HEIGHT = 66;
    private static final int MAX_WIDTH = 340;
    private static final int MARGIN = 12;
    /** From the box's edge to the portrait frame, and between the frame and the text. */
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 11;
    private static final long MILLIS_PER_CHAR = 12;
    private static final int FILL = 0xF0000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int HINT = 0xFFAAAAAA;

    private final int npc;
    private final Component name;
    /** The NPC's portrait, or null when it has none yet (the border's opening is left dark). */
    private final @Nullable Identifier portrait;
    private final Component line;
    /** Whether right-clicking opens the NPC's shop. */
    private final boolean shop;
    private List<String> rows = List.of();
    private int length;
    private final long opened = Util.getMillis();
    private boolean skipped;

    public NpcDialogueScreen(NpcEntity npc, Component line, boolean shop) {
        super(npc.getDisplayName());
        this.npc = npc.getId();
        this.name = npc.getType().getDescription();
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType());
        Identifier portrait = Identifier.fromNamespaceAndPath(type.getNamespace(), "textures/gui/dialogue/portrait/" + type.getPath() + ".png");
        this.portrait = Minecraft.getInstance().getResourceManager().getResource(portrait).isPresent() ? portrait : null;
        this.line = line;
        this.shop = shop;
    }

    public static void onOpen(NpcDialoguePayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.npc()) instanceof NpcEntity entity) {
            minecraft.gui.setScreen(new NpcDialogueScreen(entity, Component.translatable(payload.line()), payload.shop()));
            NpcDialogueCamera.focus(payload.npc());
        }
    }

    @Override
    protected void init() {
        // The line is wrapped once to the text's width, then typed out row by row.
        rows = font.splitIgnoringLanguage(line, boxWidth() - PADDING * 3 - BORDER_SIZE).stream().map(FormattedText::getString).toList();
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
        graphics.fill(x, y, x + w, y + BOX_HEIGHT, FILL);
        graphics.fill(x, y, x + w, y + 1, WHITE);
        graphics.fill(x, y + BOX_HEIGHT - 1, x + w, y + BOX_HEIGHT, WHITE);
        graphics.fill(x, y + 1, x + 1, y + BOX_HEIGHT - 1, WHITE);
        graphics.fill(x + w - 1, y + 1, x + w, y + BOX_HEIGHT - 1, WHITE);
        // The portrait sits in the border's 42x42 opening, the border drawn over it.
        int frameX = x + PADDING;
        int frameY = y + PADDING;
        if (portrait != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, portrait, frameX + EDGE, frameY + EDGE, 0.0F, 0.0F, PORTRAIT_SIZE, PORTRAIT_SIZE,
                    PORTRAIT_SIZE, PORTRAIT_SIZE);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, BORDER, frameX, frameY, 0.0F, 0.0F, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE);
        int textX = frameX + BORDER_SIZE + PADDING;
        // All in the default font, which has ꢔ (assets/minecraft/font/default.json): a child component inherits its
        // parent's font, so a separate glyph font round the name turned every letter into a missing-glyph box.
        Component title = Component.literal("\uA894 ").append(name.copy().withStyle(ChatFormatting.BOLD));
        graphics.text(font, title, textX, frameY + EDGE, WHITE, true);
        int left = shown();
        int rowY = frameY + EDGE + LINE_HEIGHT + 3;
        for (String row : rows) {
            if (left <= 0) {
                break;
            }
            graphics.text(font, row.substring(0, Math.min(row.length(), left)), textX, rowY, WHITE, true);
            left -= row.length();
            rowY += LINE_HEIGHT;
        }
        if (shop) {
            Component hint = Component.translatable("dialogue.apeira.hint.shop");
            graphics.text(font, hint, x + w - font.width(hint), y - LINE_HEIGHT, HINT, true);
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
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (shop) {
                // The server opens the shop's menu, which takes this screen's place.
                ApeiraClientNetwork.sendToServer(new OpenNpcShopPayload(npc));
            }
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyShift.matches(event)) {
            onClose();
            return true;
        }
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
