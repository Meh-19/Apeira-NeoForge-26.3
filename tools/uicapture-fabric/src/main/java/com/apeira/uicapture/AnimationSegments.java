package com.apeira.uicapture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.core.Rotations;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;

import com.apeira.uicapture.ArmorStandRecorder.Event;
import com.apeira.uicapture.ArmorStandRecorder.HitboxSnapshot;
import com.apeira.uicapture.ArmorStandRecorder.HitboxTrack;
import com.apeira.uicapture.ArmorStandRecorder.PropSnapshot;
import com.apeira.uicapture.ArmorStandRecorder.PropTrack;
import com.apeira.uicapture.ArmorStandRecorder.Recording;
import com.apeira.uicapture.ArmorStandRecorder.Snapshot;
import com.apeira.uicapture.ArmorStandRecorder.Track;

/**
 * Finds the loop in a recording and slices it into clips. Two ticks match when every stand, interaction and prop is in
 * the area in both or neither and, where present, its position (within {@link #POSITION} blocks), angles (within
 * {@link #ANGLE} degrees), transformation (translation and scale within {@link #SCALE}, rotations within {@link #ROTATION}
 * of the same turn), flags, items and state are the same. The loop is the shortest period of at least {@link #MIN_PERIOD}
 * ticks with motion that is seen repeated in full at least once, starting as early as possible; the ticks before it are the
 * transition and those after the repeats end the action. A recording with no repeat whose first and last ticks match and
 * that moves in between is one loop; a recording that never moves is not split.
 */
final class AnimationSegments {
    static final double POSITION = 0.01;
    static final double ANGLE = 0.5;
    static final double SCALE = 0.001;
    static final double ROTATION = 1e-5;
    static final int MIN_PERIOD = 10;
    /** Tick comparisons the search may make before it gives up (long recordings). */
    private static final long BUDGET = 50_000_000L;

    /** A clip: its name ({@code transition}, {@code loop}, {@code action}), first tick and tick count (the loop's last frame repeats its first). */
    record Segment(String name, int from, int ticks, boolean loop) {
    }

    /** The clips found, the loop's period and repeats, and why there are none. */
    record Result(List<Segment> segments, int period, int repeats, String note) {
    }

    private AnimationSegments() {
    }

    static Result find(Recording recording) {
        int n = recording.ticks();
        if (n < MIN_PERIOD + 1) {
            return new Result(List.of(), 0, 0, "recording too short (" + n + " ticks)");
        }
        // moves[t]: whether tick t + 1 differs from tick t; prefix sums tell whether a range moves.
        int[] moving = new int[n];
        for (int t = 0; t + 1 < n; t++) {
            moving[t + 1] = moving[t] + (same(recording, t, t + 1) ? 0 : 1);
        }
        if (moving[n - 1] == 0) {
            return new Result(List.of(), 0, 0, "nothing moves");
        }
        long budget = BUDGET;
        for (int period = MIN_PERIOD; period * 2 <= n; period++) {
            for (int start = 0; start + 2 * period <= n; start++) {
                if (moving[start + period] - moving[start] == 0) {
                    continue;
                }
                boolean repeats = true;
                for (int t = start; t <= start + period && t + period < n && repeats; t++) {
                    repeats = same(recording, t, t + period);
                    if (--budget < 0) {
                        return new Result(List.of(), 0, 0, "recording too long to search");
                    }
                }
                if (!repeats) {
                    continue;
                }
                int end = start + 2 * period;
                while (end < n && same(recording, end, end - period)) {
                    end++;
                }
                List<Segment> segments = new ArrayList<>();
                if (start > 0) {
                    segments.add(new Segment("transition", 0, start + 1, false));
                }
                segments.add(new Segment("loop", start, period + 1, true));
                if (n - end >= 2) {
                    segments.add(new Segment("action", end - 1, n - end + 1, false));
                }
                return new Result(segments, period, (end - start) / period, "");
            }
        }
        if (same(recording, 0, n - 1)) {
            return new Result(List.of(new Segment("loop", 0, n, true)), n - 1, 1, "");
        }
        return new Result(List.of(), 0, 0, "no repeat found");
    }

    /** The recording's ticks {@code from} to {@code from + ticks - 1}, moved to start at 0. */
    static Recording slice(Recording recording, int from, int ticks) {
        List<Track> tracks = new ArrayList<>();
        for (Track track : recording.tracks()) {
            Track copy = new Track(track.uuid, track.bone, track.entityId, track.name, Math.max(0, track.firstTick - from));
            shift(track.frames, copy.frames, from, ticks);
            tracks.add(copy);
        }
        List<HitboxTrack> hitboxes = new ArrayList<>();
        for (HitboxTrack track : recording.hitboxes()) {
            HitboxTrack copy = new HitboxTrack(track.uuid, track.bone, track.entityId, Math.max(0, track.firstTick - from));
            shift(track.frames, copy.frames, from, ticks);
            hitboxes.add(copy);
        }
        List<PropTrack> props = new ArrayList<>();
        for (PropTrack track : recording.props()) {
            PropTrack copy = new PropTrack(track.uuid, track.bone, track.kind, track.type, track.entityId, Math.max(0, track.firstTick - from));
            shift(track.frames, copy.frames, from, ticks);
            props.add(copy);
        }
        List<Event> events = new ArrayList<>();
        for (Event event : recording.events()) {
            if (event.tick() >= from && event.tick() < from + ticks) {
                events.add(new Event(event.tick() - from, event.type(), event.bone(), event.details()));
            }
        }
        return new Recording(recording.name(), recording.min(), recording.max(), ticks, tracks, hitboxes, props, events);
    }

