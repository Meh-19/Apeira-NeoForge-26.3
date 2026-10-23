package com.apeira.main.client.ruby;

import java.util.List;
import java.util.Optional;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.ruby.ApeiraRubyDepositMenu;
import com.apeira.main.ruby.Rubies;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The ruby deposit's screen, drawn as Origin Realms' (captured at GUI scale 4): vanilla's 4-row chest (its top 89
 * pixels and the player inventory part of {@code generic_54}), then the server pack's {@code ruby_deposit_menu.png}
 * (256x256) at 6 pixels above the frame, with its five ruby slots, the arrows into the balance box and the question
 * mark button. The balance is written in the menu's digit font at (65, 76), after the box's ruby; the question mark
 * shows "Learn more about Rubies". Origin Realms' title and inventory label are blank, so no labels are drawn.
 */
public class RubyDepositScreen extends AbstractContainerScreen<ApeiraRubyDepositMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier MENU = ApeiraIds.id("textures/gui/ruby/deposit_menu.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 186;
    /** The chest's title bar and four rows, then where its player inventory part starts in {@code generic_54}. */
    private static final int CHEST_TOP = 17 + 4 * 18;
    private static final int INVENTORY_V = 126;
    private static final int INVENTORY_HEIGHT = 96;
    private static final int MENU_Y = -6;
    private static final int BALANCE_X = 65;
    private static final int BALANCE_Y = 76;
    private static final int INFO_X = 152;
    private static final int INFO_Y = 18;
    private static final int INFO_SIZE = 16;
    private static final List<Component> INFO = List.of(
            Component.translatable("container.apeira.ruby_deposit.info"),
            Component.translatable("container.apeira.ruby_deposit.info1").withStyle(ChatFormatting.GRAY),
            Component.translatable("container.apeira.ruby_deposit.info2").withStyle(ChatFormatting.GRAY));

    public RubyDepositScreen(ApeiraRubyDepositMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, WIDTH, CHEST_TOP, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + CHEST_TOP, 0.0F, INVENTORY_V, WIDTH, INVENTORY_HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MENU, leftPos, topPos + MENU_Y, 0.0F, 0.0F, 256, 256, 256, 256);
        if (isHovering(INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, INFO, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long rubies = minecraft.player == null ? 0 : Rubies.get(minecraft.player);
        graphics.text(font, RubyCounter.menu(rubies), BALANCE_X, BALANCE_Y, 0xFFFFFFFF, false);
    }
}
