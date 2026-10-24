package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Rotations;
import net.minecraft.world.phys.Vec3;

import com.apeira.uicapture.ArmorStandRecorder.Event;
import com.apeira.uicapture.ArmorStandRecorder.HitboxSnapshot;
import com.apeira.uicapture.ArmorStandRecorder.HitboxTrack;
import com.apeira.uicapture.ArmorStandRecorder.Recording;
import com.apeira.uicapture.ArmorStandRecorder.Snapshot;
import com.apeira.uicapture.ArmorStandRecorder.Track;

/**
 * Writes an {@link ArmorStandRecorder} recording to {@code logs/apeira_armor_stand_capture/<time>_<name>/}:
 * <ul>
 * <li>{@code recording.json}: the area, every stand's state each tick it was in the area, and the events.</li>
 * <li>{@code <name>.geo.json}: a GeckoLib / Blockbench (Bedrock) model with one armor stand per recorded stand,
 * {@code stand_<n>} with the vanilla armor stand's parts ({@code _head}, {@code _body}, {@code _left_arm},
 * {@code _right_arm}, {@code _left_leg}, {@code _right_leg}, {@code _base_plate}), their cubes in {@code <part>_model}
 * bones, and an empty bone per equipment slot ({@code stand_<n>_head_item}, {@code _chest_item}, {@code _legs_item},
 * {@code _feet_item}, {@code _mainhand_item}, {@code _offhand_item}) where an item model can be attached.</li>
 * <li>{@code <name>.animation.json}: the GeckoLib animation {@code animation.<name>.recorded}. The stand bone's position
 * (relative to the area's bottom centre) and yaw, each part's pose, scale 0 for a stand outside the area (0.5 for small
 * stands), for the cubes of an invisible stand (and armless or plateless ones) and for empty slots; and in
 * {@code timeline} one instruction per event (see {@link #instruction}), which GeckoLib passes to a custom instruction
 * keyframe handler.</li>
 * </ul>
 * Each recorded {@code minecraft:interaction} entity is an {@code interaction_<n>} bone with a locator of the same name at
 * its position (the bottom centre of its hitbox) and an {@code interaction_<n>_hitbox} child holding a cube of its first
 * width and height; the animation moves the bone, scales it to 0 outside the area and scales the hitbox by the size
 * changes. {@code recording.json} lists each interaction's frames (position, yaw, width, height, response) and its offset
 * from the nearest recorded stand.
 * Discrete changes are two keyframes {@value #STEP} seconds apart, so they are instant in any Bedrock-format reader.
 * Values are Bedrock's: x is mirrored (GeckoLib negates it), rotations are in degrees as GeckoLib reads them, so an
 * armor stand pose is written as it is and a yaw as {@code yRot - 180}. Angles are unwrapped so a turn past 180 degrees
 * does not spin back.
 */
public final class ArmorStandExporter {
    private static final String FOLDER = "logs/apeira_armor_stand_capture";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();
    private static final double STEP = 0.001;
    private static final String[] PARTS = {"head", "body", "left_arm", "right_arm", "left_leg", "right_leg"};
    private static final String[] SLOT_NAMES = {"head", "chest", "legs", "feet", "mainhand", "offhand"};

    private ArmorStandExporter() {
    }

    public static Path write(Recording recording) throws IOException {
        String name = recording.name().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        Path folder = FabricLoader.getInstance().getGameDir().resolve(FOLDER).resolve(LocalDateTime.now().format(TIME) + "_" + name);
        Files.createDirectories(folder);
        Vec3 origin = new Vec3((recording.min().getX() + recording.max().getX() + 1) / 2.0, recording.min().getY(),
                (recording.min().getZ() + recording.max().getZ() + 1) / 2.0);
        Files.writeString(folder.resolve("recording.json"), GSON.toJson(raw(recording, origin)), StandardCharsets.UTF_8);
        Files.writeString(folder.resolve(name + ".geo.json"), GSON.toJson(geometry(recording, name)), StandardCharsets.UTF_8);
        Files.writeString(folder.resolve(name + ".animation.json"), GSON.toJson(animation(recording, name, origin)), StandardCharsets.UTF_8);
        return folder;
    }

    // recording.json

