package com.apeira.main.client.toolskin;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.toolskin.SalvagingTableMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The salvaging table's screen, Origin Realms' "Salvage" menu as captured: vanilla's 3-row chest (176x168, the top 71 pixels
 * and the player inventory part of {@code generic_54}), then the server's {@code menus/salvaging/background.png}
 * ({@code textures/gui/salvaging/background.png}, 256x256; it is the title glyph at ascent 19, so its top is 6 pixels above the
 * frame) with the "Salvage" heading, the input slot and the two outputs. The title and inventory label are blank on the
 * server, so none are drawn.
 */
public class SalvagingTableScreen extends AbstractContainerScreen<SalvagingTableMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier BACKGROUND = ApeiraIds.id("textures/gui/salvaging/background.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 168;
    private static final int CHEST_TOP = 17 + 3 * 18;
    private static final int INVENTORY_V = 126;
    private static final int INVENTORY_HEIGHT = 96;
    private static final int BACKGROUND_Y = -6;

    public SalvagingTableScreen(SalvagingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, WIDTH, CHEST_TOP, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + CHEST_TOP, 0.0F, INVENTORY_V, WIDTH, INVENTORY_HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos + BACKGROUND_Y, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
