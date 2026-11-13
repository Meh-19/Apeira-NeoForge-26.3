package com.apeira.uicapture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * What one frame of the GUI draws, while a capture records it: the mixins report every texture blit, sprite, fill,
 * text and item here between {@link #begin()} and {@link #end()}, in draw order. Positions are as the screen passed
 * them; a {@code screen} rectangle is added when the pose moved or scaled them. Everything runs on the render thread.
 */
public final class UiCapture {
    private static boolean recording;
    private static int spriteDepth;
    private static JsonArray drawn = new JsonArray();
    /** Codepoints drawn with each font, for {@link FontGlyphs}. */
    private static Map<Identifier, TreeSet<Integer>> glyphs = new LinkedHashMap<>();
    /** Captured items, encoded with the level's registries when the capture ends. */
    private static Map<JsonObject, ItemStack> items = new LinkedHashMap<>();

    private UiCapture() {}

    static void begin() {
        recording = true;
        spriteDepth = 0;
        drawn = new JsonArray();
        glyphs = new LinkedHashMap<>();
        items = new LinkedHashMap<>();
    }

    /** Stops recording and hands over what was drawn. */
    static Frame end() {
        recording = false;
        return new Frame(drawn, glyphs, items);
    }

    record Frame(JsonArray drawn, Map<Identifier, TreeSet<Integer>> glyphs, Map<JsonObject, ItemStack> items) {}

    public static boolean recording() {
        return recording;
    }

    /** Enters a sprite draw by id; true when it is the outermost one and should be recorded. */
    public static boolean enterSprite() {
        if (!recording) {
            return false;
        }
        return spriteDepth++ == 0;
    }

    public static void leaveSprite() {
        if (recording && spriteDepth > 0) {
            spriteDepth--;
        }
    }

    public static boolean inSprite() {
        return spriteDepth > 0;
    }

    public static void blit(Matrix3x2fc pose, Identifier texture, int x, int y, int width, int height, float u, float v,
            int srcWidth, int srcHeight, int textureWidth, int textureHeight, int color) {
        JsonObject entry = entry("texture", pose, x, y, x + width, y + height);
        entry.addProperty("texture", texture.toString());
        entry.addProperty("u", u);
        entry.addProperty("v", v);
        entry.addProperty("region_width", srcWidth);
        entry.addProperty("region_height", srcHeight);
        entry.addProperty("texture_width", textureWidth);
        entry.addProperty("texture_height", textureHeight);
        color(entry, color);
    }

    public static void uvBlit(Matrix3x2fc pose, Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
        JsonObject entry = entry("texture", pose, x0, y0, x1, y1);
        entry.addProperty("texture", texture.toString());
        JsonArray uv = new JsonArray();
        uv.add(u0);
        uv.add(v0);
        uv.add(u1);
        uv.add(v1);
        entry.add("uv", uv);
    }

    /** A GUI sprite by id; {@code region} is sprite width, height and the drawn part's X and Y, when only part is drawn. */
    public static void sprite(Matrix3x2fc pose, Identifier sprite, int x, int y, int width, int height, int @Nullable [] region, int color) {
        JsonObject entry = entry("sprite", pose, x, y, x + width, y + height);
        entry.addProperty("sprite", sprite.toString());
        if (region != null) {
            entry.addProperty("sprite_width", region[0]);
            entry.addProperty("sprite_height", region[1]);
            entry.addProperty("region_x", region[2]);
            entry.addProperty("region_y", region[3]);
        }
        color(entry, color);
    }

    public static void atlasSprite(Matrix3x2fc pose, TextureAtlasSprite sprite, int x, int y, int width, int height, int color) {
        JsonObject entry = entry("atlas_sprite", pose, x, y, x + width, y + height);
        entry.addProperty("atlas", sprite.atlasLocation().toString());
        entry.addProperty("sprite", sprite.contents().name().toString());
        color(entry, color);
    }

    public static void fill(Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color) {
        color(entry("fill", pose, Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1)), color);
    }

    public static void item(Matrix3x2fc pose, ItemStack stack, int x, int y) {
        JsonObject entry = entry("item", pose, x, y, x + 16, y + 16);
        items.put(entry, stack.copy());
    }

    /**
     * A text draw: its plain text, its size, and runs of one font and style, each with its colour and formatting and,
     * where it is not plain ASCII, its codepoints.
     */
    public static void text(Matrix3x2fc pose, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
        StringBuilder plain = new StringBuilder();
        text.accept((index, style, codepoint) -> {
            plain.appendCodePoint(codepoint);
            return true;
        });
        Font font = Minecraft.getInstance().font;
        JsonObject entry = entry("text", pose, x, y, x + font.width(text), y + font.lineHeight);
        entry.addProperty("text", plain.toString());
        color(entry, color);
        entry.addProperty("shadow", shadow);
        entry.add("runs", runs(text));
    }

    /** {@code text} split into runs of one font and style, as {@link #text} records them. */
    public static JsonArray runs(FormattedCharSequence text) {
        List<Run> runs = new ArrayList<>();
        text.accept((index, style, codepoint) -> {
            String font = font(style, codepoint);
            if (runs.isEmpty() || !runs.getLast().font.equals(font) || !runs.getLast().style.equals(style)) {
                runs.add(new Run(font, style));
            }
            runs.getLast().add(codepoint);
            return true;
        });
        JsonArray json = new JsonArray();
        runs.forEach(run -> json.add(run.toJson()));
        return json;
    }

    /** Consecutive characters drawn with one font and style. */
    private static final class Run {
        final String font;
        final Style style;
        final StringBuilder text = new StringBuilder();
        final JsonArray codepoints = new JsonArray();
        boolean special;

        Run(String font, Style style) {
            this.font = font;
            this.style = style;
        }

        void add(int codepoint) {
            text.appendCodePoint(codepoint);
            codepoints.add(hex(codepoint));
            special |= codepoint < 0x20 || codepoint > 0x7E;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("font", font);
            json.addProperty("text", text.toString());
            TextColor color = style.getColor();
            if (color != null) {
                // The name for a named colour ("gold"), else "#RRGGBB", and the value either way.
                json.addProperty("color", color.serialize());
                json.addProperty("rgb", String.format("#%06X", color.getValue() & 0xFFFFFF));
            }
            if (style.getShadowColor() != null) {
                json.addProperty("shadow_color", String.format("#%08X", style.getShadowColor()));
            }
            JsonArray formatting = new JsonArray();
            if (style.isBold()) {
                formatting.add("bold");
            }
            if (style.isItalic()) {
                formatting.add("italic");
            }
            if (style.isUnderlined()) {
                formatting.add("underlined");
            }
            if (style.isStrikethrough()) {
                formatting.add("strikethrough");
            }
            if (style.isObfuscated()) {
                formatting.add("obfuscated");
            }
            if (!formatting.isEmpty()) {
                json.add("formatting", formatting);
            }
            if (special) {
                json.add("codepoints", codepoints);
            }
            return json;
        }
    }

    private static String font(Style style, int codepoint) {
        FontDescription font = style.getFont();
        if (font instanceof FontDescription.Resource resource) {
            glyphs.computeIfAbsent(resource.id(), id -> new TreeSet<>()).add(codepoint);
            return resource.id().toString();
        }
        if (font instanceof FontDescription.AtlasSprite sprite) {
            return "atlas_sprite " + sprite.atlasId() + " " + sprite.spriteId();
        }
        return font.getClass().getSimpleName();
    }

    static String hex(int codepoint) {
        return String.format("U+%04X", codepoint);
    }

    private static JsonObject entry(String kind, Matrix3x2fc pose, float x0, float y0, float x1, float y1) {
        JsonObject entry = new JsonObject();
        entry.addProperty("kind", kind);
        entry.addProperty("x", x0);
        entry.addProperty("y", y0);
        entry.addProperty("width", x1 - x0);
        entry.addProperty("height", y1 - y0);
        Vector2f a = pose.transformPosition(x0, y0, new Vector2f());
        Vector2f b = pose.transformPosition(x1, y1, new Vector2f());
        if (a.x != x0 || a.y != y0 || b.x != x1 || b.y != y1) {
            JsonArray screen = new JsonArray();
            screen.add(a.x);
            screen.add(a.y);
            screen.add(b.x - a.x);
            screen.add(b.y - a.y);
            entry.add("screen", screen);
        }
        drawn.add(entry);
        return entry;
    }

    private static void color(JsonObject entry, int color) {
        if (color != -1) {
            entry.addProperty("color", String.format("#%08X", color));
        }
    }
}
