package com.apeira.main.client.navigator;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.navigator.Navigator;
import com.apeira.main.navigator.NavigatorAction;
import com.apeira.main.navigator.NavigatorPage;
import com.apeira.main.network.NavigatorActionPayload;
import com.apeira.main.network.OpenNavigatorPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The navigator, a copy of Origin Realms' {@code /navigator} menu. The server drew it as a 9x6 chest whose title holds
 * the menu's panels as glyphs of a pack font and whose slots hold invisible buttons; this screen draws the same chest,
 * title (the {@code apeira:navigator} font, made from the captured {@code menus/navigator/home} and
 * {@code menus/realms/*} fonts) and slot highlight at the captured positions, and sends each click to the server as a
 * {@link NavigatorActionPayload}. The server answers page changes with {@link OpenNavigatorPayload}.
 */
public final class ApeiraNavigatorScreen extends Screen {
    private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_HIGHLIGHT_BACK = Identifier.withDefaultNamespace("container/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT = Identifier.withDefaultNamespace("container/slot_highlight_front");
    private static final Style GLYPHS = Style.EMPTY.withFont(new FontDescription.Resource(ApeiraIds.id("navigator"))).withColor(0xFFFFFF);
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 222;
    private static final int GREY = 0xA7AEBA;
    private static final int WHITE = 0xFFFFFF;
    /** The font's characters: the panels and the space that moves back over the last one. */
    private static final String START = "";
    private static final String BACK = "";
    private static final String NAVIGATION = "" + BACK + "" + BACK + "" + BACK;
    private static final Item[] OPEN_WORLD_ICONS = {Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL,
            Items.BLUE_WOOL, Items.PURPLE_WOOL};

    private NavigatorPage page;
    private List<Region> regions = List.of();
    private int leftPos;
    private int topPos;

    public ApeiraNavigatorScreen(NavigatorPage page) {
        super(Component.translatable("navigator.apeira.title"));
        this.page = page;
        this.regions = regions(page);
    }

    /** Opens the navigator on the payload's page, or turns an open one to it. */
    public static void onOpen(OpenNavigatorPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ApeiraNavigatorScreen screen) {
            screen.setPage(payload.page());
        } else {
            minecraft.setScreen(new ApeiraNavigatorScreen(payload.page()));
        }
    }

    public void setPage(NavigatorPage page) {
        this.page = page;
        this.regions = regions(page);
    }

    @Override
    protected void init() {
        leftPos = (width - IMAGE_WIDTH) / 2;
        topPos = (height - IMAGE_HEIGHT) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        // ContainerScreen's 6-row chest, which the server's panels cover.
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos, 0.0F, 0.0F, IMAGE_WIDTH, 6 * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, leftPos, topPos + 6 * 18 + 17, 0.0F, 126.0F, IMAGE_WIDTH, 96, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        // Title at ContainerScreen's label position (8, 6), as the server's menu title was drawn.
        graphics.text(font, title(page), leftPos + 8, topPos + 6, 0xFF404040, false);
        int[] hovered = hoveredSlot(mouseX, mouseY);
        if (hovered != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_BACK, hovered[0] - 4, hovered[1] - 4, 24, 24);
        }
        if (page == NavigatorPage.RESOURCE_WORLDS) {
            for (int i = 0; i < OPEN_WORLD_ICONS.length; i++) {
                graphics.item(new ItemStack(OPEN_WORLD_ICONS[i]), leftPos + 8 + 18 * (2 + 2 * (i % 3)), topPos + 18 + 18 * (1 + 2 * (i / 3)));
            }
            graphics.item(new ItemStack(Items.ARROW), leftPos + 8, topPos + 18 + 18 * 5);
        }
        extractInventory(graphics);
        if (hovered != null) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT, hovered[0] - 4, hovered[1] - 4, 24, 24);
        }
        Region region = regionAt(mouseX, mouseY);
        if (region != null) {
            graphics.setComponentTooltipForNextFrame(font, region.tooltip(), mouseX, mouseY);
        }
    }

    /** The player's inventory, drawn where the chest menu's inventory slots are; it can't be clicked. */
    private void extractInventory(GuiGraphicsExtractor graphics) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            int x = leftPos + 8 + 18 * (i % 9);
            int y = topPos + (i < 9 ? 197 : 139 + 18 * (i / 9 - 1));
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Region region = regionAt(event.x(), event.y());
        if (region != null) {
            ApeiraClientNetwork.sendToServer(new NavigatorActionPayload(region.action(), region.index()));
            if (region.action() == NavigatorAction.SPAWN) {
                onClose();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component title(NavigatorPage page) {
        String glyphs = switch (page) {
            case MAIN -> "" + BACK;
            case REALMS, SETTINGS -> NAVIGATION + "" + BACK;
            case ACCESS -> NAVIGATION + "" + BACK;
            case MEMBERS -> NAVIGATION + "" + BACK;
            case RESOURCE_WORLDS -> null;
        };
        return glyphs == null ? Component.translatable("navigator.apeira.open_worlds")
                : Component.literal(START + glyphs).withStyle(GLYPHS);
    }

    /** The top left of the chest or inventory slot under the mouse, as vanilla highlights it, or null. */
    private int @Nullable [] hoveredSlot(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - leftPos - 7) / 18);
        if (col < 0 || col > 8) {
            return null;
        }
        double y = mouseY - topPos;
        int slotY;
        if (y >= 17 && y < 17 + 6 * 18) {
            slotY = 18 + 18 * (int) ((y - 17) / 18);
        } else if (y >= 138 && y < 138 + 3 * 18) {
            slotY = 139 + 18 * (int) ((y - 138) / 18);
        } else if (y >= 196 && y < 214) {
            slotY = 197;
        } else {
            return null;
        }
        return new int[] {leftPos + 8 + 18 * col, topPos + slotY};
    }

    private @Nullable Region regionAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos - 7;
        double y = mouseY - topPos - 17;
        for (Region region : regions) {
            if (x >= region.col() * 18 && x < (region.col() + region.cols()) * 18 && y >= region.row() * 18
                    && y < (region.row() + region.rows()) * 18) {
                return region;
            }
        }
        return null;
    }

    /** The buttons of a page, in the chest slots the server's buttons filled (captured from Origin Realms). */
    private static List<Region> regions(NavigatorPage page) {
        return switch (page) {
            case MAIN -> List.of(
                    new Region(0, 0, 4, 4, NavigatorAction.OPEN_REALMS, 0, tooltip("realm", GREY, true)),
                    new Region(5, 0, 4, 4, NavigatorAction.OPEN_RESOURCE_WORLDS, 0, tooltip("open_world", GREY, true)),
                    new Region(0, 4, 4, 2, NavigatorAction.HOMES, 0, tooltip("homes", GREY, true)),
                    new Region(5, 4, 4, 2, NavigatorAction.SPAWN, 0, tooltip("spawn", GREY, true)));
            case REALMS -> withNavigation(page,
                    new Region(0, 1, 3, 1, NavigatorAction.SPAWN, 0, tooltip("teleport", WHITE, false)),
                    new Region(3, 1, 3, 1, NavigatorAction.OPEN_ACCESS, 0, tooltip("access", GREY, false)),
                    new Region(6, 1, 1, 1, NavigatorAction.OPEN_SETTINGS, 0, tooltip("settings", GREY, false)));
            case ACCESS, MEMBERS -> withNavigation(page,
                    new Region(0, 1, 2, 1, NavigatorAction.OPEN_ACCESS, 0, tooltip("in_realm", page == NavigatorPage.ACCESS ? WHITE : GREY, false)),
                    new Region(2, 1, 2, 1, NavigatorAction.OPEN_MEMBERS, 0, tooltip("members", page == NavigatorPage.MEMBERS ? WHITE : GREY, false)));
            case SETTINGS -> withNavigation(page);
            case RESOURCE_WORLDS -> {
                Region[] worlds = new Region[Navigator.OPEN_WORLDS.length + 1];
                for (int i = 0; i < Navigator.OPEN_WORLDS.length; i++) {
                    worlds[i] = new Region(2 + 2 * (i % 3), 1 + 2 * (i / 3), 1, 1, NavigatorAction.OPEN_WORLD, i,
                            List.<Component>of(Component.translatable("navigator.apeira.open_world." + Navigator.OPEN_WORLDS[i]).withColor(WHITE)));
                }
                worlds[worlds.length - 1] = new Region(0, 5, 1, 1, NavigatorAction.OPEN_MAIN, 0, tooltip("back", GREY, false));
                yield List.of(worlds);
            }
        };
    }

    /** The navigation bar's back button (slot 0) and Realms tab (slot 4), then the page's own buttons. */
    private static List<Region> withNavigation(NavigatorPage page, Region... buttons) {
        Region back = new Region(0, 0, 1, 1, page.parent() == NavigatorPage.MAIN ? NavigatorAction.OPEN_MAIN : NavigatorAction.OPEN_REALMS, 0,
                tooltip("back", GREY, false));
        Region realms = new Region(4, 0, 1, 1, NavigatorAction.OPEN_REALMS, 0, tooltip("realms", WHITE, false));
        return Stream.concat(Stream.of(back, realms), Stream.of(buttons)).toList();
    }

    /** A button's name, with its captured colour, and its grey description line when it has one. */
    private static List<Component> tooltip(String key, int color, boolean description) {
        Component name = Component.translatable("navigator.apeira." + key).withColor(color);
        return description ? List.of(name, Component.translatable("navigator.apeira." + key + ".description").withColor(GREY)) : List.of(name);
    }

    /** A button covering {@code cols} x {@code rows} chest slots from slot ({@code col}, {@code row}). */
    private record Region(int col, int row, int cols, int rows, NavigatorAction action, int index, List<Component> tooltip) {}
}
