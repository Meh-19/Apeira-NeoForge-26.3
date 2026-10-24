package com.apeira.uicapture;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import com.mojang.serialization.DynamicOps;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import com.apeira.uicapture.mixin.BlockDisplayAccessor;
import com.apeira.uicapture.mixin.DisplayAccessor;
import com.apeira.uicapture.mixin.ItemDisplayAccessor;
import com.apeira.uicapture.mixin.TextDisplayAccessor;

/**
 * What the armor stand recorder reads from the other entities in its area: display entities (item, block and text),
 * item frames (glow ones included), dropped items and any other non-player entity, each described as JSON with what
 * picks its look (item or block, custom model data, billboard mode, text, frame rotation and facing), and display
 * entities' transformation as the server set it. {@link #dump} lists an area's entities by type for
 * {@code /apeiradev dumparea}.
 */
final class AreaEntities {
    /** A display entity's transformation: translation (blocks), left rotation, scale and right rotation, applied after the entity's own yaw and pitch. */
    record Transform(Vector3fc translation, Quaternionfc left, Vector3fc scale, Quaternionfc right) {
        static Transform of(Transformation transformation) {
            return new Transform(transformation.getTranslation(), transformation.getLeftRotation(), transformation.getScale(),
                    transformation.getRightRotation());
        }

        JsonObject json() {
            JsonObject json = new JsonObject();
            json.add("translation", vector(translation.x(), translation.y(), translation.z()));
            json.add("left_rotation", vector(left.x(), left.y(), left.z(), left.w()));
            json.add("scale", vector(scale.x(), scale.y(), scale.z()));
            json.add("right_rotation", vector(right.x(), right.y(), right.z(), right.w()));
            return json;
        }
    }

    private AreaEntities() {
    }

    /**
     * Whether the recorder tracks an entity as a prop: everything but players and the armor stands and interactions it
     * tracks on their own.
     */
    static boolean isProp(Entity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand) && !(entity instanceof Interaction);
    }

    /** The prop's kind, which names its bone: {@code item_display}, {@code block_display}, {@code text_display}, {@code item_frame}, {@code item} or {@code entity}. */
    static String kind(Entity entity) {
        return switch (entity) {
            case Display.ItemDisplay ignored -> "item_display";
            case Display.BlockDisplay ignored -> "block_display";
            case Display.TextDisplay ignored -> "text_display";
            case ItemFrame ignored -> "item_frame";
            case ItemEntity ignored -> "item";
            default -> "entity";
        };
    }

    static String typeId(Entity entity) {
        return EntityType.getKey(entity.getType()).toString();
    }

    /** A display entity's transformation, or {@code null} for other entities. */
    static @Nullable Transform transform(Entity entity) {
        return entity instanceof Display display ? Transform.of(DisplayAccessor.apeira$createTransformation(display.getEntityData())) : null;
    }

    /** What picks the entity's look, without its position or transformation (those are tracked on their own). */
    static JsonObject state(Entity entity, DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        json.addProperty("type", typeId(entity));
        if (entity instanceof Display display) {
            json.addProperty("billboard", ((DisplayAccessor) display).apeira$getBillboardConstraints().getSerializedName());
        }
        switch (entity) {
            case Display.ItemDisplay display -> {
                ItemDisplayAccessor accessor = (ItemDisplayAccessor) display;
                json.add("item", itemOrNull(accessor.apeira$getItemStack()));
                json.addProperty("item_display", accessor.apeira$getItemTransform().getSerializedName());
            }
            case Display.BlockDisplay display -> json.addProperty("block_state", BlockStateParser.serialize(((BlockDisplayAccessor) display).apeira$getBlockState()));
            case Display.TextDisplay display -> {
                TextDisplayAccessor accessor = (TextDisplayAccessor) display;
                Component text = accessor.apeira$getText();
                json.addProperty("text", text.getString());
                json.add("text_json", ComponentSerialization.CODEC.encodeStart(ops, text).result().orElse(JsonNull.INSTANCE));
                json.addProperty("line_width", accessor.apeira$getLineWidth());
                json.addProperty("background", String.format("#%08X", accessor.apeira$getBackgroundColor()));
                json.addProperty("text_opacity", accessor.apeira$getTextOpacity() & 0xFF);
                json.addProperty("flags", accessor.apeira$getFlags());
            }
            case ItemFrame frame -> {
                json.add("item", itemOrNull(frame.getItem()));
                json.addProperty("rotation", frame.getRotation());
                json.addProperty("facing", frame.getDirection().getSerializedName());
            }
            case ItemEntity item -> json.add("item", itemOrNull(item.getItem()));
            default -> {
            }
        }
        if (entity.isInvisible()) {
            json.addProperty("invisible", true);
        }
        Component custom = entity.getCustomName();
        if (custom != null) {
            json.addProperty("custom_name", custom.getString());
        }
        return json;
    }

    private static JsonElement itemOrNull(ItemStack stack) {
        JsonObject item = ArmorStandRecorder.item(stack);
        return item == null ? JsonNull.INSTANCE : item;
    }

    /** Every non-player entity in the box, counted by type, with the items they show counted by id and custom model data. */
    static void dump(Minecraft minecraft, AABB box, Consumer<Component> chat) {
        Map<String, Integer> types = new TreeMap<>();
        Map<String, Integer> items = new TreeMap<>();
        List<Entity> entities = minecraft.level.getEntities((Entity) null, box, e -> !(e instanceof Player) && box.contains(e.position()));
        for (Entity entity : entities) {
            types.merge(typeId(entity), 1, Integer::sum);
            for (ItemStack stack : shownItems(entity)) {
                JsonObject item = ArmorStandRecorder.item(stack);
                if (item != null) {
                    items.merge(itemLabel(item), 1, Integer::sum);
                }
            }
        }
        chat.accept(Component.translatable("message.apeira.armor_stands.dump", entities.size(), types.size()).withStyle(ChatFormatting.GOLD));
        types.forEach((type, count) -> chat.accept(Component.literal("  " + count + "x " + type).withStyle(ChatFormatting.GRAY)));
        if (!items.isEmpty()) {
            chat.accept(Component.translatable("message.apeira.armor_stands.dump_items", items.size()).withStyle(ChatFormatting.GOLD));
            items.forEach((item, count) -> chat.accept(Component.literal("  " + count + "x " + item).withStyle(ChatFormatting.GRAY)));
        }
    }

    private static List<ItemStack> shownItems(Entity entity) {
        return switch (entity) {
            case ArmorStand stand -> java.util.Arrays.stream(ArmorStandRecorder.SLOTS).map(stand::getItemBySlot).toList();
            case Display.ItemDisplay display -> List.of(((ItemDisplayAccessor) display).apeira$getItemStack());
            case ItemFrame frame -> List.of(frame.getItem());
            case ItemEntity item -> List.of(item.getItem());
            default -> List.of();
        };
    }

    /** An item as one line: id, then item model and custom model data when set. */
    static String itemLabel(JsonObject item) {
        StringBuilder label = new StringBuilder(item.get("id").getAsString());
        if (item.has("item_model")) {
            label.append(" model=").append(item.get("item_model").getAsString());
        }
        if (item.has("custom_model_data")) {
            label.append(" cmd=").append(item.get("custom_model_data"));
        }
        return label.toString();
    }

    private static JsonArray vector(float... values) {
        JsonArray array = new JsonArray();
        for (float value : values) {
            array.add(Math.round(value * 10000) / 10000.0);
        }
        return array;
    }
}
