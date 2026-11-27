package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.apeira.uicapture.ArmorStandRecorder.PropSnapshot;
import com.apeira.uicapture.ArmorStandRecorder.PropTrack;
import com.apeira.uicapture.ArmorStandRecorder.Recording;
import com.apeira.uicapture.ArmorStandRecorder.Snapshot;
import com.apeira.uicapture.ArmorStandRecorder.Track;

/**
 * The balloon logger, the backswag logger's counterpart for balloon cosmetics. "Balloon: Log Hovered / Record" (End)
 * inside a screen logs the hovered balloon (its item, rarity line, tooltip and colours, as the backswag hover key) to
 * {@link #LOG}; with no screen open it starts a recording locked to the player (every armor stand and display entity
 * within {@link #RADIUS} blocks, {@link ArmorStandRecorder.Anchor} without the yaw lock, so the balloon's own sway stays),
 * checked for a loop every {@link #CHECK_EVERY} ticks and stopped by itself once one has been seen twice in full (or
 * after {@link #MAX_TICKS}, or on a second press). The export is the recorder's usual one; the balloon's height is the
 * entity showing the hovered item (else the highest one with an item), linked to the hovered balloon in
 * {@link #OFFSETS} by its name in snake case, with its offset from the player, how far it bobs and the loop's length.
 */
final class BalloonRecorder implements ArmorStandRecorder.Observer {
    static final String LOG = "logs/apeira_balloon_log.jsonl";
    static final String OFFSETS = "logs/apeira_balloon_offsets.json";
    static final double RADIUS = 5;
    static final double ABOVE = 10;
    static final int CHECK_EVERY = 40;
    static final int MIN_TICKS = 60;
    static final int MAX_TICKS = 20 * 60;
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** The last hovered balloon, which the next recording is linked to. */
    private static @Nullable JsonObject armed;

    private final @Nullable JsonObject balloon;
    private boolean looped;

    private BalloonRecorder(@Nullable JsonObject balloon) {
        this.balloon = balloon;
    }

    /** The key inside a screen: logs the hovered balloon. */
    static void logHovered(Minecraft minecraft, Screen screen) {
        JsonObject json = BackswagLogger.hovered(minecraft, screen, "hovered_balloon");
        if (json == null) {
            return;
        }
        BackswagLogger.append(LOG, json);
        armed = json;
        BackswagLogger.print("[Balloon]", json);
        Feedback.ok(Component.literal("  Equip it, close the menu, stand still and press the key again to record it").withStyle(ChatFormatting.GRAY));
    }

    /** The key with no screen: starts a balloon recording, or stops the running one. */
    static void toggleKey(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (ArmorStandRecorder.recording()) {
            ArmorStandRecorder.stopRecording();
            return;
        }
        String name = armed != null ? "balloon_" + key(armed) : "balloon";
        if (ArmorStandRecorder.startFollowing(name, minecraft.player, false, false, RADIUS, 1, ABOVE, new BalloonRecorder(armed))) {
            Feedback.ok(Component.literal("[Balloon] Recording " + (armed != null ? armed.get("name").getAsString() : "(no hovered balloon)")
                    + ": stand still, it stops when the animation loops").withStyle(ChatFormatting.GOLD));
        }
    }

    private static String key(JsonObject cosmetic) {
        String key = cosmetic.get("name").getAsString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return key.isEmpty() ? "balloon" : key;
    }

    @Override
    public void tick(Minecraft minecraft, int tick, @Nullable Entity anchor) {
        if (tick % 20 == 0) {
            minecraft.gui.hud.setOverlayMessage(Component.literal("[Balloon] recording " + tick / 20 + "s, waiting for a loop"), false);
        }
    }

    @Override
    public boolean shouldStop(int ticks) {
        if (ticks >= MAX_TICKS) {
            return true;
        }
        if (ticks < MIN_TICKS || ticks % CHECK_EVERY != 0) {
            return false;
        }
        Recording so = ArmorStandRecorder.current();
        if (so == null) {
            return false;
        }
        AnimationSegments.Result result = AnimationSegments.find(so);
        looped = result.repeats() >= 2;
        return looped;
    }

    @Override
    public Map<String, int[]> labels() {
        return Map.of();
    }

