package com.apeira.main.client.farming;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.farming.FarmCrop;
import com.apeira.main.farming.FarmingLore;
import com.apeira.main.farming.Fertilizer;
import com.apeira.main.farming.PlanterBoxBlockEntity;
import com.apeira.main.farming.PlanterBoxMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The planter box's screen: Origin Realms' {@code menu_container.png} panel (256x256, its top border 18 pixels down the
 * sheet, so the sheet is drawn 18 above the frame) with no chest rows under it, and the player inventory grid of
 * vanilla's {@code generic_54} (from its slot cells down) right where the panel opens, so the three stock slots line up
 * with the inventory's columns. The panel's slot holes get vanilla's slot cell behind them. The panel has the three
 * stock slots, the planter and the soil, water and fertilizer icons, each lit by its overlay ({@code menu_soiled}, {@code menu_watered},
 * {@code menu_fert_<type>}) while the crop has it. The crop's picture of its stage stands in the planter. Hovering the
 * picture, an icon or the question mark explains it.
 */
public class PlanterBoxScreen extends AbstractContainerScreen<PlanterBoxMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier MENU = ApeiraIds.id("textures/gui/farming/menu_container.png");
    private static final Identifier SOILED = ApeiraIds.id("textures/gui/farming/menu_soiled.png");
    private static final Identifier WATERED = ApeiraIds.id("textures/gui/farming/menu_watered.png");
    private static final int WIDTH = 176;
    /** Where {@code generic_54}'s inventory slot cells start, and how much of the sheet is left from there. */
    private static final int INVENTORY_V = 139;
    private static final int INVENTORY_HEIGHT = 222 - INVENTORY_V;
    private static final int HEIGHT = PlanterBoxMenu.PANEL_HEIGHT + INVENTORY_HEIGHT;
    /** Where the 256x256 layers go, from {@code topPos}: the panel's top border is 18 rows down the sheet. */
    private static final int MENU_Y = -18;
    /** The 64x64 crop picture, its trunk's foot on the planter's soil. */
    private static final int PICTURE_X = 56;
    private static final int PICTURE_Y = -3;
    private static final int PICTURE_SIZE = 64;
    // Icon boxes in the frame (texture position less the 18-pixel lift).
    private static final int ICON_X = 115;
    private static final int ICON_W = 17;
    private static final int SOIL_Y = 25;
    private static final int WATER_Y = 40;
    private static final int FERTILIZER_Y = 59;
    private static final int ICON_H = 15;
    private static final int PLANTER_X = 70;
    private static final int PLANTER_Y = 56;
    private static final int INFO_X = 152;
    private static final int INFO_Y = 6;
    private static final int INFO_SIZE = 16;
    private static final List<Component> INFO = List.of(
            Component.translatable("container.apeira.planter_box.info"),
            FarmingLore.line("container.apeira.planter_box.info1"),
            FarmingLore.line("container.apeira.planter_box.info2"),
            FarmingLore.line("container.apeira.planter_box.info3"),
            FarmingLore.line("container.apeira.planter_box.info4"));

    public PlanterBoxScreen(PlanterBoxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + PlanterBoxMenu.PANEL_HEIGHT, 0.0F, INVENTORY_V, WIDTH,
                INVENTORY_HEIGHT, 256, 256);
        for (int i = 0; i < PlanterBoxBlockEntity.SLOTS; i++) {
            // The inside of vanilla's slot cell, behind the panel's 16x16 hole.
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos + PlanterBoxMenu.SLOT_X, topPos + PlanterBoxMenu.SLOT_Y + i * 18,
                    8.0F, 18.0F, 16, 16, 256, 256);
        }
        layer(graphics, MENU);
        FarmCrop crop = menu.crop();
        boolean soiled = menu.has(PlanterBoxBlockEntity.FLAG_SOILED);
        boolean wet = menu.has(PlanterBoxBlockEntity.FLAG_WET);
        Fertilizer fertilizer = menu.fertilizer();
        if (soiled) {
            layer(graphics, SOILED);
        }
        if (wet) {
            layer(graphics, WATERED);
        }
        if (fertilizer != null) {
            layer(graphics, fertilizer.menuOverlay());
        }
        boolean golden = menu.has(PlanterBoxBlockEntity.FLAG_GOLDEN);
        if (crop != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, crop.picture(Math.min(Math.max(menu.stage(), 1), crop.stages()), golden),
                    leftPos + PICTURE_X, topPos + PICTURE_Y, 0.0F, 0.0F, PICTURE_SIZE, PICTURE_SIZE, PICTURE_SIZE, PICTURE_SIZE);
        }

        List<Component> tooltip = null;
        if (isHovering(INFO_X, INFO_Y, INFO_SIZE, INFO_SIZE, mouseX, mouseY)) {
            tooltip = INFO;
        } else if (isHovering(ICON_X, SOIL_Y, ICON_W, ICON_H, mouseX, mouseY)) {
            tooltip = List.of(Component.translatable(soiled ? "container.apeira.planter_box.soiled" : "container.apeira.planter_box.not_soiled")
                    .withStyle(soiled ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        } else if (isHovering(ICON_X, WATER_Y, ICON_W, ICON_H + 4, mouseX, mouseY)) {
            tooltip = List.of(Component.translatable(wet ? "container.apeira.planter_box.watered" : "container.apeira.planter_box.not_watered")
                    .withStyle(wet ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        } else if (isHovering(ICON_X, FERTILIZER_Y, ICON_W, ICON_H + 1, mouseX, mouseY)) {
            tooltip = List.of(fertilizer == null
                    ? Component.translatable("container.apeira.planter_box.no_fertilizer").withStyle(ChatFormatting.GRAY)
                    : Component.translatable(fertilizer.translationKey()).withStyle(ChatFormatting.GREEN));
        } else if (isHovering(PICTURE_X + 14, PICTURE_Y + 10, PICTURE_SIZE - 28, PLANTER_Y + 22 - PICTURE_Y - 10, mouseX, mouseY)) {
            tooltip = cropTooltip(crop, golden);
        }
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(font, tooltip, Optional.empty(), mouseX, mouseY);
        }
    }

    private List<Component> cropTooltip(@Nullable FarmCrop crop, boolean golden) {
        List<Component> lines = new ArrayList<>();
        if (crop == null) {
            lines.add(Component.translatable("container.apeira.planter_box.empty"));
            lines.add(FarmingLore.line("container.apeira.planter_box.empty1"));
            return lines;
        }
        lines.add(Component.translatable(crop.translationKey()).withStyle(golden ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        if (menu.has(PlanterBoxBlockEntity.FLAG_RIPE)) {
            lines.add(Component.translatable(golden ? "container.apeira.planter_box.ripe_golden" : "container.apeira.planter_box.ripe")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            lines.add(FarmingLore.line("container.apeira.planter_box.growing", menu.stage() - 1, crop.days()));
            if (!menu.has(PlanterBoxBlockEntity.FLAG_WET)) {
                lines.add(Component.translatable("container.apeira.planter_box.needs_water").withStyle(ChatFormatting.AQUA));
            }
        }
        return lines;
    }

    private void layer(GuiGraphicsExtractor graphics, Identifier texture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos + MENU_Y, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    /** Origin Realms' title and inventory label are blank. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}
}