    private static JsonObject raw(Recording recording, Vec3 origin) {
        JsonObject root = new JsonObject();
        root.addProperty("name", recording.name());
        root.addProperty("written_at", LocalDateTime.now().toString());
        root.add("min", array(recording.min().getX(), recording.min().getY(), recording.min().getZ()));
        root.add("max", array(recording.max().getX(), recording.max().getY(), recording.max().getZ()));
        root.add("origin", array(origin.x, origin.y, origin.z));
        root.addProperty("ticks", recording.ticks());
        root.addProperty("conventions", "Raw values are Java's: world positions, yRot (0 = south), armor stand poses in degrees. "
                + "Items list what picks a model: id, item_model, custom_model_data, dyed_color. Stand items are in its first event "
                + "and every change in a swap event.");
        JsonArray stands = new JsonArray();
        for (Track track : recording.tracks()) {
            JsonObject stand = new JsonObject();
            stand.addProperty("bone", track.bone);
            stand.addProperty("uuid", track.uuid.toString());
            stand.addProperty("entity_id", track.entityId);
            if (track.name != null) {
                stand.addProperty("custom_name", track.name);
            }
            stand.addProperty("first_tick", track.firstTick);
            JsonArray frames = new JsonArray();
            track.frames.forEach((tick, s) -> {
                JsonObject frame = new JsonObject();
                frame.addProperty("tick", tick);
                frame.add("pos", array(s.pos().x, s.pos().y, s.pos().z));
                frame.addProperty("yaw", s.yaw());
                frame.addProperty("invisible", s.invisible());
                frame.addProperty("small", s.small());
                frame.addProperty("marker", s.marker());
                frame.addProperty("show_arms", s.showArms());
                frame.addProperty("base_plate", s.basePlate());
                Rotations[] poses = poses(s);
                JsonObject pose = new JsonObject();
                for (int i = 0; i < PARTS.length; i++) {
                    pose.add(PARTS[i], array(poses[i].x(), poses[i].y(), poses[i].z()));
                }
                frame.add("pose", pose);
                frames.add(frame);
            });
            stand.add("frames", frames);
            stands.add(stand);
        }
        root.add("stands", stands);
        JsonArray hitboxes = new JsonArray();
        for (HitboxTrack track : recording.hitboxes()) {
            JsonObject hitbox = new JsonObject();
            hitbox.addProperty("bone", track.bone);
            hitbox.addProperty("type", "minecraft:interaction");
            hitbox.addProperty("uuid", track.uuid.toString());
            hitbox.addProperty("entity_id", track.entityId);
            hitbox.addProperty("first_tick", track.firstTick);
            if (!track.frames.isEmpty()) {
                HitboxSnapshot first = track.frames.values().iterator().next();
                hitbox.add("relative_to_origin", array(first.pos().x - origin.x, first.pos().y - origin.y, first.pos().z - origin.z));
                nearestStand(recording, track, first).ifPresent(near -> hitbox.add("nearest_stand", near));
            }
            JsonArray frames = new JsonArray();
            track.frames.forEach((tick, h) -> {
                JsonObject frame = new JsonObject();
                frame.addProperty("tick", tick);
                frame.add("pos", array(h.pos().x, h.pos().y, h.pos().z));
                frame.addProperty("yaw", h.yaw());
                frame.addProperty("width", h.width());
                frame.addProperty("height", h.height());
                frame.addProperty("response", h.response());
                frames.add(frame);
            });
            hitbox.add("frames", frames);
            hitboxes.add(hitbox);
        }
        root.add("interactions", hitboxes);
        JsonArray events = new JsonArray();
        for (Event event : recording.events()) {
            JsonObject json = new JsonObject();
            json.addProperty("tick", event.tick());
            json.addProperty("type", event.type());
            json.addProperty("bone", event.bone());
            for (Map.Entry<String, JsonElement> entry : event.details().entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
            events.add(json);
        }
        root.add("events", events);
        return root;
    }

    /** The stand closest to an interaction in the interaction's first tick: its bone, the offset to the interaction (blocks, Java axes) and that offset turned into the stand's own frame (x left, z forward). */
    private static java.util.Optional<JsonObject> nearestStand(Recording recording, HitboxTrack track, HitboxSnapshot first) {
        int tick = track.frames.keySet().iterator().next();
        Track best = null;
        Snapshot bestFrame = null;
        double bestDistance = Double.MAX_VALUE;
        for (Track stand : recording.tracks()) {
            Snapshot frame = stand.frames.get(tick);
            if (frame == null && !stand.frames.isEmpty()) {
                frame = stand.frames.values().iterator().next();
            }
            if (frame != null && frame.pos().distanceToSqr(first.pos()) < bestDistance) {
                best = stand;
                bestFrame = frame;
                bestDistance = frame.pos().distanceToSqr(first.pos());
            }
        }
        if (best == null) {
            return java.util.Optional.empty();
        }
        Vec3 offset = first.pos().subtract(bestFrame.pos());
        double yaw = Math.toRadians(bestFrame.yaw());
        // A stand with yaw 0 faces +z (south) and its left is +x.
        double localX = offset.x * Math.cos(yaw) + offset.z * Math.sin(yaw);
        double localZ = -offset.x * Math.sin(yaw) + offset.z * Math.cos(yaw);
        JsonObject near = new JsonObject();
        near.addProperty("bone", best.bone);
        near.add("offset", array(offset.x, offset.y, offset.z));
        near.add("offset_in_stand_frame", array(localX, offset.y, localZ));
        near.addProperty("yaw_difference", wrap(first.yaw() - bestFrame.yaw()));
        return java.util.Optional.of(near);
    }

    private static Rotations[] poses(Snapshot s) {
        return new Rotations[] {s.head(), s.body(), s.leftArm(), s.rightArm(), s.leftLeg(), s.rightLeg()};
    }

    // <name>.geo.json

    private static JsonObject geometry(Recording recording, String name) {
        JsonArray bones = new JsonArray();
        for (Track track : recording.tracks()) {
            String b = track.bone;
            bones.add(bone(b, null, 0, 0, 0));
            bones.add(bone(b + "_head", b, 0, 23, 0));
            bones.add(bone(b + "_head_model", b + "_head", 0, 23, 0, cube(-1, 23, -1, 2, 7, 2, 0, 0, false)));
            bones.add(bone(b + "_body", b, 0, 24, 0));
            bones.add(bone(b + "_body_model", b + "_body", 0, 24, 0,
                    cube(-6, 21, -1.5, 12, 3, 3, 0, 26, false), cube(-3, 14, -1, 2, 7, 2, 16, 0, false),
                    cube(1, 14, -1, 2, 7, 2, 48, 16, false), cube(-4, 12, -1, 8, 2, 2, 0, 48, false)));
            bones.add(bone(b + "_right_arm", b, -5, 22, 0));
            bones.add(bone(b + "_right_arm_model", b + "_right_arm", -5, 22, 0, cube(-7, 12, -1, 2, 12, 2, 24, 0, false)));
            bones.add(bone(b + "_left_arm", b, 5, 22, 0));
            bones.add(bone(b + "_left_arm_model", b + "_left_arm", 5, 22, 0, cube(5, 12, -1, 2, 12, 2, 32, 16, true)));
            bones.add(bone(b + "_right_leg", b, -1.9, 12, 0));
            bones.add(bone(b + "_right_leg_model", b + "_right_leg", -1.9, 12, 0, cube(-2.9, 1, -1, 2, 11, 2, 8, 0, false)));
            bones.add(bone(b + "_left_leg", b, 1.9, 12, 0));
            bones.add(bone(b + "_left_leg_model", b + "_left_leg", 1.9, 12, 0, cube(0.9, 1, -1, 2, 11, 2, 40, 16, true)));
            bones.add(bone(b + "_base_plate", b, 0, 0, 0, cube(-6, 0, -6, 12, 1, 12, 0, 32, false)));
            // Where the vanilla renderer draws each slot's item: the head item on the head, held items in the hands.
            bones.add(bone(b + "_head_item", b + "_head", 0, 23, 0));
            bones.add(bone(b + "_chest_item", b + "_body", 0, 24, 0));
            bones.add(bone(b + "_legs_item", b, 0, 12, 0));
            bones.add(bone(b + "_feet_item", b, 0, 1, 0));
            bones.add(bone(b + "_mainhand_item", b + "_right_arm", -6, 13, 0));
            bones.add(bone(b + "_offhand_item", b + "_left_arm", 6, 13, 0));
        }
        for (HitboxTrack track : recording.hitboxes()) {
            if (track.frames.isEmpty()) {
                continue;
            }
            HitboxSnapshot first = track.frames.values().iterator().next();
            JsonObject bone = bone(track.bone, null, 0, 0, 0);
            JsonObject locators = new JsonObject();
            locators.add(track.bone, array(0, 0, 0));
            bone.add("locators", locators);
            bones.add(bone);
            double w = first.width() * 16, h = first.height() * 16;
            bones.add(bone(track.bone + "_hitbox", track.bone, 0, 0, 0, cube(-w / 2, 0, -w / 2, w, h, w, 0, 0, false)));
        }
        int width = Math.max(recording.max().getX() - recording.min().getX(), recording.max().getZ() - recording.min().getZ()) + 3;
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry." + name);
        description.addProperty("texture_width", 64);
        description.addProperty("texture_height", 64);
        description.addProperty("visible_bounds_width", width);
        description.addProperty("visible_bounds_height", recording.max().getY() - recording.min().getY() + 3);
        description.add("visible_bounds_offset", array(0, (recording.max().getY() - recording.min().getY() + 1) / 2.0, 0));
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray list = new JsonArray();
        list.add(geometry);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.12.0");
        root.add("minecraft:geometry", list);
        return root;
    }

    private static JsonObject bone(String name, @Nullable String parent, double x, double y, double z, JsonObject... cubes) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        if (parent != null) {
            bone.addProperty("parent", parent);
        }
        bone.add("pivot", array(x, y, z));
        if (cubes.length > 0) {
            JsonArray list = new JsonArray();
            for (JsonObject cube : cubes) {
                list.add(cube);
            }
            bone.add("cubes", list);
        }
        return bone;
    }

