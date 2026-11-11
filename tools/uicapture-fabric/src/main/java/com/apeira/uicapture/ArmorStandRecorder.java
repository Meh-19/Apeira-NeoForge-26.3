package com.apeira.uicapture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import com.apeira.uicapture.mixin.InteractionAccessor;

/**
 * Records the armor stands inside a recording area, tick by tick, for copying a server's multi-part armor stand
 * animations: {@code /apeiradev area pos1|pos2 [x y z]} sets the area's corners (the looked-at block, else the block the
 * player stands in), {@code /apeiradev record start [name]} and {@code stop} record and export ({@link ArmorStandExporter}).
 * Only armor stands (marker ones included; the {@code minecraft:marker} entity is never sent to clients) whose position is
 * inside the area are recorded. Each tick keeps every stand's position, yaw, poses, flags and equipment, and notes when a
 * stand appears, disappears, turns invisible or visible, or changes an item (its id, item model, custom model data or dye).
 * {@code minecraft:interaction} entities inside the area (the invisible hitboxes servers put on armor stand models) are
 * recorded too: position, yaw, hitbox width and height and the response flag, with events when they appear, disappear or
 * change, when the player's crosshair starts or stops targeting one, and when the player attacks or uses one. The server
 * keeps who last attacked or used an interaction to itself, so what it answers with shows only as the recording's other
 * events (swaps, spawns) in the ticks after a click.
 */
public final class ArmorStandRecorder {
    /** The slots an armor stand shows, in the order they are exported. */
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    /** Ticks between redraws of the area's outline. */
    private static final int OUTLINE_INTERVAL = 10;
    /** At most this many outline particles a redraw; larger areas get sparser edges. */
    private static final int OUTLINE_PARTICLES = 600;
    private static final DustParticleOptions OUTLINE = new DustParticleOptions(0x47B0D7, 1.0F);
    private static final DustParticleOptions CORNER_1 = new DustParticleOptions(0xF17F61, 1.5F);
    private static final DustParticleOptions CORNER_2 = new DustParticleOptions(0xABE061, 1.5F);

    /** One stand's state in one tick. Item entries are the slot's item as JSON, or {@code null} when it is empty. */
    record Snapshot(Vec3 pos, float yaw, boolean invisible, boolean small, boolean marker, boolean showArms, boolean basePlate,
                    Rotations head, Rotations body, Rotations leftArm, Rotations rightArm, Rotations leftLeg, Rotations rightLeg,
                    @Nullable JsonObject[] items) {
    }

    /** A stand seen during the recording: its bone name, the tick it was first seen and a snapshot per tick it was in the area. */
    static final class Track {
        final UUID uuid;
        final String bone;
        final int entityId;
        final @Nullable String name;
        final int firstTick;
        final Map<Integer, Snapshot> frames = new LinkedHashMap<>();

        Track(UUID uuid, String bone, int entityId, @Nullable String name, int firstTick) {
            this.uuid = uuid;
            this.bone = bone;
            this.entityId = entityId;
            this.name = name;
            this.firstTick = firstTick;
        }
    }

    /** One interaction entity's state in one tick: position, yaw, hitbox width and height (blocks) and whether it responds to clicks. */
    record HitboxSnapshot(Vec3 pos, float yaw, float width, float height, boolean response) {
    }

    /** An interaction entity seen during the recording, as {@link Track} is for a stand. */
    static final class HitboxTrack {
        final UUID uuid;
        final String bone;
        final int entityId;
        final int firstTick;
        final Map<Integer, HitboxSnapshot> frames = new LinkedHashMap<>();

        HitboxTrack(UUID uuid, String bone, int entityId, int firstTick) {
            this.uuid = uuid;
            this.bone = bone;
            this.entityId = entityId;
            this.firstTick = firstTick;
        }
    }

    /** A prop's state in one tick: position, yaw, pitch, what picks its look ({@link AreaEntities#state}) and, for display entities, its transformation. */
    record PropSnapshot(Vec3 pos, float yaw, float pitch, JsonObject state, AreaEntities.@Nullable Transform transform) {
    }

    /** Any other entity seen in the area (display entities, item frames, dropped items, mobs), as {@link Track} is for a stand. */
    static final class PropTrack {
        final UUID uuid;
        final String bone;
        final String kind;
        final String type;
        final int entityId;
        final int firstTick;
        final Map<Integer, PropSnapshot> frames = new LinkedHashMap<>();

