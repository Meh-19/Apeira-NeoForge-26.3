package com.apeira.decor.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The blocks a multiblock piece covers and its shape in each of them, for every turn of the piece.
 *
 * <p>A footprint is made from boxes relative to the piece's own block (its controller), which may reach past that block
 * in any direction, like Create's large water wheels. It has its boxes at turn 0 (straight) and, optionally, at turn 1
 * (diagonal, 45 degrees): the other {@link #TURNS} are these turned by exact quarter turns about the controller's centre
 * column, so the boxes stay where the piece's cubes are at every turn. A footprint without diagonal boxes turns its
 * straight boxes at the odd turns too, each becoming the box around its turned corners, as {@code ApeiraFurnitureBlock}
 * turns its shapes. Each turn's boxes are cut into the blocks they reach into: each of those blocks is a part
 * ({@link MultiblockPartBlock}) with the piece's shape inside it. A box has to reach at least a pixel into a block for
 * that block to be a part, so rounding and slivers don't claim neighbours. The controller's own block is always in the
 * footprint, with an empty shape when no box reaches into it.
 *
 * <p>Turn {@code t} turns a point {@code (x, z)} about the centre by {@code t * 45} degrees, from x towards z: turn 2
 * takes {@code (dx, dz)} to {@code (-dz, dx)}.
 *
 * <p>Turns are worked out when first asked for and kept, so only the turns pieces are placed at cost anything.
 */
public final class Footprint {
    /** Eight 45 degree turns. */
    public static final int TURNS = 8;
    /** One full block, no parts. */
    public static final Footprint BLOCK = of(List.of(new AABB(0, 0, 0, 1, 1, 1)));

    /** A box must reach this far (in blocks) into a block on every axis to make it a part: a pixel, less rounding. */
    private static final double MIN_PART = 0.99 / 16.0;
    private static final double PIXEL = 1.0 / 16.0;

    private final List<AABB> straight;
    private final @Nullable List<AABB> diagonal;
    private final @Nullable List<double[]> edges;
    private final Turn[] turns = new Turn[TURNS];

    private Footprint(List<AABB> straight, @Nullable List<AABB> diagonal, @Nullable List<double[]> edges) {
        this.straight = List.copyOf(straight);
        this.diagonal = diagonal == null || diagonal.isEmpty() ? null : List.copyOf(diagonal);
        this.edges = edges == null || edges.isEmpty() ? null : List.copyOf(edges);
    }

    /** A footprint of {@code boxes}, in blocks relative to the controller's block, at turn 0. */
    public static Footprint of(List<AABB> boxes) {
        return new Footprint(boxes, null, null);
    }

    /**
     * A footprint as {@link #of(List, List)}, outlined by {@code edges} (lines of six numbers in blocks, at turn 0,
     * turned with the piece) instead of by its boxes: the model's own edges, so the outline matches what is drawn.
     */
    public static Footprint of(List<AABB> straight, @Nullable List<AABB> diagonal, @Nullable List<double[]> edges) {
        return new Footprint(straight, diagonal, edges);
    }

    /** Reads lines in pixels, each {@code x1 y1 z1 x2 y2 z2}, separated by commas, as blocks. */
    public static List<double[]> parseLines(String pixels) {
        List<double[]> lines = new ArrayList<>();
        for (String line : pixels.split(",")) {
            if (line.isBlank()) {
                continue;
            }
            String[] values = line.trim().split("\\s+");
            if (values.length != 6) {
                throw new IllegalArgumentException("An outline line needs six numbers: " + line);
            }
            double[] v = new double[6];
            for (int i = 0; i < 6; i++) {
                v[i] = Double.parseDouble(values[i]) / 16.0;
            }
            lines.add(v);
        }
        return lines;
    }

    /**
     * A footprint of {@code straight} boxes at turn 0 and {@code diagonal} boxes at turn 1, in blocks relative to the
     * controller's block; with no diagonal boxes, the straight ones are turned.
     */
    public static Footprint of(List<AABB> straight, @Nullable List<AABB> diagonal) {
        return new Footprint(straight, diagonal, null);
    }

    /**
     * Reads boxes in pixels relative to the controller's block (0-16 is that block), each {@code x1 y1 z1 x2 y2 z2},
     * separated by commas: the format of the footprint tables. An empty text is no boxes.
     */
    public static List<AABB> parse(String pixels) {
        List<AABB> boxes = new ArrayList<>();
        for (String box : pixels.split(",")) {
            if (box.isBlank()) {
                continue;
            }
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
        return boxes;
    }

    /** The boxes, in blocks, at turn 0. */
    public List<AABB> boxes() {
        return straight;
    }

    /**
     * Every block the piece covers at {@code turn}, as an offset from the controller (the controller itself is
     * {@link BlockPos#ZERO}), with the piece's shape in that block, in the block's own coordinates.
     */
    public Map<BlockPos, VoxelShape> parts(int turn) {
        return turn(turn).parts;
    }

    /** The piece's shape in the block at {@code offset} from the controller, or an empty shape outside the footprint. */
    public VoxelShape shape(int turn, BlockPos offset) {
        return parts(turn).getOrDefault(offset, Shapes.empty());
    }

    /** The box around the whole piece at {@code turn}, in blocks relative to the controller's block. */
    public AABB bounds(int turn) {
        return turn(turn).bounds;
    }

    /**
     * The outline of the whole piece at {@code turn}, as lines of six numbers ({@code x1 y1 z1 x2 y2 z2}, in blocks
     * relative to the controller's block): the model's own edges when the footprint has them (turned exactly, so the
     * outline matches the model at every turn), else the edges of its boxes' outside, worked out on the pixel grid,
     * without the seams between boxes or between the blocks it covers.
     */
    public List<double[]> outline(int turn) {
        Turn known = turn(turn);
        List<double[]> lines = known.outline;
        if (lines == null) {
            lines = edges != null ? turnedLines(edges, Math.floorMod(turn, TURNS)) : outline(known.boxes);
            known.outline = lines;
        }
        return lines;
    }

    /** Whether the piece covers only its own block at every turn: every box, straight or diagonal, stays inside it. */
    public boolean single() {
        return inside(straight, diagonal == null) && (diagonal == null || inside(diagonal, false));
    }

    /** Whether every box stays in the block; {@code turned} boxes must also stay in it turned to the diagonals. */
    private static boolean inside(List<AABB> boxes, boolean turned) {
        for (AABB box : boxes) {
            if (box.minY < -MIN_PART || box.maxY > 1 + MIN_PART) {
                return false;
            }
            for (double x : new double[] {box.minX, box.maxX}) {
                for (double z : new double[] {box.minZ, box.maxZ}) {
                    double dx = Math.abs(x - 0.5);
                    double dz = Math.abs(z - 0.5);
                    if ((turned ? Math.hypot(dx, dz) : Math.max(dx, dz)) > 0.5 + MIN_PART) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private Turn turn(int turn) {
        int t = Math.floorMod(turn, TURNS);
        Turn known = turns[t];
        if (known == null) {
            // The straight boxes at even turns; the diagonal ones, when there are any, turned by t - 1 at odd turns.
            List<AABB> boxes = diagonal != null && t % 2 == 1 ? turned(diagonal, t - 1) : turned(straight, t);
            known = new Turn(boxes, cut(boxes), bounds(boxes));
            turns[t] = known;
        }
        return known;
    }

    private static List<AABB> turned(List<AABB> boxes, int turn) {
        double angle = Math.toRadians(turn * 45.0);
        double cos = round(Math.cos(angle));
        double sin = round(Math.sin(angle));
        List<AABB> turned = new ArrayList<>(boxes.size());
        for (AABB box : boxes) {
            turned.add(turned(box, cos, sin));
        }
        return turned;
    }

    private static Map<BlockPos, VoxelShape> cut(List<AABB> boxes) {
        Map<BlockPos, List<VoxelShape>> pieces = new LinkedHashMap<>();
        pieces.put(BlockPos.ZERO, new ArrayList<>());
        for (AABB box : boxes) {
            for (int x = (int) Math.floor(box.minX); x < Math.ceil(box.maxX); x++) {
                for (int y = (int) Math.floor(box.minY); y < Math.ceil(box.maxY); y++) {
                    for (int z = (int) Math.floor(box.minZ); z < Math.ceil(box.maxZ); z++) {
                        AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        if (!box.intersects(cell)) {
                            continue;
                        }
                        AABB inside = box.intersect(cell);
                        if (inside.getXsize() < MIN_PART || inside.getYsize() < MIN_PART || inside.getZsize() < MIN_PART) {
                            continue;
                        }
                        BlockPos offset = new BlockPos(x, y, z);
                        if (!MultiblockPartBlock.reaches(offset)) {
                            continue;
                        }
                        pieces.computeIfAbsent(offset, key -> new ArrayList<>()).add(Shapes.create(inside.move(-x, -y, -z)));
                    }
                }
            }
        }
        Map<BlockPos, VoxelShape> parts = new LinkedHashMap<>();
        pieces.forEach((offset, shapes) -> parts.put(offset, union(shapes, 0, shapes.size()).optimize()));
        return Collections.unmodifiableMap(parts);
    }

    /**
     * The union of {@code shapes[from, to)}, joined in halves and left unoptimized until the end, as joining pixel boxes
     * of one block keeps to its 16 pixel grid.
     */
    private static VoxelShape union(List<VoxelShape> shapes, int from, int to) {
        if (to - from == 0) {
            return Shapes.empty();
        }
        if (to - from == 1) {
            return shapes.get(from);
        }
        int middle = (from + to) >>> 1;
        return Shapes.joinUnoptimized(union(shapes, from, middle), union(shapes, middle, to), BooleanOp.OR);
    }

    private static AABB bounds(List<AABB> boxes) {
        AABB bounds = new AABB(0, 0, 0, 1, 1, 1);
        for (AABB box : boxes) {
            bounds = bounds.minmax(box);
        }
        return bounds;
    }

    /**
     * The outline of the boxes' union on the pixel grid: an edge of a pixel is drawn where the four pixels around it are
     * not all alike and not a flat face (one or three filled, or two across from each other), and runs of drawn edges
     * along one line are joined into one line.
     */
    private static List<double[]> outline(List<AABB> boxes) {
        Set<Long> filled = new HashSet<>();
        for (AABB box : boxes) {
            int x1 = pixel(box.minX), y1 = pixel(box.minY), z1 = pixel(box.minZ);
            int x2 = pixel(box.maxX), y2 = pixel(box.maxY), z2 = pixel(box.maxZ);
            for (int x = x1; x < x2; x++) {
                for (int y = y1; y < y2; y++) {
                    for (int z = z1; z < z2; z++) {
                        filled.add(key(x, y, z));
                    }
                }
            }
        }
        // Edges along each axis, keyed by the line they lie on, holding the steps along it.
        Map<List<Integer>, List<Integer>> runs = new HashMap<>();
        Set<Long> seen = new HashSet<>();
        for (long voxel : filled) {
            int vx = unpack(voxel, 0), vy = unpack(voxel, 1), vz = unpack(voxel, 2);
            for (int axis = 0; axis < 3; axis++) {
                // The pixel's four edges along this axis, at its corners across the other two axes.
                for (int du = 0; du <= 1; du++) {
                    for (int dv = 0; dv <= 1; dv++) {
                        int[] corner = {vx, vy, vz};
                        int u = (axis + 1) % 3;
                        int v = (axis + 2) % 3;
                        corner[u] += du;
                        corner[v] += dv;
                        long edge = key(corner[0], corner[1], corner[2]) << 2 | axis;
                        if (!seen.add(edge) || !crease(filled, corner, u, v)) {
                            continue;
                        }
                        runs.computeIfAbsent(List.of(axis, corner[u], corner[v]), line -> new ArrayList<>()).add(corner[axis]);
                    }
                }
            }
        }
        List<double[]> lines = new ArrayList<>();
        runs.forEach((line, steps) -> {
            Collections.sort(steps);
            int axis = line.get(0);
            int start = steps.get(0);
            int end = start + 1;
            for (int i = 1; i <= steps.size(); i++) {
                if (i < steps.size() && steps.get(i) == end) {
                    end++;
                    continue;
                }
                lines.add(line(axis, line.get(1), line.get(2), start, end));
                if (i < steps.size()) {
                    start = steps.get(i);
                    end = start + 1;
                }
            }
        });
        return List.copyOf(lines);
    }

    /** Whether the edge at {@code corner} (the low end of a pixel's edge) between the pixels around it is drawn. */
    private static boolean crease(Set<Long> filled, int[] corner, int u, int v) {
        boolean[] around = new boolean[4];
        int count = 0;
        for (int i = 0; i < 4; i++) {
            int[] at = corner.clone();
            at[u] -= 1 - (i & 1);
            at[v] -= 1 - (i >> 1);
            around[i] = filled.contains(key(at[0], at[1], at[2]));
            if (around[i]) {
                count++;
            }
        }
        // Two filled across from each other (0 and 3, or 1 and 2) meet only at the edge.
        return count == 1 || count == 3 || count == 2 && around[0] == around[3];
    }

    private static double[] line(int axis, int u, int v, int start, int end) {
        double[] from = new double[3];
        double[] to = new double[3];
        from[axis] = start * PIXEL;
        to[axis] = end * PIXEL;
        from[(axis + 1) % 3] = to[(axis + 1) % 3] = u * PIXEL;
        from[(axis + 2) % 3] = to[(axis + 2) % 3] = v * PIXEL;
        return new double[] {from[0], from[1], from[2], to[0], to[1], to[2]};
    }

    private static int pixel(double blocks) {
        return (int) Math.round(blocks * 16.0);
    }

    /** A pixel (or a corner) as one number: 20 bits an axis, offset so negative positions fit. */
    private static long key(int x, int y, int z) {
        return ((long) (x + (1 << 19)) << 40) | ((long) (y + (1 << 19)) << 20) | (z + (1 << 19));
    }

    private static int unpack(long key, int axis) {
        return (int) ((key >> (40 - 20 * axis)) & ((1 << 20) - 1)) - (1 << 19);
    }

    /** {@code lines} turned about the controller's centre column by {@code turn} 45 degree steps. */
    private static List<double[]> turnedLines(List<double[]> lines, int turn) {
        double angle = Math.toRadians(turn * 45.0);
        double cos = round(Math.cos(angle));
        double sin = round(Math.sin(angle));
        List<double[]> turned = new ArrayList<>(lines.size());
        for (double[] line : lines) {
            double[] out = line.clone();
            for (int end = 0; end < 6; end += 3) {
                double dx = line[end] - 0.5;
                double dz = line[end + 2] - 0.5;
                out[end] = 0.5 + dx * cos - dz * sin;
                out[end + 2] = 0.5 + dx * sin + dz * cos;
            }
            turned.add(out);
        }
        return List.copyOf(turned);
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

    /** One turn's boxes, parts, bounds and (once drawn) outline. */
    private static final class Turn {
        final List<AABB> boxes;
        final Map<BlockPos, VoxelShape> parts;
        final AABB bounds;
        volatile @Nullable List<double[]> outline;

        Turn(List<AABB> boxes, Map<BlockPos, VoxelShape> parts, AABB bounds) {
            this.boxes = boxes;
            this.parts = parts;
            this.bounds = bounds;
        }
    }
}
