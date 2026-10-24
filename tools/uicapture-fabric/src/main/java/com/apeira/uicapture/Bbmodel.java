package com.apeira.uicapture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Converts the exported Bedrock geometry into a Blockbench project ({@code .bbmodel}, Bedrock format) with the atlas
 * embedded as its texture (and its file's absolute and relative path), so the model opens textured. Bones become groups,
 * cubes elements (box UV cubes keep their box UV, per-face cubes their face rectangles), locators locators; Blockbench's
 * own coordinates are Java's, so the Bedrock export's mirrored x and negated rotations are undone. The recorded
 * animation is not copied: import {@code <name>.animation.json} into the project.
 */
final class Bbmodel {
    private static final String[] FACES = {"north", "east", "south", "west", "up", "down"};

    private Bbmodel() {
    }

    static JsonObject of(JsonObject geo, String name, Path texture, int textureWidth, int textureHeight) throws IOException {
        JsonObject geometry = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonArray elements = new JsonArray();
        JsonArray outliner = new JsonArray();
        Map<String, JsonObject> groups = new HashMap<>();
        for (JsonElement b : geometry.getAsJsonArray("bones")) {
            JsonObject bone = b.getAsJsonObject();
            JsonObject group = new JsonObject();
            group.addProperty("name", bone.get("name").getAsString());
            group.add("origin", mirrored(bone.getAsJsonArray("pivot")));
            if (bone.has("rotation")) {
                group.add("rotation", rotation(bone.getAsJsonArray("rotation")));
            }
            group.addProperty("color", 0);
            group.addProperty("uuid", UUID.randomUUID().toString());
            group.addProperty("export", true);
            group.addProperty("mirror_uv", false);
            group.addProperty("isOpen", false);
            group.addProperty("locked", false);
            group.addProperty("visibility", true);
            group.addProperty("autouv", 0);
            JsonArray children = new JsonArray();
            group.add("children", children);
            if (bone.has("cubes")) {
                for (JsonElement c : bone.getAsJsonArray("cubes")) {
                    JsonObject element = element(c.getAsJsonObject(), bone.get("name").getAsString(), group.getAsJsonArray("origin"));
                    elements.add(element);
                    children.add(element.get("uuid"));
                }
            }
            if (bone.has("locators")) {
                for (Map.Entry<String, JsonElement> locator : bone.getAsJsonObject("locators").entrySet()) {
                    JsonObject element = new JsonObject();
                    element.addProperty("name", locator.getKey());
                    element.add("position", mirrored(locator.getValue().getAsJsonArray()));
                    element.add("rotation", ArmorStandExporter.array(0, 0, 0));
                    element.addProperty("type", "locator");
                    element.addProperty("uuid", UUID.randomUUID().toString());
                    elements.add(element);
                    children.add(element.get("uuid"));
                }
            }
            groups.put(group.get("name").getAsString(), group);
            JsonObject parent = bone.has("parent") ? groups.get(bone.get("parent").getAsString()) : null;
            (parent != null ? parent.getAsJsonArray("children") : outliner).add(group);
        }

        JsonObject image = new JsonObject();
        image.addProperty("path", texture.toAbsolutePath().toString());
        image.addProperty("name", texture.getFileName().toString());
        image.addProperty("folder", "");
        image.addProperty("namespace", "");
        image.addProperty("id", "0");
        image.addProperty("width", textureWidth);
        image.addProperty("height", textureHeight);
        image.addProperty("uv_width", textureWidth);
        image.addProperty("uv_height", textureHeight);
        image.addProperty("particle", false);
        image.addProperty("render_mode", "default");
        image.addProperty("render_sides", "auto");
        image.addProperty("visible", true);
        image.addProperty("internal", true);
        image.addProperty("saved", true);
        image.addProperty("uuid", UUID.randomUUID().toString());
        image.addProperty("relative_path", texture.getFileName().toString());
        image.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(texture)));
        JsonArray textures = new JsonArray();
        textures.add(image);

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "4.10");
        meta.addProperty("model_format", "bedrock");
        meta.addProperty("box_uv", false);
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", textureWidth);
        resolution.addProperty("height", textureHeight);
        JsonObject root = new JsonObject();
        root.add("meta", meta);
        root.addProperty("name", name);
        root.addProperty("model_identifier", name);
        JsonObject description = geometry.getAsJsonObject("description");
        root.add("visible_box", ArmorStandExporter.array(description.get("visible_bounds_width").getAsDouble(),
                description.get("visible_bounds_height").getAsDouble(), description.getAsJsonArray("visible_bounds_offset").get(1).getAsDouble()));
        root.add("resolution", resolution);
        root.add("elements", elements);
        root.add("outliner", outliner);
        root.add("textures", textures);
        return root;
    }

    private static JsonObject element(JsonObject cube, String bone, JsonArray groupOrigin) {
        double[] o = values(cube.getAsJsonArray("origin")), s = values(cube.getAsJsonArray("size"));
        JsonObject element = new JsonObject();
        element.addProperty("name", bone);
        element.addProperty("rescale", false);
        element.addProperty("locked", false);
        element.add("from", ArmorStandExporter.array(-o[0] - s[0], o[1], o[2]));
        element.add("to", ArmorStandExporter.array(-o[0], o[1] + s[1], o[2] + s[2]));
        element.addProperty("autouv", 0);
        element.addProperty("color", 0);
        element.add("origin", cube.has("pivot") ? mirrored(cube.getAsJsonArray("pivot")) : groupOrigin.deepCopy());
        if (cube.has("rotation")) {
            element.add("rotation", rotation(cube.getAsJsonArray("rotation")));
        }
        JsonObject faces = new JsonObject();
        JsonElement uv = cube.get("uv");
        if (uv != null && uv.isJsonArray()) {
            // Box UV: Blockbench's layout for a cube's unfolded box, mirrored by swapping east and west and each face's u.
            double u = uv.getAsJsonArray().get(0).getAsDouble(), v = uv.getAsJsonArray().get(1).getAsDouble();
            double w = s[0], h = s[1], d = s[2];
            boolean mirror = cube.has("mirror") && cube.get("mirror").getAsBoolean();
            Map<String, double[]> rects = new HashMap<>();
            rects.put("north", new double[] {u + d, v + d, u + d + w, v + d + h});
            rects.put(mirror ? "west" : "east", new double[] {u, v + d, u + d, v + d + h});
            rects.put("south", new double[] {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h});
            rects.put(mirror ? "east" : "west", new double[] {u + d + w, v + d, u + 2 * d + w, v + d + h});
            rects.put("up", new double[] {u + d + w, v + d, u + d, v});
            rects.put("down", new double[] {u + d + 2 * w, v, u + d + w, v + d});
            for (String face : FACES) {
                double[] r = rects.get(face);
                faces.add(face, face(mirror ? new double[] {r[2], r[1], r[0], r[3]} : r, 0));
            }
            element.addProperty("box_uv", true);
            element.add("uv_offset", ArmorStandExporter.array(u, v));
            element.addProperty("mirror_uv", mirror);
        } else {
            JsonObject perFace = uv != null && uv.isJsonObject() ? uv.getAsJsonObject() : new JsonObject();
            for (String face : FACES) {
                JsonObject f = perFace.getAsJsonObject(face);
                if (f == null) {
                    JsonObject empty = new JsonObject();
                    empty.add("uv", ArmorStandExporter.array(0, 0, 0, 0));
                    empty.add("texture", null);
                    faces.add(face, empty);
                    continue;
                }
                double[] p = values(f.getAsJsonArray("uv")), size = values(f.getAsJsonArray("uv_size"));
                // The Bedrock export flips up and down faces; undo it.
                double[] rect = face.equals("up") || face.equals("down")
                        ? new double[] {p[0] + size[0], p[1] + size[1], p[0], p[1]}
                        : new double[] {p[0], p[1], p[0] + size[0], p[1] + size[1]};
                faces.add(face, face(rect, f.has("uv_rotation") ? f.get("uv_rotation").getAsInt() : 0));
            }
            element.addProperty("box_uv", false);
        }
        element.add("faces", faces);
        element.addProperty("type", "cube");
        element.addProperty("uuid", UUID.randomUUID().toString());
        return element;
    }

    private static JsonObject face(double[] rect, int rotation) {
        JsonObject face = new JsonObject();
        face.add("uv", ArmorStandExporter.array(rect));
        face.addProperty("texture", 0);
        if (rotation != 0) {
            face.addProperty("rotation", rotation);
        }
        return face;
    }

    private static JsonArray mirrored(JsonArray position) {
        double[] p = values(position);
        return ArmorStandExporter.array(-p[0], p[1], p[2]);
    }

    private static JsonArray rotation(JsonArray rotation) {
        double[] r = values(rotation);
        return ArmorStandExporter.array(-r[0], -r[1], r[2]);
    }

    private static double[] values(JsonArray array) {
        double[] v = new double[array.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = array.get(i).getAsDouble();
        }
        return v;
    }
}