    @Override
    public void done(Recording recording, Path folder) {
        Vec3 origin = new Vec3((recording.min().getX() + recording.max().getX() + 1) / 2.0, recording.min().getY(),
                (recording.min().getZ() + recording.max().getZ() + 1) / 2.0);
        String match = balloon == null ? null : balloon.get("id").getAsString() + (balloon.has("item_model") ? balloon.get("item_model").getAsString() : "");
        // The entity showing the balloon: one whose item matches the hovered one, else the highest with an item.
        String bone = null, how = null, item = null;
        java.util.Map<Integer, Vec3> frames = null;
        double bestY = Double.NEGATIVE_INFINITY;
        for (PropTrack track : recording.props()) {
            if (track.frames.isEmpty()) {
                continue;
            }
            PropSnapshot first = track.frames.values().iterator().next();
            if (!(first.state().get("item") instanceof JsonObject shown)) {
                continue;
            }
            String key = shown.get("id").getAsString() + (shown.has("item_model") ? shown.get("item_model").getAsString() : "");
            boolean matches = key.equals(match);
            if (matches && !"match".equals(how) || !"match".equals(how) && first.pos().y > bestY) {
                bone = track.bone;
                how = matches ? "match" : "highest";
                item = shown.toString();
                bestY = first.pos().y;
                frames = new java.util.LinkedHashMap<>();
                for (Map.Entry<Integer, PropSnapshot> f : track.frames.entrySet()) {
                    frames.put(f.getKey(), f.getValue().pos());
                }
            }
        }
        for (Track track : recording.tracks()) {
            if (track.frames.isEmpty()) {
                continue;
            }
            Snapshot first = track.frames.values().iterator().next();
            JsonObject head = first.items()[0];
            if (head == null || first.player()) {
                continue;
            }
            String key = head.get("id").getAsString() + (head.has("item_model") ? head.get("item_model").getAsString() : "");
            boolean matches = key.equals(match);
            if (matches && !"match".equals(how) || !"match".equals(how) && first.pos().y > bestY) {
                bone = track.bone;
                how = matches ? "match" : "highest";
                item = head.toString();
                bestY = first.pos().y;
                frames = new java.util.LinkedHashMap<>();
                for (Map.Entry<Integer, Snapshot> f : track.frames.entrySet()) {
                    frames.put(f.getKey(), f.getValue().pos());
                }
            }
        }
        if (frames == null) {
            Feedback.error(Component.literal("[Balloon] No armor stand or display with an item near you; nothing linked"));
            return;
        }
        Vec3 start = frames.values().iterator().next();
        double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY;
        for (Vec3 pos : frames.values()) {
            low = Math.min(low, pos.y - origin.y);
            high = Math.max(high, pos.y - origin.y);
        }
        AnimationSegments.Result loop = AnimationSegments.find(recording);
        JsonObject entry = new JsonObject();
        entry.addProperty("name", balloon == null ? "(none hovered)" : balloon.get("name").getAsString());
        entry.addProperty("linked_by", how);
        entry.addProperty("time", LocalDateTime.now().toString());
        entry.addProperty("bone", bone);
        entry.addProperty("dy", start.y - origin.y);
        entry.addProperty("dx", start.x - origin.x);
        entry.addProperty("dz", start.z - origin.z);
        entry.addProperty("dy_min", low);
        entry.addProperty("dy_max", high);
        entry.addProperty("period_ticks", loop.period());
        entry.addProperty("looped", looped);
        entry.addProperty("export", folder.getFileName().toString());
        entry.addProperty("worn_item", item);
        if (balloon != null) {
            entry.add("cosmetic", balloon);
        }
        String key = balloon == null ? "unlinked_" + folder.getFileName() : key(balloon);
        Path file = FabricLoader.getInstance().getGameDir().resolve(OFFSETS);
        try {
            JsonObject table = Files.exists(file) ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            table.add(key, entry);
            Files.createDirectories(file.getParent());
            Files.writeString(file, PRETTY.toJson(table), StandardCharsets.UTF_8);
            Files.writeString(folder.resolve("balloon.json"), PRETTY.toJson(entry), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            UiCaptureClient.LOGGER.error("Balloon offset table failed", e);
            Feedback.error(Component.literal("Balloon offset table failed: " + e.getMessage()));
            return;
        }
        armed = null;
        Feedback.ok(Component.literal(String.format(Locale.ROOT, "[Balloon] %s -> dy %.3f (bobs %.3f-%.3f), loop %s ticks, by %s -> %s",
                key, start.y - origin.y, low, high, loop.period() == 0 ? "none" : loop.period(), how, OFFSETS)).withStyle(ChatFormatting.GREEN));
    }
}
