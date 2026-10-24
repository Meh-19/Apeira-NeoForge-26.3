package com.apeira.uicapture;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

import org.joml.Quaternionf;

/**
 * Turns resolved item models into Bedrock geometry for one bone: Java elements become cubes with per-face UV into the
 * {@link TextureAtlas}, generated items become their {@code layer<n>} textures' opaque pixels as one-pixel-deep cubes
 * (rows of equal runs merged into rectangles, front on south, as vanilla's generator faces them). The model is moved by
 * -8 pixels, as the item renderer draws it from the block's centre. Bedrock's conventions are Blockbench's: x mirrored,
 * cube origin {@code [-to.x, from.y, from.z]}, pivot x mirrored, x and y rotations negated, up and down faces' UV
 * flipped. Element {@code rescale} and face tints (dye, potions) are not applied.
 */
final class ItemGeometry {
    private static final String[] FACES = {"north", "east", "south", "west", "up", "down"};

    private ItemGeometry() {
    }

    /** Adds every texture the model's faces use to the atlas (before it is packed). */
    static void collect(ItemModels.Model model, TextureAtlas atlas) {
        if (model.generated()) {
            for (int layer = 0; model.textures().containsKey("layer" + layer); layer++) {
                atlas.add(model.textures().get("layer" + layer));
            }
            return;
        }
        if (model.elements() == null) {
            return;
        }
        for (JsonElement element : model.elements()) {
            JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
            if (faces == null) {
                continue;
            }
            for (String face : faces.keySet()) {
                JsonElement texture = faces.getAsJsonObject(face).get("texture");
                Identifier id = texture == null ? null : ItemModels.texture(model, texture.getAsString());
                atlas.add(id != null ? id : Identifier.withDefaultNamespace("missingno"));
            }
        }
    }

    /** The model's cubes, in pixels about the item's centre. */
    static JsonArray cubes(ItemModels.Model model, TextureAtlas atlas) {
        JsonArray cubes = new JsonArray();
        if (model.generated()) {
            for (int layer = 0; model.textures().containsKey("layer" + layer); layer++) {
                generated(atlas.slot(model.textures().get("layer" + layer)), cubes);
            }
            return cubes;
        }
        if (model.elements() == null) {
            return cubes;
        }
        for (JsonElement e : model.elements()) {
            JsonObject element = e.getAsJsonObject();
            double[] from = vec(element.getAsJsonArray("from"));
            double[] to = vec(element.getAsJsonArray("to"));
            JsonObject cube = cube(from, to);
            JsonObject rotation = element.getAsJsonObject("rotation");
            if (rotation != null && rotation.has("angle") && rotation.get("angle").getAsDouble() != 0) {
                double[] origin = vec(rotation.getAsJsonArray("origin"));
                double angle = rotation.get("angle").getAsDouble();
                String axis = rotation.get("axis").getAsString();
                cube.add("pivot", array(-(origin[0] - 8), origin[1] - 8, origin[2] - 8));
                cube.add("rotation", array(axis.equals("x") ? -angle : 0, axis.equals("y") ? -angle : 0, axis.equals("z") ? angle : 0));
            }
            JsonObject faces = element.getAsJsonObject("faces");
            JsonObject uv = new JsonObject();
            if (faces != null) {
                for (String name : FACES) {
                    JsonObject face = faces.getAsJsonObject(name);
                    if (face == null) {
                        continue;
                    }
                    Identifier id = face.has("texture") ? ItemModels.texture(model, face.get("texture").getAsString()) : null;
                    TextureAtlas.Slot slot = atlas.slot(id != null ? id : Identifier.withDefaultNamespace("missingno"));
                    double[] rect = face.has("uv") ? vec(face.getAsJsonArray("uv")) : defaultUv(name, from, to);
                    int turn = face.has("rotation") ? face.get("rotation").getAsInt() : 0;
                    uv.add(name, face(name, rect, slot, turn));
                }
            }
            cube.add("uv", uv);
            cubes.add(cube);
        }
        return cubes;
    }

