package com.apeira.dev.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.decor.cosmetic.BackOffset;
import com.apeira.decor.cosmetic.BackOffsets;
import com.apeira.decor.cosmetic.CosmeticSlot;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.FurnitureTunings;
import com.apeira.decor.multiblock.Footprint;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The client's tuning session: the furniture piece the dev wand is bound to, the tuner's {@link TunerMode}, the selected
 * {@link TuningParameter} and seat, or, bound to nothing, the player's own back cosmetic and its selected
 * {@link BackParameter}. Edits change the client's own {@link FurnitureTunings} or {@link BackOffsets} at once, so the
 * piece or cosmetic updates without waiting, go to the server, which sends everyone the result, and are written to the
 * {@link TuneLog}.
 */
public final class DevSession {
    /** How far the bounding box deleter looks for a box, in blocks. */
    private static final double REACH = 8.0;

    private static @Nullable BlockPos target;
    private static TunerMode mode = TunerMode.SEATING;
    private static TuningParameter parameter = TuningParameter.MODE;
    private static int seat;
    private static BackParameter backParameter = BackParameter.Y;

    private DevSession() {}

    public static @Nullable BlockPos target() {
        return target;
    }

    public static TunerMode mode() {
        return mode;
    }

    public static TuningParameter parameter() {
        return parameter;
    }

    public static int seat() {
        return seat;
    }

    static void bind(BlockPos pos, FurnitureBlock block) {
        boolean changed = !pos.equals(target);
        target = pos.immutable();
        if (changed) {
            seat = 0;
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
        seat = 0;
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

    /** {@code block}'s live tuning, or its code values while it has none. */
    public static FurnitureTuning tuning(FurnitureBlock block) {
        return block.tuning();
    }

    /** Whether {@code block} has been changed from its code values. */
    public static boolean tuned(FurnitureBlock block) {
        return FurnitureTunings.get(block) != null;
    }

    static void setMode(TunerMode next) {
        mode = next;
        parameter = TuningParameter.MODE;
    }

    /** Selects the next shown parameter; on another piece only the mode's ones are shown. */
    static void selectParameter(int steps) {
        FurnitureBlock block = block();
        if (block == null) {
            return;
        }
        List<TuningParameter> shown = TuningParameter.shown(mode, block);
        int index = Math.max(0, shown.indexOf(parameter));
        parameter = shown.get(Math.floorMod(index + Integer.signum(steps), shown.size()));
    }

    /** Ctrl + scroll on the selected parameter. */
    static void adjust(FurnitureBlock block, int steps, boolean fine) {
        FurnitureTuning tuning = tuning(block);
        if (!TuningParameter.shown(mode, block).contains(parameter)) {
            parameter = TuningParameter.MODE;
        }
        switch (parameter) {
            case MODE -> setMode(mode.next(steps));
            case SEAT -> selectSeat(seat + steps, tuning);
            default -> {
                FurnitureTuning changed = parameter.adjust(tuning, seat, steps, fine);
                if (!changed.equals(tuning)) {
                    apply(block, changed);
                }
            }
        }
    }

    static void selectSeat(int index, FurnitureTuning tuning) {
        seat = tuning.seats().isEmpty() ? 0 : Math.floorMod(index, tuning.seats().size());
    }

    /**
     * The footprint box of the bound piece under the crosshair, nearest first: the exact box the player looks at (thin
     * and stray ones included), or {@code null} when the crosshair meets none within {@link #REACH}.
     */
    public static @Nullable TargetBox targetBox() {
        Minecraft minecraft = Minecraft.getInstance();
        FurnitureBlock block = block();
        BlockState state = state();
        Player player = minecraft.player;
        if (block == null || state == null || player == null || target == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(REACH));
        Footprint footprint = block.footprint(state);
        int turn = block.turn(state);
        List<AABB> boxes = footprint.boxes(turn);
        TargetBox best = null;
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < boxes.size(); i++) {
            AABB world = boxes.get(i).move(target);
            Optional<Vec3> hit = world.contains(eye) ? Optional.of(eye) : world.clip(eye, end);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < nearest) {
                nearest = hit.get().distanceToSqr(eye);
                boolean diagonal = footprint.diagonalAt(turn);
                AABB source = (diagonal ? footprint.diagonalBoxes() : footprint.boxes()).get(i);
                best = new TargetBox(world, new FurnitureTuning.RemovedBox(FurnitureTuning.Box.ofBlocks(source), diagonal));
            }
        }
        return best;
    }

    /** Deletes the box under the crosshair from the bound piece's hitbox. */
    static void deleteTargetBox(FurnitureBlock block) {
        TargetBox box = targetBox();
        if (box == null) {
            message(Component.translatable("message.apeira.dev.no_box"));
            return;
        }
        FurnitureTuning tuning = tuning(block);
        List<FurnitureTuning.RemovedBox> removed = new ArrayList<>(tuning.removed());
        removed.add(box.removed());
        apply(block, tuning.withRemoved(removed));
        message(Component.translatable("message.apeira.dev.box_deleted", removed.size()));
    }

    /** Changes the bound piece, sends the change to the server and logs it. */
    static void apply(FurnitureBlock block, FurnitureTuning tuning) {
        FurnitureTunings.set(block, tuning);
        selectSeat(seat, tuning);
        ApeiraClientNetwork.sendToServer(new FurnitureTuningPayload(BuiltInRegistries.BLOCK.getKey(block), Optional.of(tuning),
                Optional.ofNullable(target)));
        relight(block);
        TuneLog.record(block, tuning);
    }

    /** Gives the bound piece back its code values and takes it out of the log. */
    static void reset(FurnitureBlock block) {
        FurnitureTunings.set(block, null);
        seat = 0;
        ApeiraClientNetwork.sendToServer(new FurnitureTuningPayload(BuiltInRegistries.BLOCK.getKey(block), Optional.empty(),
                Optional.ofNullable(target)));
        relight(block);
        TuneLog.remove(block);
    }

    /** Relights the bound piece on the client, so a light change shows at once. */
    static void relight(FurnitureBlock block) {
        Minecraft minecraft = Minecraft.getInstance();
        if (target != null && minecraft.level != null && minecraft.level.getBlockState(target).is(block)) {
            minecraft.level.getChunkSource().getLightEngine().checkBlock(target);
        }
    }

    /** A footprint box under the crosshair: where it is, and how the tuning names it. */
    public record TargetBox(AABB world, FurnitureTuning.RemovedBox removed) {}

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
