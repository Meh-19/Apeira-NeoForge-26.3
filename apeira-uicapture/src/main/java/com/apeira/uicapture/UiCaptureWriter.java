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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import com.apeira.uicapture.mixin.AbstractContainerScreenAccessor;
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
import net.neoforged.fml.loading.FMLPaths;

/**
 * Turns a recorded frame into the capture file: the screen (title, class, size, the container's image size, offsets,
 * label positions and slots with their full items), the widget tree, everything drawn in order, the glyphs of every
 * font drawn with and the copied assets. Writes {@code logs/apeira_ui_capture.json} (the latest capture) and keeps
 * each capture, with the copied assets, under {@code logs/apeira_ui_capture/}.
 */
final class UiCaptureWriter {
    static final String LATEST = "logs/apeira_ui_capture.json";
    static final String FOLDER = "logs/apeira_ui_capture";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_DEPTH = 12;

    private UiCaptureWriter() {}

    /** Writes the capture of {@code screen} (null for the HUD) and returns the file written. */
    static Path write(Minecraft minecraft, @Nullable Screen screen, UiCapture.Frame frame) throws IOException {
        Path game = FMLPaths.GAMEDIR.get();
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
            screen.renderables.forEach(renderable -> widget(renderable, widgets, seen, ops, assets, 0));
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
        AbstractContainerScreenAccessor labels = (AbstractContainerScreenAccessor) screen;
        AbstractContainerMenu menu = screen.getMenu();
        JsonObject json = new JsonObject();
        json.addProperty("menu_class", menu.getClass().getName());
        try {
            json.addProperty("menu_type", String.valueOf(BuiltInRegistries.MENU.getKey(menu.getType())));
        } catch (UnsupportedOperationException e) {
            // The player's own inventory menu has no type.
        }
        json.addProperty("image_width", screen.getImageWidth());
        json.addProperty("image_height", screen.getImageHeight());
        json.addProperty("left_pos", screen.getLeftPos());
        json.addProperty("top_pos", screen.getTopPos());
        json.addProperty("title_label_x", labels.apeira$titleLabelX());
        json.addProperty("title_label_y", labels.apeira$titleLabelY());
        json.addProperty("inventory_label_x", labels.apeira$inventoryLabelX());
        json.addProperty("inventory_label_y", labels.apeira$inventoryLabelY());
        JsonArray slots = new JsonArray();
        for (Slot slot : menu.slots) {
            JsonObject entry = new JsonObject();
            entry.addProperty("index", slot.index);
            entry.addProperty("container", slot.container.getClass().getSimpleName());
            entry.addProperty("container_slot", slot.getContainerSlot());
            entry.addProperty("x", slot.x);
            entry.addProperty("y", slot.y);
            entry.addProperty("screen_x", screen.getLeftPos() + slot.x);
            entry.addProperty("screen_y", screen.getTopPos() + slot.y);
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

    /** A component as its plain text and its full JSON (fonts, colours, click events). */
    private static JsonObject component(Component component, DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        json.addProperty("text", component.getString());
        json.add("json", ComponentSerialization.CODEC.encodeStart(ops, component).result().orElse(JsonNull.INSTANCE));
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
