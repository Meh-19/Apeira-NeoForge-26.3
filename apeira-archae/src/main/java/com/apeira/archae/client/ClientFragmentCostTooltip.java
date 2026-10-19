package com.apeira.archae.client;

import com.apeira.archae.assembly.FragmentCostTooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

/** Draws a {@link FragmentCostTooltip}: the cost, then the fragment's icon after a 6 pixel gap. */
public record ClientFragmentCostTooltip(FragmentCostTooltip tooltip) implements ClientTooltipComponent {
    private static final int ICON = 16;
    private static final int GAP = 6;

    private Component text() {
        return Component.translatable("container.apeira.assembly_table.cost", tooltip.cost());
    }

    @Override
    public int getHeight(Font font) {
        return ICON + 2;
    }

    @Override
    public int getWidth(Font font) {
        return font.width(text()) + GAP + ICON;
    }

    @Override
    public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        graphics.text(font, text(), x, y + 5, 0xFFFFFFFF);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        graphics.item(tooltip.fragment(), x + font.width(text()) + GAP, y + 1);
    }
}
