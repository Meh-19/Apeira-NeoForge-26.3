package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.apeira.uicapture.ArmorStandRecorder.Recording;

/**
 * The mob recorder ("entity puller"), for copying a server's custom mobs: a hitbox mob (often an invisible slime or
 * fox) carrying armor stands or display entities as its model. "Mob: Start/Stop Recording" (Home) or
 * {@code /apeiradev mob record [name] [radius]} picks the mob under the crosshair (or the mob a looked-at stand or
 * display rides or stands by) and records everything round it as {@link ArmorStandRecorder} does, locked to the mob
 * ({@link ArmorStandRecorder.Anchor}): its walking and turning are taken out, so a walk cycle plays on the spot, and its
 * own hitbox is the {@code hitbox} bone. Each tick is labelled {@code idle}, {@code move}, {@code attack} (from 10 ticks
 * before to 10 after the mob hits the player or swings), {@code hurt}, {@code death} or what
 * {@code /apeiradev mob label <label>} set ({@code auto} goes back to the automatic labels); each label's longest run is
 * exported as its own clip, cut to its loop when it has one. {@code mob.json} has the mob's hitbox, health, name changes,
 * the label runs and the distances it started coming at the player from (its detection range) and hit them from.
 */
final class MobRecorder implements ArmorStandRecorder.Observer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** How far from the crosshair line a mob may be and still be picked, and how far along it. */
    private static final double PICK_SPREAD = 1.5;
    private static final double PICK_REACH = 32;
    /** Blocks a tick the mob must move to count as moving, and the share of it towards the player that counts as approaching. */
    private static final double MOVE_SPEED = 0.01;
    private static final double APPROACH = 0.7;
    /** Ticks without approaching before a new approach counts as a fresh detection. */
    private static final int CALM_TICKS = 40;
    private static final int ATTACK_BEFORE = 10;
    private static final int ATTACK_AFTER = 10;
    /** Runs shorter than this are folded into the label before them, so idle and move do not flicker. */
    private static final int MIN_RUN = 4;
    /** A player hurt this close to the mob counts as hit by it. */
    private static final double HIT_REACH = 6;
    private static final int MAX_TICKS = 20 * 60 * 5;
    /** Ticks to keep recording once the mob is gone (its visuals may still play a death animation). */
    private static final int GONE_TICKS = 40;

    private static @Nullable MobRecorder active;

    private final String name;
    private final JsonObject target = new JsonObject();
    private final List<String> auto = new ArrayList<>();
    private final List<@Nullable String> manual = new ArrayList<>();
    private final List<Integer> hits = new ArrayList<>();
    private final JsonArray hitLog = new JsonArray();
    private final JsonArray approaches = new JsonArray();
    private final JsonArray aggression = new JsonArray();
    private final JsonArray health = new JsonArray();
    private final JsonArray names = new JsonArray();
    private @Nullable String override;
    private @Nullable Vec3 lastPos;
    private int calm = CALM_TICKS;
    private int lastPlayerHurt;
    private boolean lastSwinging;
    private @Nullable Boolean lastAggressive;
    private float lastHealth = Float.NaN;
    private @Nullable String lastName;
    private int goneTicks;
    private boolean wasDying;
    private @Nullable Map<String, int[]> labels;
    /** The last recorded tick and the followed entity then, for timing and placing sounds. */
    private int lastTick = -1;
    private @Nullable Entity anchor;
    private final JsonArray sounds = new JsonArray();
    /** Sounds this far from the mob, or nearer, are its own. */
    private static final double SOUND_REACH = 12;
    private static final java.util.Set<net.minecraft.sounds.SoundSource> SKIPPED_SOUNDS = java.util.EnumSet.of(
            net.minecraft.sounds.SoundSource.MUSIC, net.minecraft.sounds.SoundSource.RECORDS, net.minecraft.sounds.SoundSource.AMBIENT,
            net.minecraft.sounds.SoundSource.WEATHER, net.minecraft.sounds.SoundSource.PLAYERS, net.minecraft.sounds.SoundSource.UI);

    private MobRecorder(String name, Entity mob, String resolvedBy) {
        this.name = name;
        target.addProperty("type", EntityType.getKey(mob.getType()).toString());
        target.addProperty("resolved_by", resolvedBy);
        target.addProperty("uuid", mob.getUUID().toString());
        if (mob.getCustomName() != null) {
            target.addProperty("custom_name", mob.getCustomName().getString());
        }
        target.addProperty("hitbox_width", mob.getBbWidth());
        target.addProperty("hitbox_height", mob.getBbHeight());
        target.addProperty("eye_height", mob.getEyeHeight());
        target.addProperty("invisible", mob.isInvisible());
        if (mob instanceof LivingEntity living) {
            target.addProperty("max_health", living.getMaxHealth());
            target.addProperty("health", living.getHealth());
            try {
                target.addProperty("movement_speed", living.getAttributeValue(Attributes.MOVEMENT_SPEED));
            } catch (RuntimeException e) {
                // Not every entity has the attribute.
            }
        } else {
            target.addProperty("note", "No living entity was found for it (the server likely hides the real mob, as ModelEngine does), so the "
                    + "recording follows this hitbox entity; health, hurt and death are not known to the client.");
        }
        JsonArray passengers = new JsonArray();
        for (Entity passenger : mob.getPassengers()) {
            passengers.add(EntityType.getKey(passenger.getType()).toString());
        }
        target.add("passengers", passengers);
        if (mob.getVehicle() != null) {
            target.addProperty("vehicle", EntityType.getKey(mob.getVehicle().getType()).toString());
        }
    }

    /**
     * Every sound the client plays ({@link SoundInspector}'s listener): while a mob is recorded, a positioned sound within
     * {@link #SOUND_REACH} blocks of it (not music, ambience, weather, players' or UI sounds) is kept with its recording
     * tick, label, offset from the mob, the variant played (file, volume, pitch, stream, attenuation, length, subtitle)
     * and the event's {@code sounds.json} entries.
     */
    static void onSound(net.minecraft.client.resources.sounds.SoundInstance instance, net.minecraft.client.sounds.WeighedSoundEvents event, float range) {
        MobRecorder recorder = active;
        if (recorder == null || !ArmorStandRecorder.recording() || recorder.anchor == null || instance.isRelative()
                || SKIPPED_SOUNDS.contains(instance.getSource())) {
            return;
        }
        Vec3 at = new Vec3(instance.getX(), instance.getY(), instance.getZ());
        Vec3 mob = recorder.anchor.position();
        if (at.distanceTo(mob) > SOUND_REACH) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        JsonObject entry = new JsonObject();
        // Sounds arrive with the server's packets, before this tick is recorded.
        int tick = recorder.lastTick + 1;
        entry.addProperty("tick", tick);
        entry.addProperty("seconds", tick / 20.0);
        SoundInspector.describe(minecraft, instance, event, range, entry);
        JsonArray offset = new JsonArray();
        offset.add(Math.round((at.x - mob.x) * 1000) / 1000.0);
        offset.add(Math.round((at.y - mob.y) * 1000) / 1000.0);
        offset.add(Math.round((at.z - mob.z) * 1000) / 1000.0);
        entry.add("offset_from_mob", offset);
        recorder.sounds.add(entry);
    }

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("apeiradev").then(ClientCommands.literal("mob")
                .then(ClientCommands.literal("record")
                        .executes(context -> start(null, 0))
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .executes(context -> start(StringArgumentType.getString(context, "name"), 0))
                                .then(ClientCommands.argument("radius", DoubleArgumentType.doubleArg(1, 32))
                                        .executes(context -> start(StringArgumentType.getString(context, "name"),
                                                DoubleArgumentType.getDouble(context, "radius"))))))
                .then(ClientCommands.literal("stop").executes(context -> ArmorStandRecorder.stopRecording()))
                .then(ClientCommands.literal("label")
                        .then(ClientCommands.argument("label", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (String label : new String[] {"auto", "idle", "move", "attack", "hurt", "death", "spawn", "special"}) {
                                        builder.suggest(label);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(MobRecorder::label)))));
    }

    /** The "Mob: Start/Stop Recording" key. */
    static void toggleKey(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (ArmorStandRecorder.recording()) {
            ArmorStandRecorder.stopRecording();
        } else {
            start(null, 0);
        }
    }

    private static int start(@Nullable String name, double radius) {
        Minecraft minecraft = Minecraft.getInstance();
        Picked picked = pick(minecraft);
        if (picked == null) {
            Feedback.error(Component.literal("[Mob] Look at a mob, its hitbox or a part of its model to record it"));
            return 0;
        }
        Entity mob = picked.entity();
        SoundInspector.ensureListener(minecraft);
        String clip = name != null ? name : "mob_" + ArmorStandRecorder.boneName(EntityType.getKey(mob.getType()).getPath());
        double r = radius > 0 ? radius : Math.ceil(mob.getBbWidth() / 2 + 3);
        double above = Math.ceil(mob.getBbHeight() + 4);
        MobRecorder recorder = new MobRecorder(clip, mob, picked.how());
        if (!ArmorStandRecorder.startFollowing(clip, mob, true, true, r, 2, above, recorder)) {
            return 0;
        }
        active = recorder;
        Feedback.ok(Component.literal("[Mob] Recording " + recorder.target.get("type").getAsString() + " (" + picked.how() + ") as " + clip
                + " (locked in place; press again to stop)").withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int label(CommandContext<FabricClientCommandSource> context) {
        MobRecorder recorder = active;
        if (recorder == null || !ArmorStandRecorder.recording()) {
            Feedback.error(Component.literal("[Mob] No mob recording is running"));
            return 0;
        }
        String label = ArmorStandRecorder.boneName(StringArgumentType.getString(context, "label"));
        recorder.override = label.equals("auto") ? null : label;
        Feedback.ok(Component.literal("[Mob] Label: " + label).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    /** The entity a recording follows and how it was found (for {@code mob.json}). */
    record Picked(Entity entity, String how) {
    }

    /**
     * The mob to record. From the entity under the crosshair: itself when it is a mob; the mob it rides (its root
     * vehicle) or that rides it; the nearest mob within a few blocks (servers put {@code minecraft:interaction}
     * hitboxes, armor stands and displays beside a hidden mob); else, when it is a hitbox ({@code minecraft:interaction})
     * or a model part with no mob anywhere near (ModelEngine-style mobs, such as the Wraith, never send the real mob to
     * clients), that hitbox itself, or the interaction nearest the looked-at part. With nothing under the crosshair, the
     * mob, then the interaction, nearest the crosshair line.
     */
    static @Nullable Picked pick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        Entity hit = minecraft.crosshairPickEntity;
        if (hit != null && !(hit instanceof Player)) {
            if (isMob(hit)) {
                return new Picked(hit, "crosshair");
            }
            Entity root = hit.getRootVehicle();
            if (isMob(root)) {
                return new Picked(root, "vehicle_of_" + kind(hit));
            }
            for (Entity rider : hit.getIndirectPassengers()) {
                if (isMob(rider)) {
                    return new Picked(rider, "rider_of_" + kind(hit));
                }
            }
            double reach = Math.max(3, Math.max(hit.getBbWidth(), hit.getBbHeight()) + 1);
            Entity near = nearest(minecraft, hit.position(), reach, MobRecorder::isMob);
            if (near != null) {
                return new Picked(near, "near_" + kind(hit));
            }
            if (hit instanceof Interaction) {
                return new Picked(hit, "interaction");
            }
            Entity box = nearest(minecraft, hit.position(), reach, entity -> entity instanceof Interaction);
            if (box != null) {
                return new Picked(box, "interaction_near_" + kind(hit));
            }
        }
        Entity mob = alongLook(minecraft, player, MobRecorder::isMob);
        if (mob != null) {
            return new Picked(mob, "look_line");
        }
        Entity box = alongLook(minecraft, player, entity -> entity instanceof Interaction);
        return box == null ? null : new Picked(box, "interaction_look_line");
    }

    private static String kind(Entity entity) {
        return entity instanceof Interaction ? "interaction" : AreaEntities.kind(entity).equals("entity")
                ? ArmorStandRecorder.boneName(EntityType.getKey(entity.getType()).getPath()) : AreaEntities.kind(entity);
    }

    /** The matching entity nearest the crosshair line, within {@link #PICK_SPREAD} of it and {@link #PICK_REACH} along it. */
    private static @Nullable Entity alongLook(Minecraft minecraft, Player player, java.util.function.Predicate<Entity> filter) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Entity best = null;
        double bestOff = PICK_SPREAD;
        for (Entity entity : minecraft.level.getEntities(player, player.getBoundingBox().inflate(PICK_REACH), filter)) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along <= 0) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off < bestOff) {
                bestOff = off;
                best = entity;
            }
        }
        return best;
    }

    private static @Nullable Entity nearest(Minecraft minecraft, Vec3 at, double reach, java.util.function.Predicate<Entity> filter) {
        Entity best = null;
        double bestDistance = reach * reach;
        for (Entity entity : minecraft.level.getEntities(minecraft.player, new net.minecraft.world.phys.AABB(at, at).inflate(reach), filter)) {
            double distance = entity.position().distanceToSqr(at);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isMob(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof ArmorStand) && !(entity instanceof Player);
    }

    @Override
    public void tick(Minecraft minecraft, int tick, @Nullable Entity anchor) {
        Player player = minecraft.player;
        manual.add(override);
        lastTick = tick;
        if (anchor == null || player == null) {
            goneTicks++;
            auto.add(wasDying ? "death" : "gone");
            return;
        }
        Entity mob = anchor;
        LivingEntity living = mob instanceof LivingEntity alive ? alive : null;
        this.anchor = mob;
        Vec3 pos = mob.position();
        Vec3 step = lastPos == null ? Vec3.ZERO : pos.subtract(lastPos);
        lastPos = pos;
        double speed = Math.sqrt(step.x * step.x + step.z * step.z);
        double distance = player.position().distanceTo(pos);
        Vec3 toPlayer = player.position().subtract(pos);
        double flat = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
        boolean approaching = speed > MOVE_SPEED * 2 && flat > 0
                && (step.x * toPlayer.x + step.z * toPlayer.z) / (speed * flat) > APPROACH;
        if (approaching) {
            if (calm >= CALM_TICKS) {
                approaches.add(entry(tick, distance));
            }
            calm = 0;
        } else {
            calm++;
        }
        if (mob instanceof Mob withAi && (lastAggressive == null || lastAggressive != withAi.isAggressive())) {
            JsonObject change = entry(tick, distance);
            change.addProperty("aggressive", withAi.isAggressive());
            aggression.add(change);
            lastAggressive = withAi.isAggressive();
        }
        if (living != null && living.getHealth() != lastHealth) {
            JsonObject change = new JsonObject();
            change.addProperty("tick", tick);
            change.addProperty("health", living.getHealth());
            change.addProperty("max_health", living.getMaxHealth());
            health.add(change);
            lastHealth = living.getHealth();
        }
        String custom = mob.getCustomName() == null ? null : mob.getCustomName().getString();
        if (custom != null && !custom.equals(lastName)) {
            // Plugins often write the health into the name.
            JsonObject change = new JsonObject();
            change.addProperty("tick", tick);
            change.addProperty("name", custom);
            names.add(change);
        }
        lastName = custom;
        // The player was hurt this tick (hurtTime jumps up) while close: count it as the mob's hit.
        boolean playerHurt = player.hurtTime > lastPlayerHurt;
        lastPlayerHurt = player.hurtTime;
        boolean swinging = living != null && living.isSwinging();
        boolean swingStart = swinging && !lastSwinging;
        lastSwinging = swinging;
        if (playerHurt && distance <= HIT_REACH || swingStart) {
            hits.add(tick);
            JsonObject hit = entry(tick, distance);
            hit.addProperty("by", playerHurt && distance <= HIT_REACH ? "player_hurt" : "swing");
            hitLog.add(hit);
        }
        wasDying = living != null && living.isDeadOrDying();
        auto.add(wasDying ? "death" : living != null && living.hurtTime > 0 ? "hurt" : speed > MOVE_SPEED ? "move" : "idle");
        if (tick % 20 == 0) {
            minecraft.gui.hud.setOverlayMessage(Component.literal("[Mob] " + name + " " + label(tick) + "  " + String.format(Locale.ROOT,
                    "%.1fs  %.1f blocks  %d sounds", tick / 20.0, distance, sounds.size())
                    + (living == null ? "" : String.format(Locale.ROOT, "  %.0f/%.0f hp", living.getHealth(), living.getMaxHealth()))), false);
        }
    }

    private String label(int tick) {
        return manual.get(tick) != null ? manual.get(tick) : auto.get(tick);
    }

    private static JsonObject entry(int tick, double distance) {
        JsonObject json = new JsonObject();
        json.addProperty("tick", tick);
        json.addProperty("distance", Math.round(distance * 100) / 100.0);
        return json;
    }

    @Override
    public boolean shouldStop(int ticks) {
        return ticks >= MAX_TICKS || goneTicks >= GONE_TICKS;
    }

    /** The final label of each tick: manual labels first, then death, hurt, attack windows, move and idle, short runs folded away. */
    private List<String> finalLabels() {
        int n = auto.size();
        String[] out = new String[n];
        for (int t = 0; t < n; t++) {
            out[t] = auto.get(t);
        }
        for (int hit : hits) {
            for (int t = Math.max(0, hit - ATTACK_BEFORE); t <= Math.min(n - 1, hit + ATTACK_AFTER); t++) {
                if (out[t].equals("idle") || out[t].equals("move")) {
                    out[t] = "attack";
                }
            }
        }
        // Fold runs too short to be a clip into the run before them.
        for (int t = 1; t < n; ) {
            int end = t;
            while (end < n && out[end].equals(out[t])) {
                end++;
            }
            if (end - t < MIN_RUN && !out[t].equals(out[t - 1]) && !out[t].equals("attack") && !out[t].equals("hurt")) {
                for (int u = t; u < end; u++) {
                    out[u] = out[t - 1];
                }
            }
            t = end;
        }
        List<String> labels = new ArrayList<>(n);
        for (int t = 0; t < n; t++) {
            labels.add(manual.get(t) != null ? manual.get(t) : out[t]);
        }
        return labels;
    }

    /** Each label's runs as {first tick, tick count}. */
    private static Map<String, List<int[]>> runs(List<String> labels) {
        Map<String, List<int[]>> runs = new LinkedHashMap<>();
        for (int t = 0; t < labels.size(); ) {
            int end = t;
            while (end < labels.size() && labels.get(end).equals(labels.get(t))) {
                end++;
            }
            runs.computeIfAbsent(labels.get(t), key -> new ArrayList<>()).add(new int[] {t, end - t});
            t = end;
        }
        return runs;
    }

    @Override
    public Map<String, int[]> labels() {
        Map<String, int[]> longest = new LinkedHashMap<>();
        for (Map.Entry<String, List<int[]>> label : runs(finalLabels()).entrySet()) {
            if (label.getKey().equals("gone")) {
                continue;
            }
            int[] best = label.getValue().stream().max(java.util.Comparator.comparingInt(run -> run[1])).orElseThrow();
            // One tick more, so a clip ends where the next label begins.
            longest.put(label.getKey(), new int[] {best[0], best[1] + 1});
        }
        labels = longest;
        return longest;
    }

    @Override
    public void done(Recording recording, Path folder) {
        active = null;
        JsonObject json = new JsonObject();
        json.add("mob", target);
        JsonArray runList = new JsonArray();
        for (Map.Entry<String, List<int[]>> label : runs(finalLabels()).entrySet()) {
            for (int[] run : label.getValue()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("label", label.getKey());
                entry.addProperty("from_tick", run[0]);
                entry.addProperty("ticks", run[1]);
                runList.add(entry);
            }
        }
        json.add("label_runs", runList);
        JsonObject detection = new JsonObject();
        double range = 0;
        for (var approach : approaches) {
            range = Math.max(range, approach.getAsJsonObject().get("distance").getAsDouble());
        }
        double reach = 0;
        for (var hit : hitLog) {
            if (hit.getAsJsonObject().get("by").getAsString().equals("player_hurt")) {
                reach = Math.max(reach, hit.getAsJsonObject().get("distance").getAsDouble());
            }
        }
        detection.addProperty("estimated_detection_range", range);
        detection.addProperty("estimated_attack_reach", reach);
        detection.addProperty("note", "Estimates from what the client sees: the distance the mob began walking at the player from after "
                + CALM_TICKS + " calm ticks, and the distance it hurt the player from. Walk in slowly from far away for a good range.");
        detection.add("approach_starts", approaches);
        detection.add("aggressive_changes", aggression);
        detection.add("hits", hitLog);
        json.add("detection", detection);
        json.add("health", health);
        json.add("name_changes", names);
        List<String> finalLabels = finalLabels();
        JsonObject events = new JsonObject();
        for (var element : sounds) {
            JsonObject sound = element.getAsJsonObject();
            int tick = sound.get("tick").getAsInt();
            if (tick < finalLabels.size()) {
                sound.addProperty("label", finalLabels.get(tick));
            }
            // Where it falls in each exported label clip (the clip's own tick 0 is the run's first tick).
            JsonObject clips = new JsonObject();
            if (labels != null) {
                for (Map.Entry<String, int[]> label : labels.entrySet()) {
                    int from = label.getValue()[0];
                    if (tick >= from && tick < from + label.getValue()[1]) {
                        clips.addProperty(label.getKey(), tick - from);
                    }
                }
            }
            sound.add("clip_ticks", clips);
            String id = sound.get("sound").getAsString();
            JsonObject summary = events.has(id) ? events.getAsJsonObject(id) : null;
            if (summary == null) {
                summary = new JsonObject();
                summary.addProperty("count", 0);
                summary.add("sounds_json", SoundInspector.definitions(Minecraft.getInstance(), net.minecraft.resources.Identifier.parse(id)));
                events.add(id, summary);
            }
            summary.addProperty("count", summary.get("count").getAsInt() + 1);
        }
        JsonObject soundFile = new JsonObject();
        soundFile.add("events", events);
        soundFile.add("sounds", sounds);
        JsonObject soundSummary = new JsonObject();
        soundSummary.addProperty("file", "mob_sounds.json");
        soundSummary.addProperty("count", sounds.size());
        JsonArray ids = new JsonArray();
        events.keySet().forEach(ids::add);
        soundSummary.add("events", ids);
        json.add("sounds", soundSummary);
        try {
            Files.writeString(folder.resolve("mob_sounds.json"), GSON.toJson(soundFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("mob_sounds.json failed", e);
        }
        try {
            Files.writeString(folder.resolve("mob.json"), GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("mob.json failed", e);
        }
        Feedback.ok(Component.literal("[Mob] " + recording.ticks() + " ticks, " + sounds.size() + " sounds, clips " + String.join(", ", labels == null ? List.of() : labels.keySet())
                + String.format(Locale.ROOT, "; range ~%.1f, reach ~%.1f; hitbox %sx%s -> %s", range, reach,
                target.get("hitbox_width").getAsString(), target.get("hitbox_height").getAsString(), folder.getFileName()))
                .withStyle(ChatFormatting.GREEN));
    }
}
