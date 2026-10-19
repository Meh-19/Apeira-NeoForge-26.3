package com.apeira.decor.client;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.menu.PaintingTableMenu;
import com.apeira.decor.paint.FinishType;
import com.apeira.decor.paint.Paint;
import com.apeira.decor.paint.PaintColor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The painting table's screen: a 3x3 grid of colours with a row of shades below, or, after clicking the paint icon,
 * the same row filled with finishes over two pages. Layout and textures are CosmoLib's.
 *
 * <p>Finish buttons are drawn as CosmoLib drew them: as an item painted with the finish, through the item pipeline,
 * so each shows its finish's animated effect. The item is any stack whose {@code minecraft:item_model} is
 * {@code apeira:finish_preview} (a flat paint button tinted by {@code apeira:paint}), so no item needs registering.
 */
public class PaintingTableScreen extends AbstractContainerScreen<PaintingTableMenu> {
    private static final Identifier BACKGROUND = texture("painting_table");
    private static final Identifier PAINT = texture("painting_table_paint");
    private static final Identifier SELECTED = texture("painting_table_selected");
    private static final Identifier FINISHES_OFF = texture("painting_table_finishes_off");
    private static final Identifier FINISHES_ON = texture("painting_table_finishes_on");
    private static final Identifier ARROW_LEFT_OFF = texture("painting_table_arrow_left_off");
    private static final Identifier ARROW_LEFT_ON = texture("painting_table_arrow_left_on");
    private static final Identifier ARROW_RIGHT_OFF = texture("painting_table_arrow_right_off");
    private static final Identifier ARROW_RIGHT_ON = texture("painting_table_arrow_right_on");

    private static final int WIDTH = 175;
    private static final int HEIGHT = 200;
    /** The overlays are 256x256 sheets whose content starts 6 pixels down. */
    private static final int OVERLAY_SIZE = 256;
    private static final int OVERLAY_OFFSET = 6;

    /** The colour grid, relative to the panel. */
    private static final int[][] COLOR_POSITIONS = {{62, 24}, {80, 24}, {98, 24}, {62, 42}, {80, 42}, {98, 42}, {62, 60}, {80, 60}, {98, 60}};
    /** The shade row, which shows the finishes in finish mode. */
    private static final int[] ROW_X = {26, 44, 62, 80, 98, 116, 134};
    private static final int ROW_Y = 96;
    /** The paint icon that switches between colours and finishes. */
    private static final int TOGGLE_X = 79;
    private static final int TOGGLE_Y = 84;
    private static final int TOGGLE_WIDTH = 18;
    private static final int TOGGLE_HEIGHT = 13;
    private static final int ARROW_LEFT_X = 14;
    private static final int ARROW_RIGHT_X = 152;
    private static final int ARROW_Y = 102;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 16;
    private static final int BUTTON_SIZE = 16;
    /** The item definition of a finish button, {@code assets/apeira/items/finish_preview.json}. */
    private static final Identifier FINISH_PREVIEW = ApeiraIds.id("finish_preview");

    public PaintingTableScreen(PaintingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    private static Identifier texture(String name) {
        return ApeiraIds.id("textures/gui/" + name + ".png");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        overlay(graphics, menu.isFinishMode() ? FINISHES_ON : FINISHES_OFF);
        if (menu.isFinishMode()) {
            overlay(graphics, menu.finishPage() > 0 ? ARROW_LEFT_ON : ARROW_LEFT_OFF);
            overlay(graphics, menu.finishPage() < PaintingTableMenu.LAST_PAGE ? ARROW_RIGHT_ON : ARROW_RIGHT_OFF);
            finishes(graphics, mouseX, mouseY);
        } else {
            colors(graphics);
            shades(graphics);
        }
    }

    private void overlay(GuiGraphicsExtractor graphics, Identifier texture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos + OVERLAY_OFFSET, 0.0F, OVERLAY_OFFSET, imageWidth,
                imageHeight - OVERLAY_OFFSET, OVERLAY_SIZE, OVERLAY_SIZE);
    }

