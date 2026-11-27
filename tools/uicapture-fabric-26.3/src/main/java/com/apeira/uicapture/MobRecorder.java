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

    private MobRecorder(String name, LivingEntity mob) {
        this.name = name;
        target.addProperty("type", EntityType.getKey(mob.getType()).toString());
        target.addProperty("uuid", mob.getUUID().toString());
        if (mob.getCustomName() != null) {
            target.addProperty("custom_name", mob.getCustomName().getString());
        }
        target.addProperty("hitbox_width", mob.getBbWidth());
        target.addProperty("hitbox_height", mob.getBbHeight());
        target.addProperty("eye_height", mob.getEyeHeight());
        target.addProperty("invisible", mob.isInvisible());
        target.addProperty("max_health", mob.getMaxHealth());
        target.addProperty("health", mob.getHealth());
        try {
            target.addProperty("movement_speed", mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        } catch (RuntimeException e) {
            // Not every entity has the attribute.
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
        LivingEntity mob = pick(minecraft);
        if (mob == null) {
            Feedback.error(Component.literal("[Mob] Look at a mob (or a part of its model) to record it"));
            return 0;
        }
        String clip = name != null ? name : "mob_" + ArmorStandRecorder.boneName(EntityType.getKey(mob.getType()).getPath());
        double r = radius > 0 ? radius : Math.ceil(mob.getBbWidth() / 2 + 3);
        double above = Math.ceil(mob.getBbHeight() + 4);
        MobRecorder recorder = new MobRecorder(clip, mob);
        if (!ArmorStandRecorder.startFollowing(clip, mob, true, true, r, 2, above, recorder)) {
            return 0;
        }
        active = recorder;
        Feedback.ok(Component.literal("[Mob] Recording " + recorder.target.get("type").getAsString() + " as " + clip
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

    /** The mob under the crosshair, the mob a looked-at model part rides or stands by, else the mob nearest the crosshair line. */
    static @Nullable LivingEntity pick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        Entity hit = minecraft.crosshairPickEntity;
        if (hit != null) {
            if (isMob(hit)) {
                return (LivingEntity) hit;
            }
            if (isMob(hit.getRootVehicle())) {
                return (LivingEntity) hit.getRootVehicle();
            }
            LivingEntity near = nearest(minecraft, hit.position(), 3);
            if (near != null) {
                return near;
            }
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        LivingEntity best = null;
        double bestOff = PICK_SPREAD;
        for (Entity entity : minecraft.level.getEntities(player, player.getBoundingBox().inflate(PICK_REACH), MobRecorder::isMob)) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along <= 0) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off < bestOff) {
                bestOff = off;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    private static @Nullable LivingEntity nearest(Minecraft minecraft, Vec3 at, double reach) {
        LivingEntity best = null;
        double bestDistance = reach * reach;
        for (Entity entity : minecraft.level.getEntities(minecraft.player, new net.minecraft.world.phys.AABB(at, at).inflate(reach), MobRecorder::isMob)) {
            double distance = entity.position().distanceToSqr(at);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = (LivingEntity) entity;
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
        if (!(anchor instanceof LivingEntity mob) || player == null) {
            goneTicks++;
            auto.add(wasDying ? "death" : "gone");
            return;
        }
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
        if (mob.getHealth() != lastHealth) {
            JsonObject change = new JsonObject();
            change.addProperty("tick", tick);
            change.addProperty("health", mob.getHealth());
            change.addProperty("max_health", mob.getMaxHealth());
            health.add(change);
            lastHealth = mob.getHealth();
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
        boolean swingStart = mob.isSwinging() && !lastSwinging;
        lastSwinging = mob.isSwinging();
        if (playerHurt && distance <= HIT_REACH || swingStart) {
            hits.add(tick);
            JsonObject hit = entry(tick, distance);
            hit.addProperty("by", playerHurt && distance <= HIT_REACH ? "player_hurt" : "swing");
            hitLog.add(hit);
        }
        wasDying = mob.isDeadOrDying();
        auto.add(wasDying ? "death" : mob.hurtTime > 0 ? "hurt" : speed > MOVE_SPEED ? "move" : "idle");
        if (tick % 20 == 0) {
            minecraft.gui.hud.setOverlayMessage(Component.literal("[Mob] " + name + " " + label(tick) + "  " + String.format(Locale.ROOT,
                    "%.1fs  %.1f blocks  %.0f/%.0f hp", tick / 20.0, distance, mob.getHealth(), mob.getMaxHealth())), false);
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
        try {
            Files.writeString(folder.resolve("mob.json"), GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("mob.json failed", e);
        }
        Feedback.ok(Component.literal("[Mob] " + recording.ticks() + " ticks, clips " + String.join(", ", labels == null ? List.of() : labels.keySet())
                + String.format(Locale.ROOT, "; range ~%.1f, reach ~%.1f; hitbox %sx%s -> %s", range, reach,
                target.get("hitbox_width").getAsString(), target.get("hitbox_height").getAsString(), folder.getFileName()))
                .withStyle(ChatFormatting.GREEN));
    }
}
