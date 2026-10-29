package com.apeira.main.client.npc;

import com.apeira.main.Apeira;
import com.apeira.main.npc.NpcShop;
import com.apeira.main.npc.NpcShopMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * An NPC's shop ({@link NpcShopMenu}), drawn as Origin Realms' market menu from Ren's capture of Jeckyl's market
 * ({@code logs/apeira_ui_capture/captures/20261028-165346_class_476.json}): vanilla's 6-row chest, then the server's
 * title glyphs as textures in {@code textures/gui/npc_shop/}, where its font placed them. {@code menu_base} (the trade
 * rows, page buttons and portrait frame) and the shop's portrait ({@code portrait/<npc>.png}) are 256x256 sheets at
 * ({@code leftPos}, {@code topPos - 6}), as the glyphs' ascent 19 puts them; each row with a trade gets
 * {@code trade_allow} (affordable) or {@code trade_deny} at ({@code leftPos + 19}, {@code topPos + 17 + 18 * row}), the
 * glyphs' ascents -4 to -94; {@code page_up} and {@code page_down} (the same sheets as {@code menu_base}) light the page
 * buttons while there is a page that way. No title or inventory label is drawn, as the server's title is only those
 * glyphs. Hovering a trade's arrow shows its item's tooltip, and the page buttons say "Previous page" or "Next page",
 * in the captured {@link #PAGE_AVAILABLE} grey-blue, or dark grey when there is none.
 */
public class NpcShopScreen extends AbstractContainerScreen<NpcShopMenu> {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier MENU = Apeira.REGISTRAR.id("textures/gui/npc_shop/menu_base.png");
    private static final Identifier PAGE_UP = Apeira.REGISTRAR.id("textures/gui/npc_shop/page_up.png");
    private static final Identifier PAGE_DOWN = Apeira.REGISTRAR.id("textures/gui/npc_shop/page_down.png");
    private static final Identifier TRADE_ALLOW = Apeira.REGISTRAR.id("textures/gui/npc_shop/trade_allow.png");
    private static final Identifier TRADE_DENY = Apeira.REGISTRAR.id("textures/gui/npc_shop/trade_deny.png");
    private static final int WIDTH = 176;
    private static final int CHEST_TOP = 17 + NpcShopMenu.ROWS * 18;
    private static final int INVENTORY_V = 126;
    private static final int INVENTORY_HEIGHT = 96;
    /** Where the 256x256 sheets go, from {@code topPos}. */
    private static final int SHEET_Y = -6;
    private static final int TRADE_X = 19;
    private static final int TRADE_Y = 17;
    private static final int TRADE_WIDTH = 64;
    private static final int TRADE_HEIGHT = 18;
    /** The captured page buttons' name colour while there is a page that way (dark grey otherwise). */
    private static final int PAGE_AVAILABLE = 0xA7AEBA;

    public NpcShopScreen(NpcShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, CHEST_TOP + INVENTORY_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, WIDTH, CHEST_TOP, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + CHEST_TOP, 0.0F, INVENTORY_V, WIDTH, INVENTORY_HEIGHT, 256, 256);
        sheet(graphics, MENU);
        NpcShop shop = menu.shop();
        if (shop != null) {
            sheet(graphics, Apeira.REGISTRAR.id("textures/gui/npc_shop/portrait/" + shop.portrait() + ".png"));
        }
        if (menu.page() > 0) {
            sheet(graphics, PAGE_UP);
        }
        if (menu.page() < menu.pages() - 1) {
            sheet(graphics, PAGE_DOWN);
        }
        for (int row = 0; row < NpcShopMenu.ROWS; row++) {
            if (menu.hasTrade(row)) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, menu.canAfford(row) ? TRADE_ALLOW : TRADE_DENY, leftPos + TRADE_X,
                        topPos + TRADE_Y + row * 18, 0.0F, 0.0F, TRADE_WIDTH, TRADE_HEIGHT, 64, 64);
            }
        }
    }

    private void sheet(GuiGraphicsExtractor graphics, Identifier texture) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos + SHEET_Y, 0.0F, 0.0F, 256, 256, 256, 256);
    }

    /** A price over 64 is one ruby with the price written as its count: up to 9999 in full, then in thousands ("25k"). */
    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        long price = NpcShopMenu.shownCount(itemStack);
        super.renderSlotContents(graphics, itemStack, slot, price > 0 ? (price < 10000 ? Long.toString(price) : price / 1000 + "k") : itemCount);
    }

    /** The server's title is only its menu glyphs, and its inventory label a space. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot == null || hoveredSlot.hasItem() || hoveredSlot.index >= NpcShopMenu.ROWS * NpcShopMenu.COLUMNS
                || !menu.getCarried().isEmpty()) {
            return;
        }
        int row = hoveredSlot.index / NpcShopMenu.COLUMNS;
        int column = hoveredSlot.index % NpcShopMenu.COLUMNS;
        if (column == NpcShopMenu.ARROW_COLUMN && menu.hasTrade(row)) {
            ItemStack item = menu.tradeItem(row);
            graphics.setTooltipForNextFrame(font, getTooltipFromContainerItem(item), item.getTooltipImage(), item, mouseX, mouseY);
        } else if (column == NpcShopMenu.PAGE_COLUMN) {
            boolean up = row < NpcShopMenu.ROWS / 2;
            boolean available = up ? menu.page() > 0 : menu.page() < menu.pages() - 1;
            Component name = Component.translatable(up ? "container.apeira.npc_shop.previous_page" : "container.apeira.npc_shop.next_page");
            name = available ? name.copy().withColor(PAGE_AVAILABLE) : name.copy().withStyle(ChatFormatting.DARK_GRAY);
            graphics.setTooltipForNextFrame(font, name, mouseX, mouseY);
        }
    }
}
