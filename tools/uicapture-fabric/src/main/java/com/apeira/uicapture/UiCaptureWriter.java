package com.apeira.uicapture;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.apeira.uicapture.mixin.AbstractContainerScreenAccessor;
import com.apeira.uicapture.mixin.ScreenAccessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Turns a recorded frame into the capture file: the screen (title, class, size, the container's image size, offsets,
 * label positions and slots with their full items), the widget tree, everything drawn in order, the glyphs of every
 * font drawn with and the copied assets. Writes {@code logs/apeira_ui_capture.json} (the latest capture) and keeps
 * each capture, with the copied assets, under {@code logs/apeira_ui_capture/}. A deep inspection (F9) adds what is
 * under the mouse, also written alone to {@code logs/apeira_ui_inspect.json}.
 */
final class UiCaptureWriter {
    static final String LATEST = "logs/apeira_ui_capture.json";
    static final String FOLDER = "logs/apeira_ui_capture";
    static final String INSPECT = "logs/apeira_ui_inspect.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_DEPTH = 12;

    private UiCaptureWriter() {}

    /** The mouse position of a deep inspection, in GUI pixels. */
    record Mouse(int x, int y) {}

    /**
     * Writes the capture of {@code screen} (null for the HUD), with a deep inspection at {@code mouse} when it is not
     * null, and returns the file written.
     */
    static Path write(Minecraft minecraft, @Nullable Screen screen, UiCapture.Frame frame, @Nullable Mouse mouse) throws IOException {
        Path game = FabricLoader.getInstance().getGameDir();
        Path folder = game.resolve(FOLDER);
        ResourceManager resources = minecraft.getResourceManager();
        CapturedAssets assets = new CapturedAssets(resources, folder);
        DynamicOps<JsonElement> ops = minecraft.level != null ? minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;

        JsonObject root = new JsonObject();
        root.addProperty("captured_at", LocalDateTime.now().toString());
        if (minecraft.getCurrentServer() != null) {
            root.addProperty("server", minecraft.getCurrentServer().ip);
        }
        Window window = minecraft.getWindow();
        root.addProperty("gui_scale", window.getGuiScale());
        root.addProperty("gui_width", window.getGuiScaledWidth());
        root.addProperty("gui_height", window.getGuiScaledHeight());
        root.addProperty("window_width", window.getWidth());
        root.addProperty("window_height", window.getHeight());
        JsonArray packs = new JsonArray();
        for (Pack pack : minecraft.getResourcePackRepository().getSelectedPacks()) {
            packs.add(pack.getId());
        }
        root.add("resource_packs", packs);

        if (screen != null) {
            root.add("screen", screen(screen, ops));
            if (screen instanceof AbstractContainerScreen<?> container) {
                root.add("container", container(container, ops));
            }
            JsonArray widgets = new JsonArray();
            Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            screen.children().forEach(child -> widget(child, widgets, seen, ops, assets, 0));
            ((ScreenAccessor) screen).apeira$renderables().forEach(renderable -> widget(renderable, widgets, seen, ops, assets, 0));
            root.add("widgets", widgets);
        } else {
            root.addProperty("screen", "hud");
        }

        for (JsonElement element : frame.drawn()) {
            JsonObject entry = element.getAsJsonObject();
            switch (entry.get("kind").getAsString()) {
                case "texture" -> assets.copy(Identifier.parse(entry.get("texture").getAsString()));
                case "sprite" -> {
                    Identifier file = assets.copy(CapturedAssets.guiSprite(Identifier.parse(entry.get("sprite").getAsString())));
                    if (file != null) {
                        entry.addProperty("file", file.toString());
                    }
                }
                case "atlas_sprite" -> {
                    Identifier file = assets.copy(CapturedAssets.atlasSprite(Identifier.parse(entry.get("atlas").getAsString()),
                            Identifier.parse(entry.get("sprite").getAsString())));
                    if (file != null) {
                        entry.addProperty("file", file.toString());
                    }
                }
                default -> {}
            }
        }
        frame.items().forEach((entry, stack) -> entry.add("item", item(stack, ops)));
        root.add("drawn", frame.drawn());
        JsonObject inspect = screen != null && mouse != null ? inspect(screen, frame, mouse, ops) : null;
        if (inspect != null) {
            root.add("inspect", inspect);
        }

        JsonObject fonts = new JsonObject();
        for (Map.Entry<Identifier, TreeSet<Integer>> font : frame.glyphs().entrySet()) {
            fonts.add(font.getKey().toString(), FontGlyphs.describe(resources, font.getKey(), font.getValue(), assets));
        }
        root.add("fonts", fonts);
        root.add("assets", assets.index());

        String name = LocalDateTime.now().format(STAMP) + "_" + slug(screen);
        root.addProperty("capture_file", FOLDER + "/captures/" + name + ".json");
        String json = GSON.toJson(root);
        Path kept = folder.resolve("captures").resolve(name + ".json");
        Files.createDirectories(kept.getParent());
        Files.writeString(kept, json, StandardCharsets.UTF_8);
        Path latest = game.resolve(LATEST);
        try (Writer writer = Files.newBufferedWriter(latest, StandardCharsets.UTF_8)) {
            writer.write(json);
        }
        if (inspect != null) {
            Path inspected = game.resolve(INSPECT);
            Files.writeString(inspected, GSON.toJson(inspect), StandardCharsets.UTF_8);
            return inspected;
        }
        return latest;
    }

