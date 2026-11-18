import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Exports Ren's Badger Crate Blockbench recording ({@code BetterBadgerCratesMkII.bbmodel}, a free model recorded by
 * apeira-uicapture's armor stand recorder) for {@code apeira-main}'s Badger Crate. The conversion is
 * {@code scripts/bbmodel_to_geckolib.py}'s (GeckoLib geometry and animation, x mirrored), plus:
 * <ul>
 * <li>the {@code player} group (the recorded player) is dropped; its body and head track becomes the spectator camera
 * ({@code badger_crate/camera.json}: one frame per tick, eye position in blocks from the model origin (root_rig moved by its animation),
 * Java yaw and pitch);</li>
 * <li>placeholder geometry is dropped: armor stand bodies ({@code *_model}, {@code *_base_plate}), the server's empty
 * paper buttons ({@code *empty_button}) and the recorded rewards under {@code item_1}-{@code item_3} (their bones stay as
 * anchors for the rolled items);</li>
 * <li>each reward's VFX ({@code item_<n>_glow}, {@code _pulse}, {@code _trail}, {@code _spark}) gets its own atlas
 * regions, listed in {@code badger_crate/fx_layout.json}, which the client repaints with the rolled tier's textures.</li>
 * </ul>
 * Run from the repository root with Java 21+ and Gson on the class path:
 * {@code java -cp gson.jar tools/badger-crate-export/BadgerCrateExport.java <model.bbmodel>}.
 */
public final class BadgerCrateExport {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
    private static final String[] FACES = {"north", "east", "south", "west", "up", "down"};
    private static final Pattern FX_GROUP = Pattern.compile("item_([123])_(glow|pulse|trail|spark)");
    private static final Pattern FX_TEXTURE = Pattern.compile("(?:white|blue|purple|yellow)_(glow|pulse|trail|spark)(?:_(\\d+))?$");
    private static final Pattern ITEM_GROUP = Pattern.compile("item_[123]");
    private static final String CAMERA_GROUP = "player";
    private static final double EYE_HEIGHT = 1.62;

    private final JsonObject project;
    private final Map<String, JsonObject> groups = new HashMap<>();
    private final Map<String, JsonObject> elements = new HashMap<>();
    private final Set<String> names = new HashSet<>();
    private final Set<String> skippedBones = new HashSet<>();
    private final JsonArray bones = new JsonArray();
    private final BufferedImage source;
    private final double uvScaleX;
    private final double uvScaleY;
    /** Regions added below the source atlas, keyed by slot, effect, frame and source rectangle. */
    private final Map<String, int[]> regions = new LinkedHashMap<>();
    private final List<JsonObject> layout = new ArrayList<>();
    private int nextX;
    private int nextY;
    private int rowHeight;
    private int fxTop;

