package com.apeira.decor.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The blocks a multiblock piece covers and its shape in each of them, for every turn of the piece.
 *
 * <p>A footprint is made from boxes relative to the piece's own block (its controller), which may reach past that block
 * in any direction, like Create's large water wheels. For each of the eight 45 degree {@link #TURNS} the boxes are
 * turned about the controller's centre column (at the diagonals, each box becomes the box around its turned corners, as
 * {@code ApeiraFurnitureBlock} turns its shapes) and cut into the blocks they reach into: each of those blocks is a part
 * ({@link MultiblockPartBlock}) with the piece's shape inside it. A box has to reach at least a pixel into a block for
 * that block to be a part, so rounding and slivers don't claim neighbours. The controller's own block is always in the
 * footprint, with an empty shape when no box reaches into it.
 *
 * <p>Turn {@code t} turns a point {@code (x, z)} about the centre by {@code t * 45} degrees, from x towards z: turn 2
 * takes {@code (dx, dz)} to {@code (-dz, dx)}.
 */
public final class Footprint {
    /** Eight 45 degree turns. */
    public static final int TURNS = 8;
    /** One full block, no parts. */
    public static final Footprint BLOCK = of(List.of(new AABB(0, 0, 0, 1, 1, 1)));

    /** A box must reach this far (in blocks) into a block on every axis to make it a part: a pixel, less rounding. */
    private static final double MIN_PART = 0.99 / 16.0;

    private final List<AABB> boxes;
    private final List<Map<BlockPos, VoxelShape>> parts;

    private Footprint(List<AABB> boxes) {
        this.boxes = List.copyOf(boxes);
        List<Map<BlockPos, VoxelShape>> turns = new ArrayList<>(TURNS);
        for (int turn = 0; turn < TURNS; turn++) {
            turns.add(cut(boxes, turn));
        }
        this.parts = List.copyOf(turns);
    }

    /** A footprint of {@code boxes}, in blocks relative to the controller's block, at turn 0. */
    public static Footprint of(List<AABB> boxes) {
        return new Footprint(boxes);
    }

    /**
     * Reads boxes in pixels relative to the controller's block (0-16 is that block), each {@code x1 y1 z1 x2 y2 z2},
     * separated by commas: the format of the footprint tables.
     */
    public static Footprint parse(String pixels) {
        List<AABB> boxes = new ArrayList<>();
        for (String box : pixels.split(",")) {
            String[] values = box.trim().split("\\s+");
            if (values.length != 6) {
                throw new IllegalArgumentException("A footprint box needs six numbers: " + box);
            }
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) {
                v[i] = Double.parseDouble(values[i]) / 16.0;
            }
            boxes.add(new AABB(v[0], v[1], v[2], v[3], v[4], v[5]));
        }
        return of(boxes);
    }

    /** The boxes, in blocks, at turn 0. */
    public List<AABB> boxes() {
        return boxes;
    }

    /**
     * Every block the piece covers at {@code turn}, as an offset from the controller (the controller itself is
     * {@link BlockPos#ZERO}), with the piece's shape in that block, in the block's own coordinates.
     */
    public Map<BlockPos, VoxelShape> parts(int turn) {
        return parts.get(Math.floorMod(turn, TURNS));
    }

    /** The piece's shape in the block at {@code offset} from the controller, or an empty shape outside the footprint. */
    public VoxelShape shape(int turn, BlockPos offset) {
        return parts(turn).getOrDefault(offset, Shapes.empty());
    }

    /** Whether the piece covers only its own block at every turn. */
    public boolean single() {
        return parts.stream().allMatch(map -> map.size() == 1);
    }

    private static Map<BlockPos, VoxelShape> cut(List<AABB> boxes, int turn) {
        double angle = Math.toRadians(turn * 45.0);
        double cos = round(Math.cos(angle));
        double sin = round(Math.sin(angle));
        Map<BlockPos, VoxelShape> parts = new LinkedHashMap<>();
        parts.put(BlockPos.ZERO, Shapes.empty());
        for (AABB box : boxes) {
            AABB turned = turned(box, cos, sin);
            for (int x = (int) Math.floor(turned.minX); x < Math.ceil(turned.maxX); x++) {
                for (int y = (int) Math.floor(turned.minY); y < Math.ceil(turned.maxY); y++) {
                    for (int z = (int) Math.floor(turned.minZ); z < Math.ceil(turned.maxZ); z++) {
                        AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        if (!turned.intersects(cell)) {
                            continue;
                        }
                        AABB inside = turned.intersect(cell);
                        if (inside.getXsize() < MIN_PART || inside.getYsize() < MIN_PART || inside.getZsize() < MIN_PART) {
                            continue;
                        }
                        BlockPos offset = new BlockPos(x, y, z);
                        if (!MultiblockPartBlock.reaches(offset)) {
                            continue;
                        }
                        VoxelShape shape = Shapes.create(inside.move(-x, -y, -z));
                        parts.merge(offset, shape, Shapes::or);
                    }
                }
            }
        }
        parts.replaceAll((offset, shape) -> shape.optimize());
        return Collections.unmodifiableMap(parts);
    }

    /** The box around {@code box} turned about the controller's centre column. */
    private static AABB turned(AABB box, double cos, double sin) {
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double x : new double[] {box.minX, box.maxX}) {
            for (double z : new double[] {box.minZ, box.maxZ}) {
                double dx = x - 0.5;
                double dz = z - 0.5;
                double turnedX = round(0.5 + dx * cos - dz * sin);
                double turnedZ = round(0.5 + dx * sin + dz * cos);
                minX = Math.min(minX, turnedX);
                maxX = Math.max(maxX, turnedX);
                minZ = Math.min(minZ, turnedZ);
                maxZ = Math.max(maxZ, turnedZ);
            }
        }
        return new AABB(minX, box.minY, minZ, maxX, box.maxY, maxZ);
    }

    /** Drops the floating point noise of turning, so turned boxes line up with the blocks. */
    private static double round(double value) {
        return Math.round(value * 1.0E9) / 1.0E9;
    }
}