    private void colors(GuiGraphicsExtractor graphics) {
        PaintColor selected = menu.selectedColor();
        PaintColor[] colors = PaintColor.values();
        for (int i = 0; i < colors.length; i++) {
            button(graphics, leftPos + COLOR_POSITIONS[i][0], topPos + COLOR_POSITIONS[i][1], colors[i].shade(colors[i].defaultShade()),
                    colors[i] == selected);
        }
    }

    private void shades(GuiGraphicsExtractor graphics) {
        PaintColor color = menu.selectedColor();
        if (!menu.hasInput() || color == null) {
            return;
        }
        for (int i = 0; i < PaintColor.SHADES; i++) {
            button(graphics, leftPos + ROW_X[i], topPos + ROW_Y, color.shade(i), i == menu.selectedShade());
        }
    }

    private void finishes(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!menu.hasInput()) {
            return;
        }
        for (int i = 0; i < PaintingTableMenu.FINISHES_PER_PAGE; i++) {
            FinishType finish = menu.finishInSlot(i);
            if (finish == null) {
                continue;
            }
            int x = leftPos + ROW_X[i];
            int y = topPos + ROW_Y;
            if (i == menu.selectedFinishSlot()) {
                selection(graphics, x, y);
            }
            graphics.item(finishPreview(finish), x, y);
            if (isOver(mouseX, mouseY, x, y, BUTTON_SIZE, BUTTON_SIZE)) {
                graphics.setTooltipForNextFrame(font, Paint.describe(finish.paintValue()), mouseX, mouseY);
            }
        }
    }

    /** A paint button painted with {@code finish}, which the item renderer draws with the finish's effect. */
    private static ItemStack finishPreview(FinishType finish) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, FINISH_PREVIEW);
        Paint.set(stack, finish.paintValue());
        return stack;
    }

    private static void selection(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SELECTED, x - 2, y - 2, 0.0F, 0.0F, 20, 20, 20, 20);
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, int rgb, boolean selected) {
        if (selected) {
            selection(graphics, x, y);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, PAINT, x, y, 0.0F, 0.0F, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE,
                ARGB.opaque(rgb));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // The inventory label is left out: it would overlap the shade row.
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = buttonAt(event.x(), event.y());
        if (button >= 0 && minecraft.gameMode != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The menu button under the mouse, or -1. */
    private int buttonAt(double mouseX, double mouseY) {
        if (isOver(mouseX, mouseY, leftPos + TOGGLE_X, topPos + TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            return PaintingTableMenu.BUTTON_FINISH_TOGGLE;
        }
        if (menu.isFinishMode()) {
            if (menu.finishPage() > 0 && isOver(mouseX, mouseY, leftPos + ARROW_LEFT_X, topPos + ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
                return PaintingTableMenu.BUTTON_PAGE_LEFT;
            }
            if (menu.finishPage() < PaintingTableMenu.LAST_PAGE
                    && isOver(mouseX, mouseY, leftPos + ARROW_RIGHT_X, topPos + ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
                return PaintingTableMenu.BUTTON_PAGE_RIGHT;
            }
            for (int i = 0; menu.hasInput() && i < PaintingTableMenu.FINISHES_PER_PAGE; i++) {
                if (menu.finishInSlot(i) != null && isOver(mouseX, mouseY, leftPos + ROW_X[i], topPos + ROW_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                    return PaintingTableMenu.BUTTON_FINISH + i;
                }
            }
            return -1;
        }
        for (int i = 0; i < COLOR_POSITIONS.length; i++) {
            if (isOver(mouseX, mouseY, leftPos + COLOR_POSITIONS[i][0], topPos + COLOR_POSITIONS[i][1], BUTTON_SIZE, BUTTON_SIZE)) {
                return i;
            }
        }
        for (int i = 0; menu.hasInput() && menu.selectedColor() != null && i < PaintColor.SHADES; i++) {
            if (isOver(mouseX, mouseY, leftPos + ROW_X[i], topPos + ROW_Y, BUTTON_SIZE, BUTTON_SIZE)) {
                return PaintColor.values().length + i;
            }
        }
        return -1;
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