    private BadgerCrateExport(JsonObject project) throws IOException {
        this.project = project;
        for (JsonElement group : project.getAsJsonArray("groups")) {
            groups.put(group.getAsJsonObject().get("uuid").getAsString(), group.getAsJsonObject());
        }
        for (JsonElement element : project.getAsJsonArray("elements")) {
            elements.put(element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }
        String data = project.getAsJsonArray("textures").get(0).getAsJsonObject().get("source").getAsString();
        source = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1))));
        JsonObject resolution = project.getAsJsonObject("resolution");
        uvScaleX = source.getWidth() / resolution.get("width").getAsDouble();
        uvScaleY = source.getHeight() / resolution.get("height").getAsDouble();
        fxTop = source.getHeight();
        nextY = fxTop;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: BadgerCrateExport <model.bbmodel> [assets dir]");
            System.exit(2);
        }
        Path assets = Path.of(args.length > 1 ? args[1] : "apeira-main/src/main/resources/assets/apeira");
        JsonObject project = JsonParser.parseString(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8)).getAsJsonObject();
        new BadgerCrateExport(project).write(assets);
    }

    private void write(Path assets) throws IOException {
        walk(project.getAsJsonArray("outliner"), null, null, null);
        BufferedImage atlas = atlas();
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.badger_crate");
        description.addProperty("texture_width", atlas.getWidth());
        description.addProperty("texture_height", atlas.getHeight());
        description.addProperty("visible_bounds_width", 12);
        description.addProperty("visible_bounds_height", 12);
        description.add("visible_bounds_offset", array(0, 4, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray list = new JsonArray();
        list.add(geometry);
        JsonObject geo = new JsonObject();
        geo.addProperty("format_version", "1.12.0");
        geo.add("minecraft:geometry", list);

        // The texture is sized in pixels; UVs are written in texture pixels, so the UV scale is folded in.
        write(assets.resolve("geckolib/models/badger_crate.geo.json"), GSON.toJson(geo));
        write(assets.resolve("geckolib/animations/badger_crate.animation.json"), COMPACT.toJson(animations()));
        Path texture = assets.resolve("textures/entity/badger_crate/badger_crate.png");
        Files.createDirectories(texture.getParent());
        ImageIO.write(atlas, "png", texture.toFile());
        JsonObject fx = new JsonObject();
        fx.addProperty("width", atlas.getWidth());
        fx.addProperty("height", atlas.getHeight());
        JsonArray entries = new JsonArray();
        layout.forEach(entries::add);
        fx.add("regions", entries);
        write(assets.resolve("badger_crate/fx_layout.json"), GSON.toJson(fx));
        write(assets.resolve("badger_crate/camera.json"), COMPACT.toJson(camera()));
        System.out.println("bones " + bones.size() + ", fx regions " + layout.size() + ", atlas " + atlas.getWidth() + "x" + atlas.getHeight()
                + ", skipped bones " + skippedBones.size());
    }

    private static void write(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text + "\n", StandardCharsets.UTF_8);
    }

    // Geometry

    private void walk(JsonArray nodes, JsonObject parent, String top, String fxGroup) {
        for (JsonElement node : nodes) {
            if (node.isJsonPrimitive()) {
                JsonObject element = elements.get(node.getAsString());
                if (parent == null || element == null || element.has("export") && !element.get("export").getAsBoolean()) {
                    continue;
                }
                String type = element.has("type") ? element.get("type").getAsString() : "cube";
                if (!type.equals("cube") || dropsCubes(parent.get("name").getAsString(), top)) {
                    continue;
                }
                JsonObject cube = cube(element, fxGroup, parent.get("name").getAsString());
                if (cube != null) {
                    if (!parent.has("cubes")) {
                        parent.add("cubes", new JsonArray());
                    }
                    parent.getAsJsonArray("cubes").add(cube);
                }
                continue;
            }
            JsonObject outlined = node.getAsJsonObject();
            JsonObject group = groups.getOrDefault(outlined.get("uuid").getAsString(), outlined);
            String name = group.get("name").getAsString();
            String childTop = parent != null && parent.get("name").getAsString().equals("root_rig") ? name : top;
            if (name.equals(CAMERA_GROUP) && childTop != null && childTop.equals(CAMERA_GROUP)) {
                skip(outlined);
                continue;
            }
            if (!names.add(name)) {
                throw new IllegalStateException("two groups are named " + name);
            }
            JsonArray origin = group.has("origin") ? group.getAsJsonArray("origin") : array(0, 0, 0);
            JsonObject bone = new JsonObject();
            bone.addProperty("name", name);
            if (parent != null) {
                bone.addProperty("parent", parent.get("name").getAsString());
            }
            bone.add("pivot", array(-d(origin, 0), d(origin, 1), d(origin, 2)));
            JsonArray rotation = group.has("rotation") ? group.getAsJsonArray("rotation") : null;
            if (rotation != null && (d(rotation, 0) != 0 || d(rotation, 1) != 0 || d(rotation, 2) != 0)) {
                bone.add("rotation", array(-d(rotation, 0), -d(rotation, 1), d(rotation, 2)));
            }
            bones.add(bone);
            String childFx = fxGroup;
            if (childTop != null && FX_GROUP.matcher(childTop).matches()) {
                childFx = childTop;
            }
            walk(outlined.has("children") ? outlined.getAsJsonArray("children") : new JsonArray(), bone, childTop, childFx);
        }
    }

    private void skip(JsonObject outlined) {
        JsonObject group = groups.getOrDefault(outlined.get("uuid").getAsString(), outlined);
        skippedBones.add(group.get("name").getAsString());
        if (outlined.has("children")) {
            for (JsonElement child : outlined.getAsJsonArray("children")) {
                if (child.isJsonObject()) {
                    skip(child.getAsJsonObject());
                }
            }
        }
    }

    /** Armor stand bodies, the server's empty buttons and the recorded rewards are placeholders. */
    private static boolean dropsCubes(String bone, String top) {
        return bone.endsWith("_model") || bone.endsWith("_base_plate") || bone.endsWith("empty_button")
                || top != null && ITEM_GROUP.matcher(top).matches();
    }

    private JsonObject cube(JsonObject element, String fxGroup, String bone) {
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        double[] size = {d(to, 0) - d(from, 0), d(to, 1) - d(from, 1), d(to, 2) - d(from, 2)};
        JsonObject cube = new JsonObject();
        cube.add("origin", array(-(d(from, 0) + size[0]), d(from, 1), d(from, 2)));
        cube.add("size", array(size[0], size[1], size[2]));
        if (element.has("inflate") && element.get("inflate").getAsDouble() != 0) {
            cube.addProperty("inflate", round(element.get("inflate").getAsDouble()));
        }
        JsonArray rotation = element.has("rotation") ? element.getAsJsonArray("rotation") : null;
        if (rotation != null && (d(rotation, 0) != 0 || d(rotation, 1) != 0 || d(rotation, 2) != 0)) {
            JsonArray pivot = element.has("origin") ? element.getAsJsonArray("origin") : array(0, 0, 0);
            cube.add("pivot", array(-d(pivot, 0), d(pivot, 1), d(pivot, 2)));
            cube.add("rotation", array(-d(rotation, 0), -d(rotation, 1), d(rotation, 2)));
        }
        if (element.has("mirror_uv") && element.get("mirror_uv").getAsBoolean()) {
            cube.addProperty("mirror", true);
        }
        String fxEffect = null;
        int fxFrame = 0;
        int fxSlot = 0;
        if (fxGroup != null) {
            Matcher group = FX_GROUP.matcher(fxGroup);
            Matcher texture = FX_TEXTURE.matcher(bone);
            if (group.matches() && texture.find()) {
                fxSlot = Integer.parseInt(group.group(1));
                fxEffect = texture.group(1);
                fxFrame = texture.group(2) == null ? 0 : Integer.parseInt(texture.group(2));
            }
        }
        JsonObject uvs = new JsonObject();
        JsonObject faces = element.has("faces") ? element.getAsJsonObject("faces") : new JsonObject();
        for (String name : FACES) {
            if (!faces.has(name) || faces.getAsJsonObject(name).get("texture") == null
                    || faces.getAsJsonObject(name).get("texture").isJsonNull()) {
                continue;
            }
            JsonObject face = faces.getAsJsonObject(name);
            JsonArray uv = face.getAsJsonArray("uv");
            double u0 = d(uv, 0) * uvScaleX, v0 = d(uv, 1) * uvScaleY, u1 = d(uv, 2) * uvScaleX, v1 = d(uv, 3) * uvScaleY;
            if (u0 == u1 && v0 == v1) {
                continue;
            }
            if (fxEffect != null) {
                int[] region = region(fxSlot, fxEffect, fxFrame, u0, v0, u1, v1);
                // Keep the face's direction, moved onto the new region.
                double nu0 = u0 <= u1 ? region[0] : region[0] + region[2];
                double nu1 = u0 <= u1 ? region[0] + region[2] : region[0];
                double nv0 = v0 <= v1 ? region[1] : region[1] + region[3];
                double nv1 = v0 <= v1 ? region[1] + region[3] : region[1];
                u0 = nu0; u1 = nu1; v0 = nv0; v1 = nv1;
            }
            double[] start = {u0, v0};
            double[] uvSize = {u1 - u0, v1 - v0};
            if (name.equals("up") || name.equals("down")) {
                start = new double[] {u0 + uvSize[0], v0 + uvSize[1]};
                uvSize = new double[] {-uvSize[0], -uvSize[1]};
            }
            JsonObject entry = new JsonObject();
            entry.add("uv", array(start[0], start[1]));
            entry.add("uv_size", array(uvSize[0], uvSize[1]));
            if (face.has("rotation") && face.get("rotation").getAsInt() != 0) {
                entry.addProperty("uv_rotation", face.get("rotation").getAsInt());
            }
            uvs.add(name, entry);
        }
        if (uvs.isEmpty()) {
            return null;
        }
        cube.add("uv", uvs);
        return cube;
    }

    /** A region of its own for one reward's effect frame, packed in rows below the source atlas. */
    private int[] region(int slot, String effect, int frame, double u0, double v0, double u1, double v1) {
        int x = (int) Math.floor(Math.min(u0, u1));
        int y = (int) Math.floor(Math.min(v0, v1));
        int w = Math.max(1, (int) Math.round(Math.abs(u1 - u0)));
        int h = Math.max(1, (int) Math.round(Math.abs(v1 - v0)));
        String key = slot + "/" + effect + "/" + frame + "/" + x + "," + y + "," + w + "," + h;
        return regions.computeIfAbsent(key, k -> {
            if (nextX + w > source.getWidth()) {
                nextX = 0;
                nextY += rowHeight;
                rowHeight = 0;
            }
            int[] placed = {nextX, nextY, w, h, x, y};
            nextX += w;
            rowHeight = Math.max(rowHeight, h);
            JsonObject entry = new JsonObject();
            entry.addProperty("slot", slot);
            entry.addProperty("effect", effect);
            entry.addProperty("frame", frame);
            entry.addProperty("x", placed[0]);
            entry.addProperty("y", placed[1]);
            entry.addProperty("w", w);
            entry.addProperty("h", h);
            layout.add(entry);
            return placed;
        });
    }

    private BufferedImage atlas() {
        int height = nextY + rowHeight;
        int pow = 1;
        while (pow < height) {
            pow <<= 1;
        }
        BufferedImage atlas = new BufferedImage(source.getWidth(), pow, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                atlas.setRGB(x, y, source.getRGB(x, y));
            }
        }
        // Each new region starts as the recorded texture, so it looks as recorded until the client repaints it.
        for (int[] region : regions.values()) {
            for (int dy = 0; dy < region[3]; dy++) {
                for (int dx = 0; dx < region[2]; dx++) {
                    int sx = Math.min(source.getWidth() - 1, region[4] + dx);
                    int sy = Math.min(source.getHeight() - 1, region[5] + dy);
                    atlas.setRGB(region[0] + dx, region[1] + dy, source.getRGB(sx, sy));
                }
            }
        }
        return atlas;
    }

    // Animation

    private JsonObject animations() {
        JsonObject result = new JsonObject();
        for (JsonElement element : project.getAsJsonArray("animations")) {
            JsonObject animation = element.getAsJsonObject();
            JsonObject bonesOut = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("animators").entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                String type = animator.has("type") ? animator.get("type").getAsString() : "bone";
                String name = animator.has("name") ? animator.get("name").getAsString() : null;
                if (!type.equals("bone") || name == null || skippedBones.contains(name) || !names.contains(name) || !animator.has("keyframes")) {
                    continue;
                }
                Map<String, TreeMap<Double, JsonArray>> channels = new LinkedHashMap<>();
                for (JsonElement keyframe : animator.getAsJsonArray("keyframes")) {
                    JsonObject frame = keyframe.getAsJsonObject();
                    String channel = frame.get("channel").getAsString();
                    if (!channel.equals("position") && !channel.equals("rotation") && !channel.equals("scale")) {
                        continue;
                    }
                    JsonObject point = frame.getAsJsonArray("data_points").get(0).getAsJsonObject();
                    channels.computeIfAbsent(channel, c -> new TreeMap<>()).put(round(frame.get("time").getAsDouble()), value(channel, point));
                }
                if (channels.isEmpty()) {
                    continue;
                }
                JsonObject out = new JsonObject();
                channels.forEach((channel, frames) -> {
                    JsonObject times = new JsonObject();
                    frames.forEach((time, value) -> times.add(number(time), value));
                    out.add(channel, times);
                });
                bonesOut.add(name, out);
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("animation_length", animation.get("length").getAsDouble());
            entry.add("bones", bonesOut);
            result.add("animation.badger_crate.open", entry);
            break;
        }
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", result);
        return root;
    }

    private static JsonArray value(String channel, JsonObject point) {
        boolean[] negate = switch (channel) {
            case "position" -> new boolean[] {true, false, false};
            case "rotation" -> new boolean[] {true, true, false};
            default -> new boolean[] {false, false, false};
        };
        JsonArray values = new JsonArray();
        String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            String raw = point.has(axes[i]) ? point.get(axes[i]).getAsString() : "0";
            try {
                double number = raw.isEmpty() ? 0 : Double.parseDouble(raw);
                values.add(round(negate[i] ? -number : number));
            } catch (NumberFormatException molang) {
                values.add(negate[i] ? "-(" + raw + ")" : raw);
            }
        }
        return values;
    }

    // Camera

    /** The recorded player's eye, one frame per tick: x, y, z in blocks from the model origin, Java yaw and pitch. */
    private JsonObject camera() {
        JsonObject animation = project.getAsJsonArray("animations").get(0).getAsJsonObject();
        double length = animation.get("length").getAsDouble();
        JsonObject player = group(CAMERA_GROUP);
        JsonObject head = null;
        for (JsonObject group : groups.values()) {
            String name = group.get("name").getAsString();
            if (name.startsWith("player_") && name.endsWith("_head")) {
                head = group;
            }
        }
        JsonObject root = group("root_rig");
        Track playerTrack = track(animation, player);
        Track headTrack = track(animation, head);
        Track rootTrack = track(animation, root);
        JsonArray origin = player.getAsJsonArray("origin");
        double bodyY = rotation(player, 1);
        double headX = rotation(head, 0);
        double headY = rotation(head, 1);
        JsonArray frames = new JsonArray();
        int ticks = (int) Math.ceil(length * 20);
        for (int tick = 0; tick <= ticks; tick++) {
            double t = tick / 20.0;
            double[] pos = playerTrack.at("position", t);
            double[] rootPos = rootTrack.at("position", t);
            double[] bodyRot = playerTrack.at("rotation", t);
            double[] headRot = headTrack.at("rotation", t);
            double x = (d(origin, 0) + pos[0] + rootPos[0]) / 16.0;
            double y = (d(origin, 1) + pos[1] + rootPos[1]) / 16.0 + EYE_HEIGHT;
            double z = (d(origin, 2) + pos[2] + rootPos[2]) / 16.0;
            // The recorder wrote a yaw as Bedrock's yRot - 180 (Blockbench's 180 - yRot) and the head's yaw off the body
            // and pitch as Bedrock's pose (Blockbench's negated).
            double yaw = 180.0 - (bodyY + bodyRot[1]) - (headY + headRot[1]);
            double pitch = -(headX + headRot[0]);
            frames.add(array(x, y, z, wrap(yaw), pitch));
        }
        JsonObject camera = new JsonObject();
        camera.addProperty("ticks_per_frame", 1);
        camera.add("frames", frames);
        return camera;
    }

    private JsonObject group(String name) {
        for (JsonObject group : groups.values()) {
            if (group.get("name").getAsString().equals(name)) {
                return group;
            }
        }
        throw new IllegalStateException("no group " + name);
    }

    private static double rotation(JsonObject group, int axis) {
        return group != null && group.has("rotation") ? d(group.getAsJsonArray("rotation"), axis) : 0;
    }

    private Track track(JsonObject animation, JsonObject group) {
        Track track = new Track();
        if (group == null) {
            return track;
        }
        JsonElement animator = animation.getAsJsonObject("animators").get(group.get("uuid").getAsString());
        if (animator == null || !animator.getAsJsonObject().has("keyframes")) {
            return track;
        }
        for (JsonElement keyframe : animator.getAsJsonObject().getAsJsonArray("keyframes")) {
            JsonObject frame = keyframe.getAsJsonObject();
            JsonObject point = frame.getAsJsonArray("data_points").get(0).getAsJsonObject();
            double[] value = new double[3];
            String[] axes = {"x", "y", "z"};
            for (int i = 0; i < 3; i++) {
                try {
                    value[i] = point.has(axes[i]) ? Double.parseDouble(point.get(axes[i]).getAsString()) : 0;
                } catch (NumberFormatException molang) {
                    value[i] = 0;
                }
            }
            track.channels.computeIfAbsent(frame.get("channel").getAsString(), c -> new TreeMap<>()).put(frame.get("time").getAsDouble(), value);
        }
        return track;
    }

    private static final class Track {
        final Map<String, TreeMap<Double, double[]>> channels = new HashMap<>();

        double[] at(String channel, double time) {
            TreeMap<Double, double[]> frames = channels.get(channel);
            if (frames == null || frames.isEmpty()) {
                return new double[3];
            }
            Map.Entry<Double, double[]> before = frames.floorEntry(time);
            Map.Entry<Double, double[]> after = frames.ceilingEntry(time);
            if (before == null) {
                return after.getValue();
            }
            if (after == null || after.getKey().equals(before.getKey())) {
                return before.getValue();
            }
            double f = (time - before.getKey()) / (after.getKey() - before.getKey());
            double[] a = before.getValue(), b = after.getValue();
            return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
        }
    }

    // Helpers

    private static double wrap(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped >= 180.0) {
            wrapped -= 360.0;
        }
        if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    private static double d(JsonArray array, int index) {
        JsonElement element = array.get(index);
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            return text.isEmpty() ? 0 : Double.parseDouble(text);
        }
        return element.getAsDouble();
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static JsonArray array(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            double rounded = round(value);
            if (rounded == Math.rint(rounded) && Math.abs(rounded) < 1e15) {
                array.add((long) rounded);
            } else {
                array.add(rounded);
            }
        }
        return array;
    }
}