    /** Vanilla's UV for a face without one: the element's extent seen from that side. */
    private static double[] defaultUv(String face, double[] f, double[] t) {
        return switch (face) {
            case "down" -> new double[] {f[0], 16 - t[2], t[0], 16 - f[2]};
            case "up" -> new double[] {f[0], f[2], t[0], t[2]};
            case "north" -> new double[] {16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]};
            case "south" -> new double[] {f[0], 16 - t[1], t[0], 16 - f[1]};
            case "west" -> new double[] {f[2], 16 - t[1], t[2], 16 - f[1]};
            default -> new double[] {16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]};
        };
    }

    /** A generated item layer: its opaque pixels as one-pixel-deep cubes between z 7.5 and 8.5. */
    private static void generated(TextureAtlas.Slot slot, JsonArray cubes) {
        int w = slot.width(), h = slot.height();
        double pw = 16.0 / w, ph = 16.0 / h;
        // Open rectangles by run: [first column, last column] -> first row.
        java.util.Map<Long, Integer> open = new java.util.LinkedHashMap<>();
        for (int y = 0; y <= h; y++) {
            java.util.Set<Long> runs = new java.util.LinkedHashSet<>();
            for (int x = 0; y < h && x < w; x++) {
                if (slot.alpha(x, y) == 0) {
                    continue;
                }
                int start = x;
                while (x + 1 < w && slot.alpha(x + 1, y) != 0) {
                    x++;
                }
                runs.add(((long) start << 32) | x);
            }
            for (java.util.Iterator<java.util.Map.Entry<Long, Integer>> it = open.entrySet().iterator(); it.hasNext();) {
                java.util.Map.Entry<Long, Integer> entry = it.next();
                if (!runs.contains(entry.getKey())) {
                    int a = (int) (entry.getKey() >> 32), b = (int) (long) entry.getKey(), top = entry.getValue(), bottom = y - 1;
                    double u1 = a * pw, u2 = (b + 1) * pw, v1 = top * ph, v2 = (bottom + 1) * ph;
                    JsonObject cube = cube(new double[] {u1, 16 - v2, 7.5}, new double[] {u2, 16 - v1, 8.5});
                    JsonObject uv = new JsonObject();
                    uv.add("north", face("north", new double[] {u2, v1, u1, v2}, slot, 0));
                    uv.add("east", face("east", new double[] {u2 - pw, v1, u2, v2}, slot, 0));
                    uv.add("south", face("south", new double[] {u1, v1, u2, v2}, slot, 0));
                    uv.add("west", face("west", new double[] {u1, v1, u1 + pw, v2}, slot, 0));
                    uv.add("up", face("up", new double[] {u1, v1, u2, v1 + ph}, slot, 0));
                    uv.add("down", face("down", new double[] {u1, v2 - ph, u2, v2}, slot, 0));
                    cube.add("uv", uv);
                    cubes.add(cube);
                    it.remove();
                }
            }
            for (long run : runs) {
                open.putIfAbsent(run, y);
            }
        }
    }

    private static JsonObject cube(double[] from, double[] to) {
        JsonObject cube = new JsonObject();
        cube.add("origin", array(-(to[0] - 8), from[1] - 8, from[2] - 8));
        cube.add("size", array(to[0] - from[0], to[1] - from[1], to[2] - from[2]));
        return cube;
    }

    /** One face's Bedrock UV: Java's 0-16 rectangle scaled into the texture's place in the atlas. */
    private static JsonObject face(String name, double[] rect, TextureAtlas.Slot slot, int turn) {
        double a = slot.x() + rect[0] * slot.width() / 16, b = slot.y() + rect[1] * slot.height() / 16;
        double c = slot.x() + rect[2] * slot.width() / 16, d = slot.y() + rect[3] * slot.height() / 16;
        JsonObject face = new JsonObject();
        if (name.equals("up") || name.equals("down")) {
            face.add("uv", array(c, d));
            face.add("uv_size", array(a - c, b - d));
        } else {
            face.add("uv", array(a, b));
            face.add("uv_size", array(c - a, d - b));
        }
        if (turn % 360 != 0) {
            face.addProperty("uv_rotation", ((turn % 360) + 360) % 360);
        }
        return face;
    }

    /** A model's {@code display.<context>} transform as vanilla applies it: translation (pixels, within ±80), rotation (degrees, x, y, z order), scale (within ±4). */
    static double[][] rawDisplay(ItemModels.Model model, String context) {
        double[] t = {0, 0, 0}, r = {0, 0, 0}, s = {1, 1, 1};
        JsonObject display = model.display().getAsJsonObject(context.toLowerCase(Locale.ROOT));
        if (display != null) {
            if (display.has("translation")) {
                t = vec(display.getAsJsonArray("translation"));
                for (int i = 0; i < 3; i++) {
                    t[i] = Math.clamp(t[i], -80, 80);
                }
            }
            if (display.has("rotation")) {
                r = vec(display.getAsJsonArray("rotation"));
            }
            if (display.has("scale")) {
                s = vec(display.getAsJsonArray("scale"));
                for (int i = 0; i < 3; i++) {
                    s[i] = Math.clamp(s[i], -4, 4);
                }
            }
        }
        return new double[][] {t, r, s};
    }

    /**
     * The bone transform that draws a model in an item display: the display entity's half turn about y, then the model's
     * {@link #rawDisplay} transform, as Bedrock position, rotation and scale (geometry bones have no position or scale, so
     * the animation holds them).
     */
    static double[][] displayTransform(ItemModels.Model model, String context) {
        double[][] raw = rawDisplay(model, context);
        double[] t = raw[0], r = raw[1];
        Quaternionf rotation = new Quaternionf().rotationY((float) Math.PI)
                .mul(new Quaternionf().rotationXYZ((float) Math.toRadians(r[0]), (float) Math.toRadians(r[1]), (float) Math.toRadians(r[2])));
        // The half turn takes (x, y, z) to (-x, y, -z); Bedrock mirrors x back.
        return new double[][] {{t[0], t[1], -t[2]}, ArmorStandExporter.euler(rotation), raw[2]};
    }

    private static double[] vec(@Nullable JsonArray array) {
        double[] v = new double[array == null ? 0 : array.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = array.get(i).getAsDouble();
        }
        return v.length == 3 || v.length == 4 ? v : new double[] {0, 0, 0};
    }

    static JsonArray array(double... values) {
        return ArmorStandExporter.array(values);
    }
}
