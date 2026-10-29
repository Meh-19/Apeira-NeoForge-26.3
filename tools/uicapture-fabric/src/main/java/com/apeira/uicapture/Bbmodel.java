package com.apeira.uicapture;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Converts the exported geometry (GeckoLib's, which is Bedrock's geometry format) into a Blockbench 5.2 project
 * ({@code .bbmodel}, project format {@value #FORMAT_VERSION}, GeckoLib's Animated Model format
 * {@code animated_entity_model}, which needs the GeckoLib plugin, 4.2.5, so it exports GeckoLib files rather than Bedrock
 * entity files) with the atlas embedded as its texture (and its file's absolute and relative path), so the model opens
 * textured. Bones become groups, cubes elements (box UV cubes keep their box UV, per-face cubes their face rectangles),
 * locators locators; Blockbench's own coordinates are Java's, so the Bedrock export's mirrored x and negated rotations
 * are undone. As Blockbench 5 saves them, groups are listed once in {@code groups} and the outliner holds only their
 * uuids and children.
 *
 * <p>The exported clips are embedded as the project's animations, each bone's animator keyed by its group's uuid, so they
 * play on the right bones without importing anything. Blockbench keeps keyframes in its own axes: a position's x and a
 * rotation's x and y are the animation file's negated (the GeckoLib export negates them back), scales as they are.
 */
final class Bbmodel {
    private static final String[] FACES = {"north", "east", "south", "west", "up", "down"};
    /** The project format Blockbench 5.x writes. */
    static final String FORMAT_VERSION = "5.0";
    /** The GeckoLib plugin's model format. */
    static final String MODEL_FORMAT = "animated_entity_model";

    private Bbmodel() {
    }

    /** The project, with {@code animationFiles} (GeckoLib animation files over this geometry) as its animations. */
    static JsonObject of(JsonObject geo, String name, Path texture, int textureWidth, int textureHeight, List<JsonObject> animationFiles)
            throws IOException {
        JsonObject geometry = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonArray elements = new JsonArray();
        JsonArray groupList = new JsonArray();
        JsonArray outliner = new JsonArray();
        // Each bone's outliner node (uuid and children) and group uuid, by name.
        Map<String, JsonObject> nodes = new HashMap<>();
        Map<String, String> uuids = new HashMap<>();
        for (JsonElement b : geometry.getAsJsonArray("bones")) {
            JsonObject bone = b.getAsJsonObject();
            String uuid = UUID.randomUUID().toString();
            JsonObject group = new JsonObject();
            group.addProperty("name", bone.get("name").getAsString());
            group.addProperty("uuid", uuid);
            group.addProperty("export", true);
            group.addProperty("locked", false);
            group.addProperty("scope", 0);
            group.addProperty("visibility", true);
            group.add("origin", mirrored(bone.getAsJsonArray("pivot")));
            group.add("rotation", bone.has("rotation") ? rotation(bone.getAsJsonArray("rotation")) : ArmorStandExporter.array(0, 0, 0));
            group.addProperty("color", 0);
            group.add("children", new JsonArray());
            group.addProperty("reset", false);
            group.addProperty("shade", true);
            group.addProperty("mirror_uv", false);
            group.addProperty("autouv", 0);
            group.addProperty("isOpen", false);
            groupList.add(group);
            JsonObject node = new JsonObject();
            node.addProperty("uuid", uuid);
            node.addProperty("isOpen", false);
            JsonArray children = new JsonArray();
            node.add("children", children);
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
                    element.addProperty("ignore_inherited_scale", false);
                    element.addProperty("visibility", true);
                    element.addProperty("locked", false);
                    element.addProperty("scope", 0);
                    element.addProperty("type", "locator");
                    element.addProperty("uuid", UUID.randomUUID().toString());
                    elements.add(element);
                    children.add(element.get("uuid"));
                }
            }
            nodes.put(bone.get("name").getAsString(), node);
            uuids.put(bone.get("name").getAsString(), uuid);
            JsonObject parent = bone.has("parent") ? nodes.get(bone.get("parent").getAsString()) : null;
            (parent != null ? parent.getAsJsonArray("children") : outliner).add(node);
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
        image.addProperty("group", "");
        image.addProperty("scope", 0);
        image.addProperty("particle", false);
        image.addProperty("use_as_default", false);
        image.addProperty("layers_enabled", false);
        image.addProperty("sync_to_project", "");
        image.addProperty("file_format", "png");
        image.addProperty("render_mode", "default");
        image.addProperty("render_sides", "auto");
        image.addProperty("wrap_mode", "limited");
        image.addProperty("pbr_channel", "color");
        image.addProperty("frame_time", 1);
        image.addProperty("frame_order_type", "loop");
        image.addProperty("frame_order", "");
        image.addProperty("frame_interpolate", false);
        image.addProperty("visible", true);
        image.addProperty("internal", true);
        image.addProperty("saved", true);
        image.addProperty("uuid", UUID.randomUUID().toString());
        image.addProperty("relative_path", texture.getFileName().toString());
        image.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(texture)));
        JsonArray textures = new JsonArray();
        textures.add(image);

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", FORMAT_VERSION);
        meta.addProperty("model_format", MODEL_FORMAT);
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
        root.addProperty("variable_placeholders", "");
        root.addProperty("multi_file_ruleset", "");
        root.add("variable_placeholder_buttons", new JsonArray());
        root.add("timeline_setups", new JsonArray());
        root.add("unhandled_root_fields", new JsonObject());
        root.add("resolution", resolution);
        root.add("elements", elements);
        root.add("groups", groupList);
        root.add("outliner", outliner);
        root.add("textures", textures);
        JsonArray animations = new JsonArray();
        for (JsonObject file : animationFiles) {
            for (Map.Entry<String, JsonElement> clip : file.getAsJsonObject("animations").entrySet()) {
                animations.add(animation(clip.getKey(), clip.getValue().getAsJsonObject(), uuids));
            }
        }
        root.add("animations", animations);
        return root;
    }

    /** A GeckoLib clip as a Blockbench animation, its bones bound to their groups by uuid. */
    private static JsonObject animation(String name, JsonObject clip, Map<String, String> uuids) {
        JsonObject animators = new JsonObject();
        JsonObject bones = clip.has("bones") ? clip.getAsJsonObject("bones") : new JsonObject();
        for (Map.Entry<String, JsonElement> bone : bones.entrySet()) {
            String uuid = uuids.get(bone.getKey());
            if (uuid == null) {
                continue;
            }
            JsonArray keyframes = new JsonArray();
            for (Map.Entry<String, JsonElement> channel : bone.getValue().getAsJsonObject().entrySet()) {
                if (channel.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> frame : channel.getValue().getAsJsonObject().entrySet()) {
                        keyframes.add(keyframe(channel.getKey(), Double.parseDouble(frame.getKey()), frame.getValue()));
                    }
                } else {
                    keyframes.add(keyframe(channel.getKey(), 0, channel.getValue()));
                }
            }
            JsonObject animator = new JsonObject();
            animator.addProperty("name", bone.getKey());
            animator.addProperty("type", "bone");
            animator.addProperty("rotation_global", false);
            animator.addProperty("quaternion_interpolation", false);
            animator.add("keyframes", keyframes);
            animators.add(uuid, animator);
        }
        JsonObject animation = new JsonObject();
        animation.addProperty("uuid", UUID.randomUUID().toString());
        animation.addProperty("name", name);
        animation.addProperty("loop", clip.has("loop") && clip.get("loop").getAsBoolean() ? "loop" : "once");
        animation.addProperty("override", false);
        animation.addProperty("length", clip.has("animation_length") ? clip.get("animation_length").getAsDouble() : 0);
        animation.addProperty("snapping", 20);
        animation.addProperty("selected", false);
        animation.addProperty("group_name", "");
        animation.addProperty("scope", 0);
        animation.addProperty("anim_time_update", "");
        animation.addProperty("blend_weight", "");
        animation.addProperty("start_delay", "");
        animation.addProperty("loop_delay", "");
        animation.add("animators", animators);
        return animation;
    }

    /** One keyframe in Blockbench's axes: positions' x and rotations' x and y negated, scales as they are. */
    private static JsonObject keyframe(String channel, double time, JsonElement value) {
        double[] v;
        if (value.isJsonArray()) {
            v = values(value.getAsJsonArray());
        } else {
            double d = value.getAsDouble();
            v = new double[] {d, d, d};
        }
        if (channel.equals("position")) {
            v[0] = -v[0];
        } else if (channel.equals("rotation")) {
            v[0] = -v[0];
            v[1] = -v[1];
        }
        JsonObject point = new JsonObject();
        point.addProperty("x", number(v[0]));
        point.addProperty("y", number(v[1]));
        point.addProperty("z", number(v[2]));
        JsonArray points = new JsonArray();
        points.add(point);
        JsonObject keyframe = new JsonObject();
        keyframe.addProperty("channel", channel);
        keyframe.add("data_points", points);
        keyframe.addProperty("uuid", UUID.randomUUID().toString());
        keyframe.addProperty("time", time);
        keyframe.addProperty("color", -1);
        keyframe.addProperty("interpolation", "linear");
        return keyframe;
    }

    /** A keyframe value as Blockbench saves it: a string, whole numbers without ".0", others to 4 places. */
    private static String number(double value) {
        double rounded = Math.round(value * 10000) / 10000.0;
        if (rounded == 0) {
            return "0";
        }
        return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString();
    }

    private static JsonObject element(JsonObject cube, String bone, JsonArray groupOrigin) {
        double[] o = values(cube.getAsJsonArray("origin")), s = values(cube.getAsJsonArray("size"));
        JsonObject element = new JsonObject();
        element.addProperty("name", bone);
        element.addProperty("render_order", "default");
        element.addProperty("rescale", false);
        element.addProperty("locked", false);
        element.addProperty("export", true);
        element.addProperty("scope", 0);
        element.addProperty("allow_mirror_modeling", true);
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
