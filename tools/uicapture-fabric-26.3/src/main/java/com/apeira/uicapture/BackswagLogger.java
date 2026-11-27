package com.apeira.uicapture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.phys.AABB;

import com.apeira.uicapture.mixin.AbstractContainerScreenAccessor;

/**
 * The backswag height logger, for rendering a server's back cosmetics 1:1. Servers such as Origin Realms show a worn
 * back cosmetic as the head item of an invisible armor stand riding above the player, so its height is that stand's
 * offset. "Backswag: Log Hovered Cosmetic" (inside a screen) logs the hovered slot's item: id, translation key, name,
 * item model, custom model data, its colours ({@code minecraft:firework_explosion}, dye, custom model data colours; the
 * first firework colour, unless white, is the cosmetic's default paint when it is ported), its full tooltip and every
 * component. "Backswag: Log Head Height" (no screen) logs every invisible armor stand within {@link #RADIUS} blocks
 * that wears something on its head: the item with the same colour fields (its {@code match} key equals the hovered
 * entry's when it is the same cosmetic), and {@code dy = stand.getY() - player.getY()} at full precision, plus the x/z
 * offset, the yaw difference, the head pose and the small and marker flags, which change where the head item is drawn.
 * Both go to chat and are appended to {@link #LOG}, one JSON object per line.
 */
final class BackswagLogger {
    static final String LOG = "logs/apeira_backswag_log.jsonl";
    static final String OFFSETS = "logs/apeira_backswag_offsets.json";
    static final double RADIUS = 5.0;
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** The last hovered cosmetic, which the next height log links its stand to. */
    private static @Nullable JsonObject armed;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private BackswagLogger() {}

    /** The hovered slot's item in the open screen. */
    static void logHovered(Minecraft minecraft, Screen screen) {
        JsonObject json = hovered(minecraft, screen, "hovered_cosmetic");
        if (json == null) {
            return;
        }
        write(json);
        armed = json;
        print("[Backswag]", json);
        Feedback.ok(Component.literal("  -> " + LOG + "; equip it, close the menu and press the height key to link its offset")
                .withStyle(ChatFormatting.GRAY));
    }

    /** The hovered slot's item as a log entry of type {@code type} (with its tooltip and components), or null after saying there is none. */
    static @Nullable JsonObject hovered(Minecraft minecraft, Screen screen, String type) {
        Slot slot = screen instanceof AbstractContainerScreen<?> container
                ? ((AbstractContainerScreenAccessor) container).apeira$hoveredSlot() : null;
        if (slot == null || !slot.hasItem()) {
            Feedback.error(Component.translatable("message.apeira.backswag.no_hovered_item"));
            return null;
        }
        ItemStack stack = slot.getItem();
        DynamicOps<JsonElement> ops = UiCaptureWriter.ops(minecraft);
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("time", LocalDateTime.now().toString());
        json.addProperty("screen", screen.getTitle().getString());
        json.addProperty("screen_class", screen.getClass().getName());
        json.addProperty("slot", slot.index);
        describe(stack, json, ops);
        List<Component> tooltip = Screen.getTooltipFromItem(minecraft, stack);
        JsonArray lines = new JsonArray();
        JsonArray linesJson = new JsonArray();
        for (Component line : tooltip) {
            lines.add(line.getString());
            linesJson.add(component(line, ops));
        }
        json.add("tooltip", lines);
        json.add("tooltip_json", linesJson);
        json.add("components", UiCaptureWriter.item(stack, ops));
        return json;
    }