    /** A cube of the vanilla armor stand model (Java's {@code ArmorStandModel}, moved to Bedrock's y-up frame), box UV on its 64x64 texture. */
    private static JsonObject cube(double x, double y, double z, double w, double h, double d, int u, int v, boolean mirror) {
        JsonObject cube = new JsonObject();
        cube.add("origin", array(x, y, z));
        cube.add("size", array(w, h, d));
        cube.add("uv", array(u, v));
        if (mirror) {
            cube.addProperty("mirror", true);
        }
        return cube;
    }

    // <name>.animation.json

    private static JsonObject animation(Recording recording, String name, Vec3 origin) {
        int ticks = Math.max(1, recording.ticks());
        JsonObject bones = new JsonObject();
        for (Track track : recording.tracks()) {
            String b = track.bone;
            // Ticks the stand was in the area; the others hold its last state, hidden.
            Snapshot[] states = new Snapshot[ticks];
            Snapshot last = null;
            for (int t = 0; t < ticks; t++) {
                Snapshot s = track.frames.get(t);
                if (s != null) {
                    last = s;
                }
                states[t] = s != null ? s : last;
            }
            Snapshot first = track.frames.values().iterator().next();
            for (int t = 0; t < ticks && states[t] == null; t++) {
                states[t] = first;
            }
            boolean[] present = new boolean[ticks];
            for (int t = 0; t < ticks; t++) {
                present[t] = track.frames.containsKey(t);
            }

            JsonObject root = new JsonObject();
            root.add("position", continuous(ticks, t -> new double[] {
                    -(states[t].pos().x - origin.x) * 16, (states[t].pos().y - origin.y) * 16, (states[t].pos().z - origin.z) * 16}, false));
            root.add("rotation", continuous(ticks, t -> new double[] {0, states[t].yaw() - 180, 0}, true));
            root.add("scale", discrete(ticks, t -> present[t] ? states[t].small() ? 0.5 : 1.0 : 0.0));
            bones.add(b, root);

            for (int i = 0; i < PARTS.length; i++) {
                int part = i;
                JsonObject bone = new JsonObject();
                bone.add("rotation", continuous(ticks, t -> {
                    Rotations r = poses(states[t])[part];
                    return new double[] {r.x(), r.y(), r.z()};
                }, true));
                bones.add(b + "_" + PARTS[i], bone);
                boolean arm = PARTS[i].endsWith("_arm");
                JsonObject model = new JsonObject();
                model.add("scale", discrete(ticks, t -> states[t].invisible() || arm && !states[t].showArms() ? 0.0 : 1.0));
                bones.add(b + "_" + PARTS[i] + "_model", model);
            }
            JsonObject plate = new JsonObject();
            plate.add("scale", discrete(ticks, t -> states[t].invisible() || !states[t].basePlate() ? 0.0 : 1.0));
            bones.add(b + "_base_plate", plate);
            for (int i = 0; i < SLOT_NAMES.length; i++) {
                int slot = i;
                JsonObject item = new JsonObject();
                item.add("scale", discrete(ticks, t -> states[t].items()[slot] == null ? 0.0 : 1.0));
                bones.add(b + "_" + SLOT_NAMES[i] + "_item", item);
            }
        }
        for (HitboxTrack track : recording.hitboxes()) {
            if (track.frames.isEmpty()) {
                continue;
            }
            HitboxSnapshot[] states = new HitboxSnapshot[ticks];
            HitboxSnapshot first = track.frames.values().iterator().next();
            HitboxSnapshot last = first;
            for (int t = 0; t < ticks; t++) {
                HitboxSnapshot h = track.frames.get(t);
                if (h != null) {
                    last = h;
                }
                states[t] = t < track.firstTick && h == null ? first : last;
            }
            JsonObject root = new JsonObject();
            root.add("position", continuous(ticks, t -> new double[] {
                    -(states[t].pos().x - origin.x) * 16, (states[t].pos().y - origin.y) * 16, (states[t].pos().z - origin.z) * 16}, false));
            root.add("scale", discrete(ticks, t -> track.frames.containsKey(t) ? 1.0 : 0.0));
            bones.add(track.bone, root);
            JsonObject hitbox = new JsonObject();
            hitbox.add("scale", continuous(ticks, t -> {
                double sw = first.width() == 0 ? 1 : states[t].width() / first.width();
                double sh = first.height() == 0 ? 1 : states[t].height() / first.height();
                return new double[] {sw, sh, sw};
            }, false));
            bones.add(track.bone + "_hitbox", hitbox);
        }
        JsonObject timeline = new JsonObject();
        Map<Integer, JsonArray> byTick = new TreeMap<>();
        for (Event event : recording.events()) {
            byTick.computeIfAbsent(event.tick(), t -> new JsonArray()).add(instruction(event));
        }
        byTick.forEach((tick, list) -> timeline.add(time(tick), list));

        JsonObject clip = new JsonObject();
        clip.addProperty("animation_length", ticks / 20.0);
        clip.add("bones", bones);
        clip.add("timeline", timeline);
        JsonObject animations = new JsonObject();
        animations.add("animation." + name + ".recorded", clip);
        JsonObject root = new JsonObject();
        root.addProperty("format_version", "1.8.0");
        root.add("animations", animations);
        return root;
    }