    private static <T> void shift(Map<Integer, T> frames, Map<Integer, T> into, int from, int ticks) {
        Map<Integer, T> sorted = new LinkedHashMap<>();
        for (int t = 0; t < ticks; t++) {
            T frame = frames.get(from + t);
            if (frame != null) {
                sorted.put(t, frame);
            }
        }
        into.putAll(sorted);
    }

    static boolean same(Recording recording, int a, int b) {
        for (Track track : recording.tracks()) {
            Snapshot x = track.frames.get(a), y = track.frames.get(b);
            if ((x == null) != (y == null) || x != null && !same(x, y)) {
                return false;
            }
        }
        for (HitboxTrack track : recording.hitboxes()) {
            HitboxSnapshot x = track.frames.get(a), y = track.frames.get(b);
            if ((x == null) != (y == null) || x != null && !(near(x.pos(), y.pos()) && Math.abs(x.width() - y.width()) <= SCALE
                    && Math.abs(x.height() - y.height()) <= SCALE && x.response() == y.response())) {
                return false;
            }
        }
        for (PropTrack track : recording.props()) {
            PropSnapshot x = track.frames.get(a), y = track.frames.get(b);
            if ((x == null) != (y == null) || x != null && !same(x, y)) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(Snapshot x, Snapshot y) {
        if (!near(x.pos(), y.pos()) || !angle(x.yaw(), y.yaw()) || x.invisible() != y.invisible() || x.small() != y.small()
                || x.marker() != y.marker() || x.showArms() != y.showArms() || x.basePlate() != y.basePlate()) {
            return false;
        }
        Rotations[] p = {x.head(), x.body(), x.leftArm(), x.rightArm(), x.leftLeg(), x.rightLeg()};
        Rotations[] q = {y.head(), y.body(), y.leftArm(), y.rightArm(), y.leftLeg(), y.rightLeg()};
        for (int i = 0; i < p.length; i++) {
            if (!angle(p[i].x(), q[i].x()) || !angle(p[i].y(), q[i].y()) || !angle(p[i].z(), q[i].z())) {
                return false;
            }
        }
        for (int i = 0; i < x.items().length; i++) {
            if (!Objects.equals(x.items()[i], y.items()[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean same(PropSnapshot x, PropSnapshot y) {
        if (!near(x.pos(), y.pos()) || !angle(x.yaw(), y.yaw()) || !angle(x.pitch(), y.pitch()) || !x.state().equals(y.state())) {
            return false;
        }
        AreaEntities.@Nullable Transform s = x.transform(), u = y.transform();
        if (s == null || u == null) {
            return s == u;
        }
        return near(s.translation(), u.translation(), POSITION) && near(s.scale(), u.scale(), SCALE)
                && turn(s.left(), u.left()) && turn(s.right(), u.right());
    }

    private static boolean near(Vec3 a, Vec3 b) {
        return Math.abs(a.x - b.x) <= POSITION && Math.abs(a.y - b.y) <= POSITION && Math.abs(a.z - b.z) <= POSITION;
    }

    private static boolean near(Vector3fc a, Vector3fc b, double tolerance) {
        return Math.abs(a.x() - b.x()) <= tolerance && Math.abs(a.y() - b.y()) <= tolerance && Math.abs(a.z() - b.z()) <= tolerance;
    }

    private static boolean angle(float a, float b) {
        return Math.abs(net.minecraft.util.Mth.wrapDegrees(a - b)) <= ANGLE;
    }

    /** Whether two rotations are the same turn (q and -q are). */
    private static boolean turn(Quaternionfc a, Quaternionfc b) {
        return 1 - Math.abs(a.x() * b.x() + a.y() * b.y() + a.z() * b.z() + a.w() * b.w()) <= ROTATION;
    }

    /** The result as JSON for {@code segments.json}: tolerances, period, repeats and each clip's file, ticks and seconds. */
    static JsonObject json(Result result, String name) {
        JsonObject json = new JsonObject();
        json.addProperty("period_ticks", result.period());
        json.addProperty("repeats", result.repeats());
        if (!result.note().isEmpty()) {
            json.addProperty("note", result.note());
        }
        JsonObject tolerance = new JsonObject();
        tolerance.addProperty("position_blocks", POSITION);
        tolerance.addProperty("angle_degrees", ANGLE);
        tolerance.addProperty("scale", SCALE);
        tolerance.addProperty("rotation", ROTATION);
        tolerance.addProperty("min_period_ticks", MIN_PERIOD);
        json.add("tolerance", tolerance);
        com.google.gson.JsonArray clips = new com.google.gson.JsonArray();
        for (Segment segment : result.segments()) {
            JsonObject clip = new JsonObject();
            clip.addProperty("name", segment.name());
            clip.addProperty("file", file(name, segment));
            clip.addProperty("animation", "animation." + name + "." + segment.name());
            clip.addProperty("from_tick", segment.from());
            clip.addProperty("to_tick", segment.from() + segment.ticks() - 1);
            clip.addProperty("seconds", length(segment) / 20.0);
            clip.addProperty("loop", segment.loop());
            clips.add(clip);
        }
        json.add("clips", clips);
        return json;
    }

    static String file(String name, Segment segment) {
        return name + "." + segment.name() + ".animation.json";
    }

    /** A clip's length in ticks: its last frame is where the next clip (or the loop's start again) begins. */
    static int length(Segment segment) {
        return Math.max(1, segment.ticks() - 1);
    }
}
