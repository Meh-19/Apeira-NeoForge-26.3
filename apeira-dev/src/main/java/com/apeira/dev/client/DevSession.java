package com.apeira.dev.client;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.decor.cosmetic.BackOffsets;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTunings;
import com.apeira.dev.DevItems;
import com.apeira.decor.registry.DecorAttachments;
import com.apeira.dev.network.BackOffsetPayload;
import com.apeira.dev.network.FurnitureTuningPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The client's tuning session: the furniture piece the dev wand is bound to, the selected {@link TuningParameter} and
 * the selected box, or, bound to nothing, the player's own back cosmetic and its selected {@link BackParameter}. Edits
 * change the client's own {@link FurnitureTunings} or {@link BackOffsets} at once, so the piece or cosmetic updates
 * without waiting, and go to the server, which sends everyone the result.
 */
public final class DevSession {
    private static @Nullable BlockPos target;
    private static TuningParameter parameter = TuningParameter.MIN_X;
    private static int box;
    private static BackParameter backParameter = BackParameter.Y;

    private DevSession() {}

    public static @Nullable BlockPos target() {
        return target;
    }

    public static TuningParameter parameter() {
        return parameter;
    }

    public static int box() {
        return box;
    }

    static void bind(BlockPos pos, FurnitureBlock block) {
        boolean changed = !pos.equals(target);
        target = pos.immutable();
        if (changed) {
            box = 0;
        }
        message(Component.translatable("message.apeira.dev.bound", block.getName(), pos.toShortString()));
    }

    static void unbind() {
        if (target != null) {
            target = null;
            message(Component.translatable("message.apeira.dev.unbound"));
        }
    }

    /** Forgets the session without a message, as when leaving the world. */
    static void clear() {
        target = null;
        box = 0;
    }

    /** The bound piece's block, or {@code null} when nothing (or no longer a furniture piece) is bound. */
    public static @Nullable FurnitureBlock block() {
        Minecraft minecraft = Minecraft.getInstance();
        if (target == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockState(target).getBlock() instanceof FurnitureBlock block ? block : null;
    }

    public static @Nullable BlockState state() {
        Minecraft minecraft = Minecraft.getInstance();
        return target == null || minecraft.level == null ? null : minecraft.level.getBlockState(target);
    }

    /** {@code block}'s tuning, or the catalogue's values while it has none. */
    public static FurnitureTuning tuning(FurnitureBlock block) {
        FurnitureTuning tuning = FurnitureTunings.get(block);
        return tuning != null ? tuning : FurnitureTuning.of(block.furniture());
    }

    /** Whether {@code block} has been changed from the catalogue. */
    public static boolean tuned(FurnitureBlock block) {
        return FurnitureTunings.get(block) != null;
    }

    static void selectParameter(int steps) {
        TuningParameter[] values = TuningParameter.values();
        FurnitureBlock block = block();
        int index = parameter.ordinal();
        // Skips the seat height of pieces without a seat.
        for (int i = 0; i < values.length; i++) {
            index = Math.floorMod(index + Integer.signum(steps), values.length);
            if (block == null || values[index].appliesTo(block)) {
                break;
            }
        }
        parameter = values[index];
    }

    static void selectBox(int index, FurnitureTuning tuning) {
        box = tuning.boxes().isEmpty() ? 0 : Math.floorMod(index, tuning.boxes().size());
    }

    /** Changes the bound piece and sends the change to the server. */
    static void apply(FurnitureBlock block, FurnitureTuning tuning) {
        FurnitureTunings.set(block, tuning);
        selectBox(box, tuning);
        ApeiraClientNetwork.sendToServer(new FurnitureTuningPayload(BuiltInRegistries.BLOCK.getKey(block), Optional.of(tuning)));
    }

    /** Gives the bound piece back the catalogue's values. */
    static void reset(FurnitureBlock block) {
        FurnitureTunings.set(block, null);
        box = 0;
        ApeiraClientNetwork.sendToServer(new FurnitureTuningPayload(BuiltInRegistries.BLOCK.getKey(block), Optional.empty()));
    }

    public static BackParameter backParameter() {
        return backParameter;
    }

    static void selectBackParameter(int steps) {
        BackParameter[] values = BackParameter.values();
        backParameter = values[Math.floorMod(backParameter.ordinal() + Integer.signum(steps), values.length)];
    }

    /**
     * The back cosmetic the wand tunes: the player's own, while the wand is bound to no furniture piece; {@code null}
     * otherwise or when nothing is worn on the back.
     */
    public static @Nullable Item backCosmetic() {
        Player player = Minecraft.getInstance().player;
        if (player == null || block() != null) {
            return null;
        }
        ItemStack back = player.getData(DecorAttachments.WARDROBE.get()).get(CosmeticSlot.BACK);
        return back.isEmpty() ? null : back.getItem();
    }

    /** {@code item}'s offset, or the fixed transform's while it has none. */
    public static BackOffset backOffset(Item item) {
        BackOffset offset = BackOffsets.get(item);
        return offset != null ? offset : BackOffset.DEFAULT;
    }

    /** Moves the back cosmetic and sends the change to the server. */
    static void applyBack(Item item, BackOffset offset) {
        BackOffsets.set(item, offset);
        ApeiraClientNetwork.sendToServer(new BackOffsetPayload(BuiltInRegistries.ITEM.getKey(item), Optional.of(offset)));
    }

    /** Gives the back cosmetic back the fixed transform. */
    static void resetBack(Item item) {
        BackOffsets.set(item, null);
        ApeiraClientNetwork.sendToServer(new BackOffsetPayload(BuiltInRegistries.ITEM.getKey(item), Optional.empty()));
    }

    static boolean holdingWand(@Nullable Player player) {
        return player != null && (player.getMainHandItem().is(DevItems.FURNITURE_DEV_WAND) || player.getOffhandItem().is(DevItems.FURNITURE_DEV_WAND));
    }

    static void message(Component message) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(message);
        }
    }
}
