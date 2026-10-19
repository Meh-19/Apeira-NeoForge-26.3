package com.apeira.archae.client;

import java.util.List;
import java.util.Optional;

import com.apeira.archae.assembly.AssemblyRecipe;
import com.apeira.archae.assembly.AssemblyTableMenu;
import com.apeira.archae.assembly.FragmentCostTooltip;
import com.apeira.core.registry.ApeiraIds;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

/**
 * Argon's assembly table screen ({@code textures/gui/assembly_table.png}): the input fragment's recipes in a grid of
 * three per row, each tile selected, unselected or greyed out when there are too few fragments, with the cost over the
 * hovered tile and Argon's hints over the info icon at the top right. Clicking a tile picks it.
 */
public class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {
    private static final Identifier BACKGROUND = texture("assembly_table");
    private static final Identifier SELECT = texture("artifact_select");
    private static final Identifier DESELECT = texture("artifact_deselect");
    private static final Identifier DISABLED = texture("artifact_disabled");

    private static final int WIDTH = 176;
    private static final int HEIGHT = 166;
    private static final int GRID_X = 61;
    private static final int GRID_Y = 8;
    private static final int TILE = 18;
    private static final int PER_ROW = 3;
    private static final int INFO_X = 152;
    private static final int INFO_Y = 8;
    private static final int INFO_SIZE = 16;
    private static final List<Component> INFO = List.of(
            Component.translatable("container.apeira.assembly_table").withStyle(ChatFormatting.GOLD),
            Component.translatable("container.apeira.assembly_table.info1"),
            Component.translatable("container.apeira.assembly_table.info2"));

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    private static Identifier texture(String name) {
        return ApeiraIds.id("textures/gui/" + name + ".png");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        List<AssemblyRecipe> recipes = menu.recipes();
        for (int i = 0; i < recipes.size(); i++) {
            AssemblyRecipe recipe = recipes.get(i);
            int x = tileX(i);
            int y = tileY(i);
            Identifier tile = menu.fragmentCount() < recipe.cost() ? DISABLED : i == menu.selectedIndex() ? SELECT : DESELECT;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tile, x, y, 0.0F, 0.0F, TILE, TILE, TILE, TILE);
            graphics.item(recipe.assemble(), x + 1, y + 1);
            if (isOver(mouseX, mouseY, x, y, TILE, TILE)) {
                graphics.setTooltipForNextFrame(font, List.of(),
                        Optional.of(new FragmentCostTooltip(recipe.cost(), recipe.fragment().getDefaultInstance())), mouseX, mouseY);
            }
        }
        if (isOver(mouseX, mouseY, leftPos + INFO_X, topPos + INFO_Y, INFO_SIZE, INFO_SIZE)) {
            graphics.setTooltipForNextFrame(font, INFO, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, 0xFF404040, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < menu.recipes().size(); i++) {
            if (isOver(event.x(), event.y(), tileX(i), tileY(i), TILE, TILE) && minecraft.player != null && minecraft.gameMode != null
                    && menu.clickMenuButton(minecraft.player, i)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int tileX(int index) {
        return leftPos + GRID_X + index % PER_ROW * TILE;
    }

    private int tileY(int index) {
        return topPos + GRID_Y + index / PER_ROW * TILE;
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
