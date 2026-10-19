package com.apeira.decor.furniture;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One furniture piece of {@link FurnitureCatalogue}: its id, how it is placed, painted and sat on, and its shape.
 * {@link com.apeira.decor.registry.DecorFurniture} registers a {@link FurnitureBlock} for each.
 *
 * <p>Its block model is {@code models/block/furniture/<name>.json} and its item model
 * {@code models/item/furniture/<name>.json}, both converted from the legacy model and facing north. Placed pieces are
 * drawn by {@code FurnitureRenderer}, as CosmoLib drew its furniture: its item definition shows the block model in the
 * {@code none} display context, or for a {@linkplain #large() large} piece, which reaches further than a block model
 * may (-16 to 32 pixels), {@code models/item/furniture/<name>_placed.json} at full size.
 *
 * @param name         the block's id path
 * @param crate        the crate it is exclusive to ({@code tooltip.apeira.exclusive.<crate>}), or {@code null}
 * @param paintable    whether the paintbrush and painting table paint it; paint shows on the model's
 *                     {@code tintindex} 0 faces
 * @param defaultPaint the paint a new item has, or {@code null}
 * @param collision    whether it has a collision shape (a rug has none)
 * @param seatHeight   the top of its seat in pixels, or a negative number if it has no seat
 * @param large        how {@code FurnitureRenderer} draws its {@code _placed} model, or {@code null} to draw its block model
 * @param shape        its shape facing north
 */
public record Furniture(String name, @Nullable String crate, boolean paintable, @Nullable Integer defaultPaint, boolean collision,
        double seatHeight, @Nullable Large large, VoxelShape shape) {

    /** Whether a player can sit on it. */
    public boolean seat() {
        return seatHeight >= 0;
    }

    /**
     * How a large piece is drawn: its {@code _placed} model, in the legacy model's units, scaled by {@code scale} about
     * the centre of the block, with its bottom ({@code modelBottom}, in pixels) moved to {@code bottom} pixels up.
     */
    public record Large(float scale, float modelBottom, float bottom) {
        /** A piece drawn with its block model, which is already at its size and stands on the floor. */
        public static final Large BLOCK_MODEL = new Large(1.0F, 0.0F, 0.0F);
    }

    /** Starts a piece with a full-block shape, unpaintable, solid and without a seat. */
    static Builder piece(String name, @Nullable String crate) {
        return new Builder(name, crate);
    }

    /** Builds a {@link Furniture}; {@link #shape} finishes it. */
    static final class Builder {
        private final String name;
        private final @Nullable String crate;
        private boolean paintable;
        private @Nullable Integer defaultPaint;
        private boolean collision = true;
        private double seatHeight = -1;
        private @Nullable Large large;

        private Builder(String name, @Nullable String crate) {
            this.name = name;
            this.crate = crate;
        }

        Builder paintable() {
            paintable = true;
            return this;
        }

        Builder defaultPaint(int paint) {
            defaultPaint = paint;
            return this;
        }

        Builder noCollision() {
            collision = false;
            return this;
        }

        Builder seat(double heightInPixels) {
            seatHeight = heightInPixels;
            return this;
        }

        Builder large(double scale, double modelBottom, double bottom) {
            large = new Large((float) scale, (float) modelBottom, (float) bottom);
            return this;
        }

        /** Finishes the piece with a shape of boxes given as {@code x1, y1, z1, x2, y2, z2} in pixels, six per box. */
        Furniture shape(double... boxes) {
            if (boxes.length == 0 || boxes.length % 6 != 0) {
                throw new IllegalArgumentException(name + ": a shape needs six numbers per box");
            }
            VoxelShape shape = Shapes.empty();
            for (int i = 0; i < boxes.length; i += 6) {
                shape = Shapes.or(shape, Block.box(boxes[i], boxes[i + 1], boxes[i + 2], boxes[i + 3], boxes[i + 4], boxes[i + 5]));
            }
            return new Furniture(name, crate, paintable, defaultPaint, collision, seatHeight, large, shape.optimize());
        }
    }

    /** Every piece, in catalogue order. */
    public static List<Furniture> all() {
        return FurnitureCatalogue.all();
    }
}
