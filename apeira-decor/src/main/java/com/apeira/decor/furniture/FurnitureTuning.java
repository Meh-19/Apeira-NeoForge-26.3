package com.apeira.decor.furniture;

import java.util.ArrayList;
import java.util.List;

import com.apeira.decor.block.ApeiraFurnitureBlock;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A furniture piece's shape, drawing transform, placement and seat, as a dev tool edits them live
 * ({@link FurnitureTunings}). All lengths are in pixels and face north, as in {@link FurnitureCatalogue}.
 *
 * @param boxes      the shape's boxes
 * @param scale      the scale {@code FurnitureRenderer} draws the model at ({@link Furniture.Large#scale()})
 * @param offsetX    how far the model is moved east, before it is turned with the piece
 * @param offsetY    how far the model's bottom is lifted ({@link Furniture.Large#bottom()})
 * @param offsetZ    how far the model is moved south, before it is turned with the piece
 * @param rotation   the 45 degree steps added to the placing rotation, or the rotation itself with
 *                   {@link FurniturePlacement#DEFAULT}
 * @param placements the {@link FurniturePlacement} bit mask
 * @param seatHeight the top of the seat, or a negative number for a piece without one
 */
public record FurnitureTuning(List<Box> boxes, float scale, float offsetX, float offsetY, float offsetZ, int rotation, int placements,
        double seatHeight) {
    /** The most boxes a client may give a piece; the catalogue's shapes come to fewer. */
    public static final int MAX_BOXES = 64;
    /** Boxes stay in the block across (shapes are cut to it when turned) and at most a fence's height up. */
    public static final double MAX_HEIGHT = 24.0;

    public static final StreamCodec<ByteBuf, FurnitureTuning> STREAM_CODEC = StreamCodec.composite(
            Box.STREAM_CODEC.apply(ByteBufCodecs.list()), FurnitureTuning::boxes,
            ByteBufCodecs.FLOAT, FurnitureTuning::scale,
            ByteBufCodecs.FLOAT, FurnitureTuning::offsetX,
            ByteBufCodecs.FLOAT, FurnitureTuning::offsetY,
            ByteBufCodecs.FLOAT, FurnitureTuning::offsetZ,
            ByteBufCodecs.VAR_INT, FurnitureTuning::rotation,
            ByteBufCodecs.VAR_INT, FurnitureTuning::placements,
            ByteBufCodecs.DOUBLE, FurnitureTuning::seatHeight,
            FurnitureTuning::new);

    public FurnitureTuning {
        boxes = List.copyOf(boxes);
    }

    /** The piece as the catalogue has it. */
    public static FurnitureTuning of(Furniture furniture) {
        List<Box> boxes = new ArrayList<>();
        for (AABB box : furniture.shape().toAabbs()) {
            boxes.add(Box.ofBlocks(box));
        }
        Furniture.Large large = furniture.large() != null ? furniture.large() : Furniture.Large.BLOCK_MODEL;
        return new FurnitureTuning(boxes, large.scale(), 0.0F, large.bottom(), 0.0F, 0, FurniturePlacement.ANY_FACE, furniture.seatHeight());
    }

    /** This tuning with the values a client may send kept in range. */
    public FurnitureTuning sanitized() {
        List<Box> kept = new ArrayList<>();
        for (Box box : boxes.subList(0, Math.min(boxes.size(), MAX_BOXES))) {
            Box clamped = box.clamped();
            if (clamped != null) {
                kept.add(clamped);
            }
        }
        int mask = placements & (FurniturePlacement.ANY_FACE | FurniturePlacement.DEFAULT.bit());
        return new FurnitureTuning(kept, Mth.clamp(scale, 0.05F, 16.0F), Mth.clamp(offsetX, -64.0F, 64.0F), Mth.clamp(offsetY, -64.0F, 64.0F),
                Mth.clamp(offsetZ, -64.0F, 64.0F), Math.floorMod(rotation, ApeiraFurnitureBlock.ROTATIONS), mask == 0 ? FurniturePlacement.FLOOR.bit() : mask,
                seatHeight < 0 ? -1.0 : Math.min(seatHeight, MAX_HEIGHT));
    }

    /** The shape facing north; an empty shape when every box was removed. */
    public VoxelShape shape() {
        VoxelShape shape = Shapes.empty();
        for (Box box : boxes) {
            shape = Shapes.or(shape, box.shape());
        }
        return shape.optimize();
    }

    public boolean seat() {
        return seatHeight >= 0;
    }

    public FurnitureTuning withBoxes(List<Box> boxes) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, rotation, placements, seatHeight);
    }

    public FurnitureTuning withBox(int index, Box box) {
        List<Box> changed = new ArrayList<>(boxes);
        changed.set(index, box);
        return withBoxes(changed);
    }

    public FurnitureTuning withScale(float scale) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, rotation, placements, seatHeight);
    }

    public FurnitureTuning withOffset(float offsetX, float offsetY, float offsetZ) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, rotation, placements, seatHeight);
    }

    public FurnitureTuning withRotation(int rotation) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, Math.floorMod(rotation, ApeiraFurnitureBlock.ROTATIONS), placements,
                seatHeight);
    }

    public FurnitureTuning withPlacements(int placements) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, rotation, placements, seatHeight);
    }

    public FurnitureTuning withSeatHeight(double seatHeight) {
        return new FurnitureTuning(boxes, scale, offsetX, offsetY, offsetZ, rotation, placements, seatHeight);
    }

    /** One box of a shape, {@code Block.box} style: corners in pixels, facing north. */
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
            return new Box(pixels(box.minX), pixels(box.minY), pixels(box.minZ), pixels(box.maxX), pixels(box.maxY), pixels(box.maxZ));
        }

        private static double pixels(double blocks) {
            return Math.round(blocks * 1600.0) / 100.0;
        }

        public VoxelShape shape() {
            return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        /** The box in blocks. */
        public AABB aabb() {
            return new AABB(minX / 16.0, minY / 16.0, minZ / 16.0, maxX / 16.0, maxY / 16.0, maxZ / 16.0);
        }

        /** The box cut to the block (and {@link #MAX_HEIGHT}), or {@code null} if nothing is left. */
        public Box clamped() {
            Box box = new Box(Mth.clamp(minX, 0, 16), Mth.clamp(minY, 0, MAX_HEIGHT), Mth.clamp(minZ, 0, 16), Mth.clamp(maxX, 0, 16),
                    Mth.clamp(maxY, 0, MAX_HEIGHT), Mth.clamp(maxZ, 0, 16));
            return box.minX < box.maxX && box.minY < box.maxY && box.minZ < box.maxZ ? box : null;
        }
    }
}