    /**
     * An event as a timeline instruction: {@code <type> <bone>}, and for swaps {@code swap <bone> <slot> <item JSON or empty>};
     * spawns, entries and the first tick's stands add their state's JSON (flags and items).
     */
    static String instruction(Event event) {
        StringBuilder text = new StringBuilder(event.type()).append(' ').append(event.bone());
        if (event.type().equals("swap")) {
            JsonElement to = event.details().get("to");
            text.append(' ').append(event.details().get("slot").getAsString()).append(' ')
                    .append(to == null || to.isJsonNull() ? "empty" : COMPACT.toJson(to));
        } else if (!event.details().isEmpty()) {
            text.append(' ').append(COMPACT.toJson(event.details()));
        }
        return text.toString();
    }

    /** Linear keyframes at the ends of every run of equal values, so held values stay held. */
    private static JsonObject continuous(int ticks, Function<Integer, double[]> value, boolean angles) {
        List<double[]> values = new ArrayList<>(ticks);
        for (int t = 0; t < ticks; t++) {
            double[] v = value.apply(t);
            if (angles && t > 0) {
                double[] previous = values.get(t - 1);
                for (int i = 0; i < v.length; i++) {
                    v[i] = previous[i] + wrap(v[i] - previous[i]);
                }
            }
            values.add(v);
        }
        JsonObject frames = new JsonObject();
        for (int t = 0; t < ticks; t++) {
            boolean changesIn = t == 0 || !same(values.get(t), values.get(t - 1));
            boolean changesOut = t == ticks - 1 || !same(values.get(t), values.get(t + 1));
            if (changesIn || changesOut) {
                frames.add(time(t), array(values.get(t)));
            }
        }
        return frames;
    }