        PropTrack(UUID uuid, String bone, String kind, String type, int entityId, int firstTick) {
            this.uuid = uuid;
            this.bone = bone;
            this.kind = kind;
            this.type = type;
            this.entityId = entityId;
            this.firstTick = firstTick;
        }
    }

    /** One recorded event: its tick, type ({@code spawn}, {@code enter}, {@code present}, {@code despawn}, {@code exit}, {@code invisible}, {@code visible}, {@code swap};
     * for interactions also {@code resize}, {@code response}, {@code target_start}, {@code target_end}, {@code attack}, {@code use}), stand or interaction bone and details. */
    record Event(int tick, String type, String bone, JsonObject details) {
    }

    /** A finished recording, handed to the exporter. */
    record Recording(String name, BlockPos min, BlockPos max, int ticks, List<Track> tracks, List<HitboxTrack> hitboxes, List<PropTrack> props, List<Event> events) {
    }

    private static @Nullable BlockPos pos1;
    private static @Nullable BlockPos pos2;
    private static boolean showOutline = true;
    private static @Nullable Session session;

    /**
     * A bone name from an item's model: the last part of the first model its definition resolves to ({@code badger_head}),
     * else the item's path and custom model data ({@code paper_cmd2319}).
     */
    static String modelName(Session session, JsonObject item, String context) {
        try {
            List<ItemModels.Model> models = session.models.resolve(item, context).models();
            if (!models.isEmpty()) {
                return boneName(models.getFirst().id().getPath().replaceAll(".*/", ""));
            }
        } catch (RuntimeException e) {
            UiCaptureClient.LOGGER.debug("Could not resolve {}", item, e);
        }
        return boneName(itemName(item));
    }

    /** An item as a bone name: its path, and its first custom model data float ({@code paper_cmd2319}). */
    static String itemName(JsonObject item) {
        String name = Identifier.parse(item.get("id").getAsString()).getPath();
        if (item.has("custom_model_data") && item.getAsJsonObject("custom_model_data").has("floats")
                && !item.getAsJsonObject("custom_model_data").getAsJsonArray("floats").isEmpty()) {
            name += "_cmd" + Math.round(item.getAsJsonObject("custom_model_data").getAsJsonArray("floats").get(0).getAsDouble());
        }
        return name;
    }

    static String boneName(String text) {
        String name = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        return name.isEmpty() ? "bone" : name;
    }

    private static final class Session {
        final String name;
        final BlockPos min;
        final BlockPos max;
        final AABB box;
        final Map<UUID, Track> tracks = new LinkedHashMap<>();
        final Map<UUID, HitboxTrack> hitboxes = new LinkedHashMap<>();
        /** The interactions in the area last tick, and the one the crosshair is on. */
        Set<UUID> hitboxesInside = new HashSet<>();
        final Map<UUID, PropTrack> props = new LinkedHashMap<>();
        /** The props in the area last tick, and how many of each kind have been seen, for their bone names. */
        Set<UUID> propsInside = new HashSet<>();
        final Map<String, Integer> kindCounts = new HashMap<>();
        /** Every bone name given out, and the models items resolve to, for bones named after their model. */
        final Set<String> names = new HashSet<>();
        final ItemModels models = new ItemModels(Minecraft.getInstance().getResourceManager(),
                new CapturedAssets(Minecraft.getInstance().getResourceManager(), null));
        @Nullable UUID targeted;
        final List<Event> events = new ArrayList<>();
        /** The stands in the area last tick. */
        Set<UUID> inside = new HashSet<>();
        int tick;

        /** {@code base}, or {@code base_2}, {@code base_3} ... when it is taken. */
        String name(String base) {
            String name = base;
            for (int n = 2; !names.add(name); n++) {
                name = base + "_" + n;
            }
            return name;
        }

