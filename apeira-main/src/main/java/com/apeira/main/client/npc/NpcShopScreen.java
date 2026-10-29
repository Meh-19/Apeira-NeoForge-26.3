package com.apeira.main.client.npc;

import com.apeira.main.npc.NpcShopMenu;
import com.apeira.main.ruby.Rubies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * An NPC's shop ({@link NpcShopMenu}), drawn as vanilla's 3-row chest ({@code generic_54}'s top and its player inventory
 * part) with the NPC's name as the title and the player's ruby balance at the title row's right end. The offers' lore
 * gives their prices and how to buy them.
 */
public class NpcShopScreen extends AbstractContainerScreen<NpcShopMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int WIDTH = 176;
    private static final int CHEST_TOP = 17 + NpcShopMenu.ROWS * 18;
    private static final int INVENTORY_V = 126;
    private static final int INVENTORY_HEIGHT = 96;
    private static final int LABEL = 0xFF404040;

    public NpcShopScreen(NpcShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, CHEST_TOP + INVENTORY_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, WIDTH, CHEST_TOP, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + CHEST_TOP, 0.0F, INVENTORY_V, WIDTH, INVENTORY_HEIGHT, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        long rubies = minecraft.player == null ? 0 : Rubies.get(minecraft.player);
        Component balance = Component.translatable("container.apeira.npc_shop.balance", Rubies.format(rubies));
        graphics.text(font, balance, imageWidth - 8 - font.width(balance), titleLabelY, LABEL, false);
    }
}