    /** Instant keyframes: the old value {@value #STEP} seconds before each change and the new one at it. */
    private static JsonObject discrete(int ticks, Function<Integer, Double> value) {
        JsonObject frames = new JsonObject();
        double previous = value.apply(0);
        frames.add(time(0), array(previous, previous, previous));
        for (int t = 1; t < ticks; t++) {
            double v = value.apply(t);
            if (v != previous) {
                frames.add(String.format(Locale.ROOT, "%.4f", t / 20.0 - STEP), array(previous, previous, previous));
                frames.add(time(t), array(v, v, v));
                previous = v;
            }
        }
        return frames;
    }

    private static double wrap(double degrees) {
        double d = degrees % 360;
        return d > 180 ? d - 360 : d < -180 ? d + 360 : d;
    }

    private static boolean same(double[] a, double[] b) {
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > 1.0E-4) {
                return false;
            }
        }
        return true;
    }

    private static String time(int tick) {
        return String.format(Locale.ROOT, "%.4f", tick / 20.0);
    }

    private static JsonArray array(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            // Whole numbers without ".0", others rounded to 4 places.
            double rounded = Math.round(value * 10000) / 10000.0;
            if (rounded == Math.rint(rounded) && Math.abs(rounded) < 1.0E9) {
                array.add((long) rounded);
            } else {
                array.add(rounded);
            }
        }
        return array;
    }
}