    private static JsonObject screen(Screen screen, DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        json.addProperty("class", screen.getClass().getName());
        json.add("title", component(screen.getTitle(), ops));
        json.addProperty("width", screen.width);
        json.addProperty("height", screen.height);
        return json;
    }

    private static JsonObject container(AbstractContainerScreen<?> screen, DynamicOps<JsonElement> ops) {
        AbstractContainerScreenAccessor layout = (AbstractContainerScreenAccessor) screen;
        AbstractContainerMenu menu = screen.getMenu();
        JsonObject json = new JsonObject();
        json.addProperty("menu_class", menu.getClass().getName());
        try {
            json.addProperty("menu_type", String.valueOf(BuiltInRegistries.MENU.getKey(menu.getType())));
        } catch (UnsupportedOperationException e) {
            // The player's own inventory menu has no type.
        }
        json.addProperty("image_width", layout.apeira$imageWidth());
        json.addProperty("image_height", layout.apeira$imageHeight());
        json.addProperty("left_pos", layout.apeira$leftPos());
        json.addProperty("top_pos", layout.apeira$topPos());
        json.addProperty("title_label_x", layout.apeira$titleLabelX());
        json.addProperty("title_label_y", layout.apeira$titleLabelY());
        json.addProperty("inventory_label_x", layout.apeira$inventoryLabelX());
        json.addProperty("inventory_label_y", layout.apeira$inventoryLabelY());
        JsonArray slots = new JsonArray();
        for (Slot slot : menu.slots) {
            JsonObject entry = new JsonObject();
            entry.addProperty("index", slot.index);
            entry.addProperty("container", slot.container.getClass().getSimpleName());
            entry.addProperty("container_slot", slot.getContainerSlot());
            entry.addProperty("x", slot.x);
            entry.addProperty("y", slot.y);
            entry.addProperty("screen_x", layout.apeira$leftPos() + slot.x);
            entry.addProperty("screen_y", layout.apeira$topPos() + slot.y);
            entry.addProperty("active", slot.isActive());
            if (!slot.getItem().isEmpty()) {
                entry.add("item", item(slot.getItem(), ops));
            }
            slots.add(entry);
        }
        json.add("slots", slots);
        return json;
    }

