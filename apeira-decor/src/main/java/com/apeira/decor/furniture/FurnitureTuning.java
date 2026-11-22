package com.apeira.decor.furniture;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What apeira-dev's furniture tuner changes on a piece, in its three modes: the seats (seating adjuster), the footprint
 * boxes taken out of its hitbox (bounding box deleter), and its light level and collision (light and collision toggle).
 * Lengths are in pixels at turn 0, as the footprint tables have them. The same values, applied to code, are the baked
 * tunes of {@link FurnitureTunes}; live ones are {@link FurnitureTunings}.
 *
 * @param removed   footprint boxes taken out, each as the table has it at turn 0 (or turn 1 for a diagonal box)
 * @param seats     the seats ({@code x, y, z}, y the sit height), or an empty list for the piece's own; only seat
 *                  pieces ({@link SeatFurnitureBlock}) use them
 * @param light     the light the piece gives, 0-15, or {@link #OWN_LIGHT} for its own
 * @param collision whether the piece (and its parts) stops entities
 */
public record FurnitureTuning(List<RemovedBox> removed, List<Seat> seats, int light, boolean collision) {
    /** The most boxes and seats a client may give a piece. */
    public static final int MAX_ENTRIES = 64;
    /** {@link #light} for a piece that gives its own light. */
    public static final int OWN_LIGHT = -1;
    /** Seats stay within the reach of the multiblock around the block (pixels). */
    private static final double SEAT_REACH = 64.0;

    public static final StreamCodec<ByteBuf, FurnitureTuning> STREAM_CODEC = StreamCodec.composite(
            RemovedBox.STREAM_CODEC.apply(ByteBufCodecs.list()), FurnitureTuning::removed,
            Seat.STREAM_CODEC.apply(ByteBufCodecs.list()), FurnitureTuning::seats,
            ByteBufCodecs.VAR_INT, FurnitureTuning::light,
            ByteBufCodecs.BOOL, FurnitureTuning::collision,
            FurnitureTuning::new);

    public FurnitureTuning {
        removed = List.copyOf(removed);
        seats = List.copyOf(seats);
    }

    /** {@code block} as its code has it (with its baked tune): what the tuner starts from. */
    public static FurnitureTuning of(FurnitureBlock block) {
        FurnitureTuning baked = FurnitureTunes.get(block);
        List<Seat> seats = baked != null && !baked.seats.isEmpty() ? baked.seats : ownSeats(block);
        return new FurnitureTuning(baked != null ? baked.removed : List.of(), seats, baked != null ? baked.light : OWN_LIGHT,
                baked != null ? baked.collision : block.ownCollision());
    }

    /** A seat piece's seats in pixels at turn 0, as it is registered; none for other pieces. */
    private static List<Seat> ownSeats(FurnitureBlock block) {
        if (!(block instanceof SeatFurnitureBlock seat)) {
            return List.of();
        }
        List<Seat> seats = new ArrayList<>();
        // Turn 0 is ROTATION 0 for every piece that sits (no seat is a wall piece).
        for (Vec3 position : seat.ownSeats(block.defaultBlockState())) {
            seats.add(new Seat(round(position.x * 16.0), round(position.y * 16.0), round(position.z * 16.0)));
        }
        return seats;
    }

    /** This tuning with the values a client may send kept in range. */
    public FurnitureTuning sanitized() {
        List<Seat> kept = new ArrayList<>();
        for (Seat seat : seats.subList(0, Math.min(seats.size(), MAX_ENTRIES))) {
            kept.add(seat.clamped());
        }
        return new FurnitureTuning(removed.subList(0, Math.min(removed.size(), MAX_ENTRIES)), kept, Mth.clamp(light, OWN_LIGHT, 15), collision);
    }

    /** Whether {@code box} (turn 0, or turn 1 when {@code diagonal}) is one this tuning takes out. */
    public boolean removes(AABB box, boolean diagonal) {
        for (RemovedBox removedBox : removed) {
            if (removedBox.diagonal == diagonal && removedBox.box.matches(box)) {
                return true;
            }
        }
        return false;
    }

    public FurnitureTuning withRemoved(List<RemovedBox> removed) {
        return new FurnitureTuning(removed, seats, light, collision);
    }

    public FurnitureTuning withSeats(List<Seat> seats) {
        return new FurnitureTuning(removed, seats, light, collision);
    }

    public FurnitureTuning withSeat(int index, Seat seat) {
        List<Seat> changed = new ArrayList<>(seats);
        changed.set(index, seat);
        return withSeats(changed);
    }

    public FurnitureTuning withLight(int light) {
        return new FurnitureTuning(removed, seats, Mth.clamp(light, OWN_LIGHT, 15), collision);
    }

    public FurnitureTuning withCollision(boolean collision) {
        return new FurnitureTuning(removed, seats, light, collision);
    }

    /** The tuning as the tune log and the baked tunes file write it. */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray straight = new JsonArray();
        JsonArray diagonal = new JsonArray();
        for (RemovedBox box : removed) {
            (box.diagonal ? diagonal : straight).add(box.box.toJson());
        }
        json.add("removed_boxes", straight);
        json.add("removed_diagonal_boxes", diagonal);
        JsonArray seatList = new JsonArray();
        for (Seat seat : seats) {
            JsonArray numbers = new JsonArray();
            numbers.add(seat.x);
            numbers.add(seat.y);
            numbers.add(seat.z);
            seatList.add(numbers);
        }
        json.add("seats", seatList);
        json.addProperty("light", light);
        json.addProperty("collision", collision);
        return json;
    }

    /**
     * Reads {@link #toJson()}'s format; missing fields are {@code block}'s own (no removed boxes, its seats, its light
     * and collision), so a baked entry may name only what it changes.
     */
    public static FurnitureTuning fromJson(JsonObject json, FurnitureBlock block) {
        List<RemovedBox> removed = new ArrayList<>();
        for (String key : new String[] {"removed_boxes", "removed_diagonal_boxes"}) {
            if (json.has(key)) {
                for (JsonElement element : json.getAsJsonArray(key)) {
                    removed.add(new RemovedBox(Box.fromJson(element.getAsJsonArray()), key.contains("diagonal")));
                }
            }
        }
        List<Seat> seats = new ArrayList<>();
        if (json.has("seats")) {
            for (JsonElement element : json.getAsJsonArray("seats")) {
                JsonArray numbers = element.getAsJsonArray();
                seats.add(new Seat(numbers.get(0).getAsDouble(), numbers.get(1).getAsDouble(), numbers.get(2).getAsDouble()));
            }
        }
        int light = json.has("light") ? json.get("light").getAsInt() : OWN_LIGHT;
        boolean collision = json.has("collision") ? json.get("collision").getAsBoolean() : block.ownCollision();
        return new FurnitureTuning(removed, seats, light, collision).sanitized();
    }

    private static double round(double pixels) {
        return Math.round(pixels * 100.0) / 100.0;
    }

    /** One box of a footprint in pixels, relative to the controller's block, at turn 0 (or 1 for diagonal boxes). */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public static final StreamCodec<ByteBuf, Box> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Box::minX,
                ByteBufCodecs.DOUBLE, Box::minY,
                ByteBufCodecs.DOUBLE, Box::minZ,
                ByteBufCodecs.DOUBLE, Box::maxX,
                ByteBufCodecs.DOUBLE, Box::maxY,
                ByteBufCodecs.DOUBLE, Box::maxZ,
                Box::new);

        /** A box given in blocks, rounded to a hundredth of a pixel. */
        public static Box ofBlocks(AABB box) {
            return new Box(round(box.minX * 16.0), round(box.minY * 16.0), round(box.minZ * 16.0), round(box.maxX * 16.0),
                    round(box.maxY * 16.0), round(box.maxZ * 16.0));
        }

        /** Whether {@code box} (in blocks) is this box, to a hundredth of a pixel. */
        public boolean matches(AABB box) {
            return equals(ofBlocks(box));
        }

        JsonArray toJson() {
            JsonArray numbers = new JsonArray();
            for (double value : new double[] {minX, minY, minZ, maxX, maxY, maxZ}) {
                numbers.add(value);
            }
            return numbers;
        }

        static Box fromJson(JsonArray numbers) {
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) {
                v[i] = numbers.get(i).getAsDouble();
            }
            return new Box(v[0], v[1], v[2], v[3], v[4], v[5]);
        }
    }

    /** A footprint box taken out of the hitbox; {@code diagonal} for one of the turn 1 boxes. */
    public record RemovedBox(Box box, boolean diagonal) {
        public static final StreamCodec<ByteBuf, RemovedBox> STREAM_CODEC = StreamCodec.composite(
                Box.STREAM_CODEC, RemovedBox::box,
                ByteBufCodecs.BOOL, RemovedBox::diagonal,
                RemovedBox::new);
    }

    /** A seat in pixels from the block's bottom north-west corner at turn 0; {@code y} is the sit height. */
    public record Seat(double x, double y, double z) {
        public static final StreamCodec<ByteBuf, Seat> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Seat::x,
                ByteBufCodecs.DOUBLE, Seat::y,
                ByteBufCodecs.DOUBLE, Seat::z,
                Seat::new);

        Seat clamped() {
            return new Seat(Mth.clamp(x, -SEAT_REACH, 16 + SEAT_REACH), Mth.clamp(y, 0, SEAT_REACH), Mth.clamp(z, -SEAT_REACH, 16 + SEAT_REACH));
        }

        /** The seat in blocks. */
        public Vec3 blocks() {
            return new Vec3(x / 16.0, y / 16.0, z / 16.0);
        }
    }
}