        Session(String name, BlockPos min, BlockPos max) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.box = AABB.encapsulatingFullBlocks(min, max);
        }
    }

    private ArmorStandRecorder() {
    }

    /** The recording area's lowest and highest corners, or null while a corner is not set; {@link DatapackExporter} saves it as a structure. */
    static BlockPos @Nullable [] area() {
        return pos1 == null || pos2 == null ? null : new BlockPos[] {BlockPos.min(pos1, pos2), BlockPos.max(pos1, pos2)};
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("apeiradev")
                .then(ClientCommandManager.literal("area")
                        .executes(ArmorStandRecorder::info)
                        .then(corner("pos1", 1))
                        .then(corner("pos2", 2))
                        .then(ClientCommandManager.literal("show").executes(context -> outline(context, true)))
                        .then(ClientCommandManager.literal("hide").executes(context -> outline(context, false)))
                        .then(ClientCommandManager.literal("clear").executes(ArmorStandRecorder::clear)))
                .then(ClientCommandManager.literal("dumparea").executes(ArmorStandRecorder::dump))
                .then(ClientCommandManager.literal("record")
                        .then(ClientCommandManager.literal("start")
                                .executes(context -> start(context, "armor_stands"))
                                .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                        .executes(context -> start(context, StringArgumentType.getString(context, "name")))))
                        .then(ClientCommandManager.literal("stop").executes(ArmorStandRecorder::stop))
                        .then(ClientCommandManager.literal("cancel").executes(ArmorStandRecorder::cancel))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> corner(String literal, int corner) {
        return ClientCommandManager.literal(literal)
                .executes(context -> setCorner(context, corner, targetBlock(Minecraft.getInstance())))
                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                        .executes(context -> setCorner(context, corner, new BlockPos(
                                                IntegerArgumentType.getInteger(context, "x"),
                                                IntegerArgumentType.getInteger(context, "y"),
                                                IntegerArgumentType.getInteger(context, "z")))))));
    }

    /** The block under the crosshair, else the block the player stands in. */
    private static BlockPos targetBlock(Minecraft minecraft) {
        if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }
        return minecraft.player.blockPosition();
    }

    private static int setCorner(CommandContext<FabricClientCommandSource> context, int corner, BlockPos pos) {
        if (session != null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.busy"));
            return 0;
        }
        if (corner == 1) {
            pos1 = pos;
        } else {
            pos2 = pos;
        }
        context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.corner", corner, pos.toShortString()));
        return info(context);
    }

    private static int info(CommandContext<FabricClientCommandSource> context) {
        if (pos1 == null || pos2 == null) {
            context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.no_area",
                    pos1 == null ? "-" : pos1.toShortString(), pos2 == null ? "-" : pos2.toShortString()));
            return 0;
        }
        BlockPos min = BlockPos.min(pos1, pos2);
        BlockPos max = BlockPos.max(pos1, pos2);
        AABB box = AABB.encapsulatingFullBlocks(min, max);
        int stands = Minecraft.getInstance().level.getEntitiesOfClass(ArmorStand.class, box).size();
        int hitboxes = Minecraft.getInstance().level.getEntitiesOfClass(Interaction.class, box).size();
        context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.area", min.toShortString(), max.toShortString(),
                max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1, stands, hitboxes));
        return 1;
    }

    private static int dump(CommandContext<FabricClientCommandSource> context) {
        if (pos1 == null || pos2 == null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.no_area",
                    pos1 == null ? "-" : pos1.toShortString(), pos2 == null ? "-" : pos2.toShortString()));
            return 0;
        }
        AreaEntities.dump(Minecraft.getInstance(), AABB.encapsulatingFullBlocks(BlockPos.min(pos1, pos2), BlockPos.max(pos1, pos2)), line -> {
            context.getSource().sendFeedback(line);
            UiCaptureClient.LOGGER.info("[dumparea] {}", line.getString());
        });
        return 1;
    }

    private static int outline(CommandContext<FabricClientCommandSource> context, boolean show) {
        showOutline = show;
        context.getSource().sendFeedback(Component.translatable(show ? "message.apeira.armor_stands.outline_on" : "message.apeira.armor_stands.outline_off"));
        return 1;
    }

    private static int clear(CommandContext<FabricClientCommandSource> context) {
        if (session != null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.busy"));
            return 0;
        }
        pos1 = null;
        pos2 = null;
        context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.cleared"));
        return 1;
    }

    private static int start(CommandContext<FabricClientCommandSource> context, String name) {
        if (session != null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.busy"));
            return 0;
        }
        if (pos1 == null || pos2 == null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.no_area",
                    pos1 == null ? "-" : pos1.toShortString(), pos2 == null ? "-" : pos2.toShortString()));
            return 0;
        }
        session = new Session(name, BlockPos.min(pos1, pos2), BlockPos.max(pos1, pos2));
        context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.started", name));
        return 1;
    }

    private static int stop(CommandContext<FabricClientCommandSource> context) {
        Session done = session;
        if (done == null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.not_recording"));
            return 0;
        }
        session = null;
        Recording recording = new Recording(done.name, done.min, done.max, done.tick, new ArrayList<>(done.tracks.values()),
                new ArrayList<>(done.hitboxes.values()), new ArrayList<>(done.props.values()), done.events);
        try {
            Path folder = ArmorStandExporter.write(recording, line -> {
                context.getSource().sendFeedback(Component.literal(line).withStyle(ChatFormatting.GRAY));
                UiCaptureClient.LOGGER.info("[display diagnostics] {}", line);
            });
            UiCaptureClient.LOGGER.info("Armor stand recording written to {}", folder);
            context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.done",
                    done.tracks.size(), done.hitboxes.size(), done.props.size(), done.tick, done.events.size(), folder.getFileName().toString()));
            return 1;
        } catch (Exception e) {
            UiCaptureClient.LOGGER.error("Armor stand export failed", e);
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.failed", String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static int cancel(CommandContext<FabricClientCommandSource> context) {
        if (session == null) {
            context.getSource().sendError(Component.translatable("message.apeira.armor_stands.not_recording"));
            return 0;
        }
        session = null;
        context.getSource().sendFeedback(Component.translatable("message.apeira.armor_stands.cancelled"));
        return 1;
    }

    /** Called every client tick: draws the area and records a frame. */
    public static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            if (session != null) {
                // Left the world: the stands it recorded are gone, so the recording is too.
                session = null;
                UiCaptureClient.LOGGER.warn("Armor stand recording dropped: the world was left");
            }
            return;
        }
        if (showOutline && pos1 != null && pos2 != null && level.getGameTime() % OUTLINE_INTERVAL == 0) {
            drawOutline(level);
        }
        Session current = session;
        if (current != null && !minecraft.isPaused()) {
            record(level, current);
            recordHitboxes(minecraft, level, current);
            recordProps(minecraft, level, current);
            current.tick++;
            if (current.tick % 10 == 0) {
                minecraft.player.displayClientMessage(Component.translatable("message.apeira.armor_stands.recording",
                        current.name, current.inside.size(), current.hitboxesInside.size(), current.propsInside.size(), String.format("%.1f", current.tick / 20.0)), true);
            }
        }
    }

    private static void record(ClientLevel level, Session session) {
        int tick = session.tick;
        Set<UUID> now = new HashSet<>();
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, session.box, e -> session.box.contains(e.position()))) {
            UUID uuid = stand.getUUID();
            now.add(uuid);
            Snapshot snapshot = snapshot(stand);
            Track track = session.tracks.get(uuid);
            if (track == null) {
                Component custom = stand.getCustomName();
                String model = null;
                for (JsonObject item : snapshot.items()) {
                    model = model != null || item == null ? model : modelName(session, item, "head");
                }
                track = new Track(uuid, session.name(model == null ? "stand_" + session.tracks.size() : model + "_stand"), stand.getId(),
                        custom == null ? null : custom.getString(), tick);
                session.tracks.put(uuid, track);
            }
            Snapshot last = track.frames.get(tick - 1);
            if (last == null) {
                // A client adds an entity with tickCount 0, so a young one was spawned now, an older one moved or was loaded in.
                String type = tick == 0 ? "present" : stand.tickCount <= 2 ? "spawn" : "enter";
                session.events.add(new Event(tick, type, track.bone, describe(snapshot)));
            } else {
                if (last.invisible() != snapshot.invisible()) {
                    session.events.add(new Event(tick, snapshot.invisible() ? "invisible" : "visible", track.bone, new JsonObject()));
                }
                for (int i = 0; i < SLOTS.length; i++) {
                    if (!same(last.items()[i], snapshot.items()[i])) {
                        JsonObject details = new JsonObject();
                        details.addProperty("slot", SLOTS[i].getName());
                        details.add("from", last.items()[i]);
                        details.add("to", snapshot.items()[i]);
                        session.events.add(new Event(tick, "swap", track.bone, details));
                    }
                }
            }
            track.frames.put(tick, snapshot);
        }
        for (UUID uuid : session.inside) {
            if (!now.contains(uuid)) {
                Track track = session.tracks.get(uuid);
                Entity entity = level.getEntity(track.entityId);
                // Still in the level means it walked or was teleported out of the area.
                String type = entity != null && !entity.isRemoved() ? "exit" : "despawn";
                session.events.add(new Event(tick, type, track.bone, new JsonObject()));
            }
        }
        session.inside = now;
    }

    /** Records the interactions in the area, then ends the tick (after the stands, so both use the same tick). */
    private static void recordHitboxes(Minecraft minecraft, ClientLevel level, Session session) {
        int tick = session.tick;
        Set<UUID> now = new HashSet<>();
        for (Interaction hitbox : level.getEntitiesOfClass(Interaction.class, session.box, e -> session.box.contains(e.position()))) {
            now.add(hitbox.getUUID());
            HitboxTrack track = hitboxTrack(session, hitbox);
            HitboxSnapshot snapshot = new HitboxSnapshot(hitbox.position(), hitbox.getYRot(), hitbox.getBbWidth(), hitbox.getBbHeight(),
                    ((InteractionAccessor) hitbox).apeira$getResponse());
            HitboxSnapshot last = track.frames.get(tick - 1);
            if (last == null) {
                String type = tick == 0 ? "present" : hitbox.tickCount <= 2 ? "spawn" : "enter";
                session.events.add(new Event(tick, type, track.bone, describe(snapshot)));
            } else {
                if (last.width() != snapshot.width() || last.height() != snapshot.height()) {
                    JsonObject details = new JsonObject();
                    details.addProperty("width", snapshot.width());
                    details.addProperty("height", snapshot.height());
                    session.events.add(new Event(tick, "resize", track.bone, details));
                }
                if (last.response() != snapshot.response()) {
                    JsonObject details = new JsonObject();
                    details.addProperty("response", snapshot.response());
                    session.events.add(new Event(tick, "response", track.bone, details));
                }
            }
            track.frames.put(tick, snapshot);
        }
        for (UUID uuid : session.hitboxesInside) {
            if (!now.contains(uuid)) {
                HitboxTrack track = session.hitboxes.get(uuid);
                Entity entity = level.getEntity(track.entityId);
                String type = entity != null && !entity.isRemoved() ? "exit" : "despawn";
                session.events.add(new Event(tick, type, track.bone, new JsonObject()));
            }
        }
        session.hitboxesInside = now;
        UUID targeted = minecraft.crosshairPickEntity instanceof Interaction hitbox && now.contains(hitbox.getUUID()) ? hitbox.getUUID() : null;
        if (!Objects.equals(targeted, session.targeted)) {
            if (session.targeted != null && session.hitboxes.containsKey(session.targeted)) {
                session.events.add(new Event(tick, "target_end", session.hitboxes.get(session.targeted).bone, new JsonObject()));
            }
            if (targeted != null) {
                session.events.add(new Event(tick, "target_start", session.hitboxes.get(targeted).bone, new JsonObject()));
            }
            session.targeted = targeted;
        }
    }

    /** Records every other non-player entity in the area: spawns, exits and changes of what picks its look. */
    private static void recordProps(Minecraft minecraft, ClientLevel level, Session session) {
        int tick = session.tick;
        Set<UUID> now = new HashSet<>();
        var ops = UiCaptureWriter.ops(minecraft);
        for (Entity entity : level.getEntities((Entity) null, session.box, e -> AreaEntities.isProp(e) && session.box.contains(e.position()))) {
            now.add(entity.getUUID());
            PropTrack track = session.props.computeIfAbsent(entity.getUUID(), uuid -> {
                String kind = AreaEntities.kind(entity);
                int index = session.kindCounts.merge(kind, 1, Integer::sum) - 1;
                JsonObject state = AreaEntities.state(entity, ops);
                String model = state.get("item") instanceof JsonObject item
                        ? modelName(session, item, state.has("item_display") ? state.get("item_display").getAsString() : "fixed") : null;
                return new PropTrack(uuid, session.name(model == null ? kind + "_" + index : model), kind, AreaEntities.typeId(entity), entity.getId(), tick);
            });
            PropSnapshot snapshot = new PropSnapshot(entity.position(), entity.getYRot(), entity.getXRot(), AreaEntities.state(entity, ops),
                    AreaEntities.transform(entity));
            PropSnapshot last = track.frames.get(tick - 1);
            if (last == null) {
                String type = tick == 0 ? "present" : entity.tickCount <= 2 ? "spawn" : "enter";
                session.events.add(new Event(tick, type, track.bone, snapshot.state().deepCopy()));
            } else if (!last.state().equals(snapshot.state())) {
                // Only the keys that changed, before and after: an item swap, a new block or text, a frame turned.
                JsonObject from = new JsonObject();
                JsonObject to = new JsonObject();
                for (String key : union(last.state(), snapshot.state())) {
                    if (!Objects.equals(last.state().get(key), snapshot.state().get(key))) {
                        from.add(key, last.state().get(key));
                        to.add(key, snapshot.state().get(key));
                    }
                }
                JsonObject details = new JsonObject();
                details.add("from", from);
                details.add("to", to);
                session.events.add(new Event(tick, "change", track.bone, details));
            }
            track.frames.put(tick, snapshot);
        }
        for (UUID uuid : session.propsInside) {
            if (!now.contains(uuid)) {
                PropTrack track = session.props.get(uuid);
                Entity entity = level.getEntity(track.entityId);
                String type = entity != null && !entity.isRemoved() ? "exit" : "despawn";
                session.events.add(new Event(tick, type, track.bone, new JsonObject()));
            }
        }
        session.propsInside = now;
    }

    private static Set<String> union(JsonObject a, JsonObject b) {
        Set<String> keys = new java.util.LinkedHashSet<>(a.keySet());
        keys.addAll(b.keySet());
        return keys;
    }

    private static HitboxTrack hitboxTrack(Session session, Interaction hitbox) {
        return session.hitboxes.computeIfAbsent(hitbox.getUUID(),
                uuid -> new HitboxTrack(uuid, session.name("interaction_" + session.hitboxes.size()), hitbox.getId(), session.tick));
    }

    private static JsonObject describe(HitboxSnapshot snapshot) {
        JsonObject json = new JsonObject();
        json.addProperty("width", snapshot.width());
        json.addProperty("height", snapshot.height());
        json.addProperty("response", snapshot.response());
        return json;
    }

    /** {@code AttackEntityCallback}: the player attacking an interaction in the area. Never changes the attack. */
    public static InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
        click("attack", level, hand, entity, hit);
        return InteractionResult.PASS;
    }

    /** {@code UseEntityCallback}: the player using (right-clicking) an interaction in the area. Never changes the use. */
    public static InteractionResult onUse(Player player, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
        click("use", level, hand, entity, hit);
        return InteractionResult.PASS;
    }

    private static void click(String type, Level level, InteractionHand hand, Entity entity, @Nullable EntityHitResult hit) {
        Session current = session;
        if (current == null || !level.isClientSide() || !(entity instanceof Interaction hitbox) || !current.box.contains(hitbox.position())) {
            return;
        }
        HitboxTrack track = hitboxTrack(current, hitbox);
        // The client asks once per hand, and for a use both with and without the hit point: one event per tick is enough.
        for (int i = current.events.size() - 1; i >= 0 && current.events.get(i).tick() == current.tick; i--) {
            Event event = current.events.get(i);
            if (event.type().equals(type) && event.bone().equals(track.bone)) {
                if (hit != null && !event.details().has("hit")) {
                    event.details().add("hit", offset(hit.getLocation(), hitbox.position()));
                }
                return;
            }
        }
        JsonObject details = new JsonObject();
        details.addProperty("hand", hand == InteractionHand.MAIN_HAND ? "main_hand" : "off_hand");
        details.addProperty("response", ((InteractionAccessor) hitbox).apeira$getResponse());
        if (hit != null) {
            details.add("hit", offset(hit.getLocation(), hitbox.position()));
        }
        current.events.add(new Event(current.tick, type, track.bone, details));
    }

    /** Where a click landed, in blocks from the interaction's position (the bottom centre of its hitbox). */
    private static JsonArray offset(Vec3 location, Vec3 origin) {
        JsonArray array = new JsonArray();
        array.add(Math.round((location.x - origin.x) * 1000) / 1000.0);
        array.add(Math.round((location.y - origin.y) * 1000) / 1000.0);
        array.add(Math.round((location.z - origin.z) * 1000) / 1000.0);
        return array;
    }

    private static Snapshot snapshot(ArmorStand stand) {
        JsonObject[] items = new JsonObject[SLOTS.length];
        for (int i = 0; i < SLOTS.length; i++) {
            items[i] = item(stand.getItemBySlot(SLOTS[i]));
        }
        return new Snapshot(stand.position(), stand.getYRot(), stand.isInvisible(), stand.isSmall(), stand.isMarker(), stand.showArms(),
                stand.showBasePlate(), stand.getHeadPose(), stand.getBodyPose(), stand.getLeftArmPose(), stand.getRightArmPose(),
                stand.getLeftLegPose(), stand.getRightLegPose(), items);
    }

    /** The parts of an item that pick its model: id, item model, custom model data and dye colour. */
    static @Nullable JsonObject item(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() != 1) {
            json.addProperty("count", stack.getCount());
        }
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model != null) {
            json.addProperty("item_model", model.toString());
        }
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data != null) {
            JsonObject cmd = new JsonObject();
            addList(cmd, "floats", data.floats());
            addList(cmd, "flags", data.flags());
            addList(cmd, "strings", data.strings());
            addList(cmd, "colors", data.colors());
            json.add("custom_model_data", cmd);
        }
        DyedItemColor dye = stack.get(DataComponents.DYED_COLOR);
        if (dye != null) {
            json.addProperty("dyed_color", String.format("#%06X", dye.rgb() & 0xFFFFFF));
        }
        return json;
    }

    private static void addList(JsonObject json, String key, List<?> values) {
        if (values.isEmpty()) {
            return;
        }
        JsonArray array = new JsonArray();
        for (Object value : values) {
            if (value instanceof Number number) {
                array.add(number);
            } else if (value instanceof Boolean flag) {
                array.add(flag);
            } else {
                array.add(String.valueOf(value));
            }
        }
        json.add(key, array);
    }

    private static boolean same(@Nullable JsonObject a, @Nullable JsonObject b) {
        return a == null ? b == null : a.equals(b);
    }

    private static JsonObject describe(Snapshot snapshot) {
        JsonObject json = new JsonObject();
        json.addProperty("invisible", snapshot.invisible());
        json.addProperty("small", snapshot.small());
        json.addProperty("marker", snapshot.marker());
        JsonObject items = new JsonObject();
        for (int i = 0; i < SLOTS.length; i++) {
            if (snapshot.items()[i] != null) {
                items.add(SLOTS[i].getName(), snapshot.items()[i]);
            }
        }
        json.add("items", items);
        return json;
    }

    /** Dust along the area's twelve edges, with its two corners marked: pos1 red, pos2 green. */
    private static void drawOutline(ClientLevel level) {
        BlockPos min = BlockPos.min(pos1, pos2);
        BlockPos max = BlockPos.max(pos1, pos2);
        double x0 = min.getX(), y0 = min.getY(), z0 = min.getZ();
        double x1 = max.getX() + 1, y1 = max.getY() + 1, z1 = max.getZ() + 1;
        double edges = 4 * ((x1 - x0) + (y1 - y0) + (z1 - z0));
        double step = Math.max(0.5, edges / OUTLINE_PARTICLES);
        double[][] lines = {
                {x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
        for (double[] line : lines) {
            double length = Math.abs(line[3] - line[0]) + Math.abs(line[4] - line[1]) + Math.abs(line[5] - line[2]);
            int count = (int) Math.ceil(length / step);
            for (int i = 0; i <= count; i++) {
                double t = count == 0 ? 0 : (double) i / count;
                level.addParticle(OUTLINE, true, true, line[0] + (line[3] - line[0]) * t, line[1] + (line[4] - line[1]) * t,
                        line[2] + (line[5] - line[2]) * t, 0, 0, 0);
            }
        }
        level.addParticle(CORNER_1, true, true, pos1.getX() + 0.5, pos1.getY() + 0.5, pos1.getZ() + 0.5, 0, 0, 0);
        level.addParticle(CORNER_2, true, true, pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5, 0, 0, 0);
    }
}