    /** A widget or other screen element: class, bounds, message, state, the sprites it keeps, and its children. */
    private static void widget(Object element, JsonArray out, Set<Object> seen, DynamicOps<JsonElement> ops, CapturedAssets assets, int depth) {
        if (depth > MAX_DEPTH || !seen.add(element)) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("class", element.getClass().getName());
        if (element instanceof LayoutElement layout) {
            json.addProperty("x", layout.getX());
            json.addProperty("y", layout.getY());
            json.addProperty("width", layout.getWidth());
            json.addProperty("height", layout.getHeight());
        }
        if (element instanceof AbstractWidget widget) {
            json.add("message", component(widget.getMessage(), ops));
            json.addProperty("active", widget.active);
            json.addProperty("visible", widget.visible);
        }
        JsonObject sprites = sprites(element, assets);
        if (!sprites.isEmpty()) {
            json.add("textures", sprites);
        }
        if (element instanceof ContainerEventHandler container) {
            JsonArray children = new JsonArray();
            container.children().forEach(child -> widget(child, children, seen, ops, assets, depth + 1));
            if (!children.isEmpty()) {
                json.add("children", children);
            }
        }
        out.add(json);
    }

    /**
     * The {@link Identifier} and {@link WidgetSprites} fields of an element's own classes (not of the base widget
     * classes), static or not: the textures it may draw in states the capture did not see (hovered, disabled).
     */
    private static JsonObject sprites(Object element, CapturedAssets assets) {
        JsonObject json = new JsonObject();
        for (Class<?> type = element.getClass(); type != null && type != AbstractWidget.class && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != Identifier.class && field.getType() != WidgetSprites.class) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(Modifier.isStatic(field.getModifiers()) ? null : element);
                    if (value instanceof Identifier id) {
                        json.addProperty(field.getName(), id.toString());
                    } else if (value instanceof WidgetSprites widgetSprites) {
                        JsonObject states = new JsonObject();
                        states.addProperty("enabled", widgetSprites.enabled().toString());
                        states.addProperty("disabled", widgetSprites.disabled().toString());
                        states.addProperty("enabled_focused", widgetSprites.enabledFocused().toString());
                        states.addProperty("disabled_focused", widgetSprites.disabledFocused().toString());
                        for (Identifier sprite : Set.of(widgetSprites.enabled(), widgetSprites.disabled(),
                                widgetSprites.enabledFocused(), widgetSprites.disabledFocused())) {
                            assets.copy(CapturedAssets.guiSprite(sprite));
                        }
                        json.add(field.getName(), states);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // Not reachable (module access); the drawn list still has what it drew.
                }
            }
        }
        return json;
    }

    /**
     * The deep inspection: what is under the mouse. The hovered slot with its item, the hovered widgets (innermost
     * last) with their messages, the text drawn under the mouse and the tooltip, each with its exact text and every run's
     * font, colour and formatting.
     */
    private static JsonObject inspect(Screen screen, UiCapture.Frame frame, Mouse mouse, DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        json.addProperty("mouse_x", mouse.x());
        json.addProperty("mouse_y", mouse.y());
        if (screen instanceof AbstractContainerScreen<?> container) {
            Slot slot = ((AbstractContainerScreenAccessor) container).apeira$hoveredSlot();
            if (slot != null) {
                JsonObject entry = new JsonObject();
                entry.addProperty("index", slot.index);
                entry.addProperty("container_slot", slot.getContainerSlot());
                entry.addProperty("x", slot.x);
                entry.addProperty("y", slot.y);
                if (slot.hasItem()) {
                    entry.add("item", item(slot.getItem(), ops));
                }
                json.add("hovered_slot", entry);
            }
        }
        JsonArray widgets = new JsonArray();
        hoveredWidgets(screen.children(), mouse, widgets, ops, 0);
        json.add("hovered_widgets", widgets);

        JsonArray hovered = new JsonArray();
        JsonArray tooltip = new JsonArray();
        float[] tooltipBox = null;
        for (JsonElement element : frame.drawn()) {
            JsonObject entry = element.getAsJsonObject();
            String kind = entry.get("kind").getAsString();
            if (kind.equals("sprite") && isTooltipBackground(entry.get("sprite").getAsString())) {
                // A new tooltip background: the text drawn inside it after this is the tooltip.
                tooltipBox = box(entry);
                tooltip = new JsonArray();
            } else if (kind.equals("text")) {
                float[] box = box(entry);
                if (mouse.x() >= box[0] && mouse.x() < box[2] && mouse.y() >= box[1] && mouse.y() < box[3]) {
                    hovered.add(entry);
                }
                if (tooltipBox != null && box[0] >= tooltipBox[0] && box[0] < tooltipBox[2] && box[1] >= tooltipBox[1] && box[1] < tooltipBox[3]) {
                    tooltip.add(entry);
                }
            }
        }
        json.add("hovered_text", hovered);
        json.add("tooltip", tooltip);
        return json;
    }

    /** Widgets under the mouse, parents before their children. */
    private static void hoveredWidgets(List<? extends GuiEventListener> children, Mouse mouse, JsonArray out, DynamicOps<JsonElement> ops, int depth) {
        if (depth > MAX_DEPTH) {
            return;
        }
        for (GuiEventListener child : children) {
            if (!child.isMouseOver(mouse.x(), mouse.y())) {
                continue;
            }
            JsonObject json = new JsonObject();
            json.addProperty("class", child.getClass().getName());
            if (child instanceof LayoutElement layout) {
                json.addProperty("x", layout.getX());
                json.addProperty("y", layout.getY());
                json.addProperty("width", layout.getWidth());
                json.addProperty("height", layout.getHeight());
            }
            if (child instanceof AbstractWidget widget) {
                json.add("message", component(widget.getMessage(), ops));
                json.addProperty("active", widget.active);
            }
            out.add(json);
            if (child instanceof ContainerEventHandler container) {
                hoveredWidgets(container.children(), mouse, out, ops, depth + 1);
            }
        }
    }

    /** Vanilla's {@code tooltip/background} sprite, or a tooltip style's {@code tooltip/<style>_background}. */
    private static boolean isTooltipBackground(String sprite) {
        String path = Identifier.parse(sprite).getPath();
        return path.startsWith("tooltip/") && path.endsWith("background");
    }

    /** A drawn entry's rectangle on screen as {x0, y0, x1, y1}. */
    private static float[] box(JsonObject entry) {
        if (entry.has("screen")) {
            JsonArray screen = entry.getAsJsonArray("screen");
            float x = screen.get(0).getAsFloat();
            float y = screen.get(1).getAsFloat();
            return new float[] {x, y, x + screen.get(2).getAsFloat(), y + screen.get(3).getAsFloat()};
        }
        float x = entry.get("x").getAsFloat();
        float y = entry.get("y").getAsFloat();
        return new float[] {x, y, x + entry.get("width").getAsFloat(), y + entry.get("height").getAsFloat()};
    }

    /** A component as its plain text, its full JSON (fonts, colours, click events) and its runs of one font and style. */
    private static JsonObject component(Component component, DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        json.addProperty("text", component.getString());
        json.add("json", ComponentSerialization.CODEC.encodeStart(ops, component).result().orElse(JsonNull.INSTANCE));
        json.add("runs", UiCapture.runs(component.getVisualOrderText()));
        return json;
    }

    /** An item with every component (name, lore, item model, custom model data), as the item stack codec writes it. */
    private static JsonElement item(ItemStack stack, DynamicOps<JsonElement> ops) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().orElseGet(() -> {
            JsonObject fallback = new JsonObject();
            fallback.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            fallback.addProperty("count", stack.getCount());
            return fallback;
        });
    }

    private static String slug(@Nullable Screen screen) {
        if (screen == null) {
            return "hud";
        }
        String title = screen.getTitle().getString().replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "");
        String name = title.isEmpty() ? screen.getClass().getSimpleName() : title;
        return name.length() > 40 ? name.substring(0, 40) : name;
    }
}