    /** A hovered entry in chat: name, id, match key and colours (the tooltip is in the log). */
    static void print(String tag, JsonObject json) {
        Feedback.ok(Component.literal(tag + " Hovered: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(json.get("name").getAsString()).withStyle(ChatFormatting.WHITE)));
        Feedback.ok(copyable("  match " + json.get("match").getAsString()));
        Feedback.ok(copyable("  colours " + colours(json)));
    }

    /** Every invisible armor stand near the player with a head item, nearest first. */
    static void logHeights(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        // Around the player and the camera, which is elsewhere while spectating (the wardrobe's turning dummy).
        AABB box = player.getBoundingBox().inflate(RADIUS);
        Entity camera = minecraft.getCameraEntity();
        if (camera != null && camera != player) {
            box = box.minmax(camera.getBoundingBox().inflate(RADIUS));
        }
        AABB search = box;
        List<ArmorStand> stands = new ArrayList<>(minecraft.level.getEntitiesOfClass(ArmorStand.class, box,
                stand -> stand.isInvisible() && !stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty()));
        if (stands.isEmpty()) {
            Feedback.error(Component.translatable("message.apeira.backswag.no_stands", (int) RADIUS));
            return;
        }
        stands.sort(Comparator.comparingDouble(stand -> stand.distanceToSqr(player)));
        DynamicOps<JsonElement> ops = UiCaptureWriter.ops(minecraft);
        Feedback.ok(Component.literal("[Backswag] " + stands.size() + " invisible head-item stand(s) within " + (int) RADIUS + " blocks:")
                .withStyle(ChatFormatting.GOLD));
        List<JsonObject> logged = new ArrayList<>();
        for (ArmorStand stand : stands) {
            ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
            // The wearer: the nearest avatar (a mannequin dummy or another player) close to the stand, else the player.
            LivingEntity wearer = wearer(minecraft, stand, search);
            double dy = stand.getY() - wearer.getY();
            double dx = stand.getX() - wearer.getX();
            double dz = stand.getZ() - wearer.getZ();
            float bodyYaw = wearer.yBodyRot;
            double yawRad = Math.toRadians(bodyYaw);
            // The wearer's body frame: forward is where the body faces (yaw 0 is +z), right is its right hand side.
            double forward = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
            double right = -dx * Math.cos(yawRad) - dz * Math.sin(yawRad);
            float yawDelta = wrap(stand.getYRot() - bodyYaw);
            JsonObject json = new JsonObject();
            json.addProperty("type", "backswag_height");
            json.addProperty("time", LocalDateTime.now().toString());
            json.addProperty("wearer", wearer == player ? "player" : BuiltInRegistries.ENTITY_TYPE.getKey(wearer.getType()).toString());
            json.addProperty("wearer_name", wearer.getName().getString());
            json.addProperty("wearer_scale", wearer.getScale());
            json.addProperty("dy", dy);
            json.addProperty("dx", dx);
            json.addProperty("dz", dz);
            json.addProperty("forward", forward);
            json.addProperty("right", right);
            json.addProperty("dy_scaled", dy / wearer.getScale());
            json.addProperty("stand_y", stand.getY());
            json.addProperty("wearer_y", wearer.getY());
            json.addProperty("player_y", player.getY());
            json.addProperty("player_eye_height", wearer.getEyeHeight());
            json.addProperty("player_pose", wearer.getPose().name());
            json.addProperty("stand_yaw", stand.getYRot());
            json.addProperty("wearer_yaw", wearer.getYRot());
            json.addProperty("wearer_body_yaw", bodyYaw);
            json.addProperty("wearer_head_yaw", wearer.yHeadRot);
            json.addProperty("yaw_delta", yawDelta);
            json.addProperty("small", stand.isSmall());
            json.addProperty("marker", stand.isMarker());
            json.addProperty("riding_player", isRiding(stand, wearer));
            Rotations pose = stand.getHeadPose();
            JsonArray headPose = new JsonArray();
            headPose.add(pose.x());
            headPose.add(pose.y());
            headPose.add(pose.z());
            json.add("head_pose", headPose);
            json.addProperty("entity_id", stand.getId());
            describe(head, json, ops);
            json.add("components", UiCaptureWriter.item(head, ops));
            write(json);
            logged.add(json);

            String dyText = String.format(Locale.ROOT, "%.6f", dy);
            Feedback.ok(Component.literal("  dy = ").withStyle(ChatFormatting.GRAY)
                    .append(copyable(dyText).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(String.format(Locale.ROOT, "  over %s (forward %.4f, right %.4f, yaw %+.1f%s%s)",
                            wearer == player ? "you" : wearer.getName().getString(), forward, right, yawDelta,
                            stand.isSmall() ? ", small" : "", stand.isMarker() ? ", marker" : "")).withStyle(ChatFormatting.GRAY)));
            Feedback.ok(copyable("    " + head.getHoverName().getString() + "  " + json.get("match").getAsString()));
            Feedback.ok(copyable("    colours " + colours(json)));
            if (pose.x() != 0 || pose.y() != 0 || pose.z() != 0) {
                Feedback.ok(Component.literal(String.format(Locale.ROOT, "    head pose %.2f, %.2f, %.2f", pose.x(), pose.y(), pose.z()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        Feedback.ok(Component.literal("  -> " + LOG).withStyle(ChatFormatting.GRAY));
        link(logged);
    }

    /**
     * Links one of the logged stands to the last hovered cosmetic and stores it in {@link #OFFSETS} under the cosmetic's
     * name (lowercase, {@code cosmo_robe}), replacing an earlier entry of that name, so going through every backswag
     * builds one table. The stand is the one whose head item has the hovered item's {@code match} key, else the one
     * riding the player, else the nearest one above the player's eyes. The hovered cosmetic is used once.
     */
    private static void link(List<JsonObject> logged) {
        JsonObject cosmetic = armed;
        if (cosmetic == null) {
            Feedback.ok(Component.literal("  Not linked: hover a cosmetic and press the hover key first").withStyle(ChatFormatting.YELLOW));
            return;
        }
        String how = "match";
        JsonObject stand = logged.stream().filter(e -> e.get("match").equals(cosmetic.get("match"))).findFirst().orElse(null);
        if (stand == null) {
            how = "riding_player";
            stand = logged.stream().filter(e -> e.get("riding_player").getAsBoolean()).findFirst().orElse(null);
        }
        if (stand == null) {
            how = "nearest_above_eyes";
            stand = logged.stream().filter(e -> e.get("dy").getAsDouble() > e.get("player_eye_height").getAsDouble()).findFirst().orElse(null);
        }
        if (stand == null) {
            Feedback.error(Component.literal("  Not linked: no stand matches " + cosmetic.get("name").getAsString() + ", rides you or is above your eyes"));
            return;
        }
        String key = cosmetic.get("name").getAsString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (key.isEmpty()) {
            key = cosmetic.get("match").getAsString();
        }
        JsonObject entry = new JsonObject();
        entry.addProperty("name", cosmetic.get("name").getAsString());
        entry.addProperty("linked_by", how);
        entry.addProperty("time", LocalDateTime.now().toString());
        for (String field : new String[] {"wearer", "wearer_name", "wearer_scale", "forward", "right", "dy_scaled", "dy", "dx", "dz", "yaw_delta", "head_pose", "small", "marker", "riding_player", "player_pose", "player_eye_height"}) {
            entry.add(field, stand.get(field));
        }
        entry.add("cosmetic", cosmetic);
        entry.add("worn", stand);
        Path file = FabricLoader.getInstance().getGameDir().resolve(OFFSETS);
        try {
            JsonObject table = Files.exists(file)
                    ? com.google.gson.JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject() : new JsonObject();
            table.add(key, entry);
            Files.createDirectories(file.getParent());
            Files.writeString(file, PRETTY.toJson(table), StandardCharsets.UTF_8);
            armed = null;
            Feedback.ok(Component.literal("  Linked " + key + " -> dy " + String.format(Locale.ROOT, "%.6f", stand.get("dy").getAsDouble())
                    + " (by " + how + ", " + table.size() + " in " + OFFSETS + ")").withStyle(ChatFormatting.GREEN));
            if (!how.equals("match")) {
                Feedback.ok(Component.literal("  The worn item differs from the hovered one; check the stand is the right one").withStyle(ChatFormatting.YELLOW));
            }
        } catch (IOException | RuntimeException e) {
            UiCaptureClient.LOGGER.error("Backswag offset table failed", e);
            Feedback.error(Component.literal("Backswag offset table failed: " + e.getMessage()));
        }
    }

    /** The nearest avatar other than the local player within 3 blocks across of the stand, else the player. */
    private static LivingEntity wearer(Minecraft minecraft, ArmorStand stand, AABB search) {
        Player player = minecraft.player;
        LivingEntity best = null;
        double bestDistance = 9.0;
        for (Avatar avatar : minecraft.level.getEntitiesOfClass(Avatar.class, search.inflate(3.0), avatar -> avatar != player)) {
            double x = avatar.getX() - stand.getX();
            double z = avatar.getZ() - stand.getZ();
            double distance = x * x + z * z;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = avatar;
            }
        }
        return best != null ? best : player;
    }

    /** True when the stand rides the player, directly or through other passengers. */
    private static boolean isRiding(ArmorStand stand, Entity player) {
        for (net.minecraft.world.entity.Entity vehicle = stand.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            if (vehicle == player) {
                return true;
            }
        }
        return false;
    }

    /** The item's id, keys, name and colours, and a {@code match} key shared by the hovered item and the worn one. */
    private static void describe(ItemStack stack, JsonObject json, DynamicOps<JsonElement> ops) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        json.addProperty("id", id);
        json.addProperty("translation_key", stack.getItem().getDescriptionId());
        Component name = stack.getHoverName();
        json.addProperty("name", name.getString());
        if (name.getContents() instanceof TranslatableContents translatable) {
            json.addProperty("name_key", translatable.getKey());
        }
        json.add("name_json", component(name, ops));
        Identifier model = stack.get(DataComponents.ITEM_MODEL);
        if (model != null) {
            json.addProperty("item_model", model.toString());
        }
        StringBuilder match = new StringBuilder(id);
        if (model != null) {
            match.append(" model=").append(model);
        }
        CustomModelData cmd = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            JsonObject data = new JsonObject();
            data.add("floats", array(cmd.floats()));
            data.add("flags", array(cmd.flags()));
            data.add("strings", array(cmd.strings()));
            JsonArray colors = new JsonArray();
            for (int color : cmd.colors()) {
                colors.add(hex(color));
            }
            data.add("colors", colors);
            json.add("custom_model_data", data);
            if (!cmd.floats().isEmpty()) {
                match.append(" cmd=").append(cmd.floats());
            }
            if (!cmd.strings().isEmpty()) {
                match.append(" cmd_strings=").append(cmd.strings());
            }
        }
        FireworkExplosion firework = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (firework != null) {
            JsonArray colors = new JsonArray();
            for (int i = 0; i < firework.colors().size(); i++) {
                colors.add(hex(firework.colors().getInt(i)));
            }
            JsonArray fades = new JsonArray();
            for (int i = 0; i < firework.fadeColors().size(); i++) {
                fades.add(hex(firework.fadeColors().getInt(i)));
            }
            JsonObject explosion = new JsonObject();
            explosion.addProperty("shape", firework.shape().name());
            explosion.add("colors", colors);
            explosion.add("fade_colors", fades);
            json.add("firework_explosion", explosion);
            if (!firework.colors().isEmpty()) {
                int first = firework.colors().getInt(0) & 0xFFFFFF;
                // Apeira's rule: the captured firework colour is the ported item's default paint; white means none.
                if (first != 0xFFFFFF) {
                    json.addProperty("default_paint", hex(first));
                }
            }
        }
        DyedItemColor dye = stack.get(DataComponents.DYED_COLOR);
        if (dye != null) {
            json.addProperty("dyed_color", hex(dye.rgb()));
        }
        json.addProperty("match", match.toString());
    }

    private static String colours(JsonObject json) {
        StringBuilder out = new StringBuilder();
        if (json.has("default_paint")) {
            out.append("default_paint ").append(json.get("default_paint").getAsString()).append(' ');
        }
        if (json.has("firework_explosion")) {
            out.append("firework ").append(json.getAsJsonObject("firework_explosion").get("colors")).append(' ');
        }
        if (json.has("dyed_color")) {
            out.append("dyed ").append(json.get("dyed_color").getAsString()).append(' ');
        }
        if (json.has("custom_model_data")) {
            JsonArray colors = json.getAsJsonObject("custom_model_data").getAsJsonArray("colors");
            if (!colors.isEmpty()) {
                out.append("cmd_colors ").append(colors).append(' ');
            }
        }
        return out.isEmpty() ? "none" : out.toString().trim();
    }

    /** A grey chat line that copies its text when clicked. */
    static MutableComponent copyable(String text) {
        return Component.literal(text).withStyle(style -> style.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent.CopyToClipboard(text.trim())));
    }

    private static JsonElement component(Component component, DynamicOps<JsonElement> ops) {
        return ComponentSerialization.CODEC.encodeStart(ops, component).result().orElseGet(() -> new com.google.gson.JsonPrimitive(component.getString()));
    }

    private static JsonArray array(List<?> values) {
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
        return array;
    }

    private static String hex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static float wrap(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    private static void write(JsonObject json) {
        append(LOG, json);
    }

    /** Appends {@code json} as one line to {@code log} in the game directory. */
    static void append(String log, JsonObject json) {
        Path file = FabricLoader.getInstance().getGameDir().resolve(log);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            UiCaptureClient.LOGGER.info("[backswag] {}", GSON.toJson(json));
        } catch (IOException e) {
            UiCaptureClient.LOGGER.error("Backswag log failed", e);
            Feedback.error(Component.literal("Backswag log failed: " + e.getMessage()));
        }
    }
}
