package com.apeira.dev.client;

import java.util.List;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the bound piece's tuning in the world, through anything in front of it: the shape's boxes as it is turned
 * (white), the selected box (yellow), the seat (green point) and the model's axes from its origin, turned with the
 * piece: X red, Y green, Z blue, and the front, where a sitter looks out (north before turning).
 */
final class TuningVisualizer implements DebugRenderer.SimpleDebugRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFFF33;
    private static final int YELLOW_FILL = 0x33FFFF33;
    private static final int RED = 0xFFFF4040;
    private static final int GREEN = 0xFF40FF40;
    private static final int BLUE = 0xFF4080FF;
    private static final int CYAN = 0xFF40FFFF;
    private static final double AXIS_LENGTH = 0.75;

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        BlockPos pos = DevSession.target();
        FurnitureBlock block = DevSession.block();
        BlockState state = DevSession.state();
        if (pos == null || block == null || state == null) {
            return;
        }
        FurnitureTuning tuning = DevSession.tuning(block);
        int rotation = state.getValue(ApeiraFurnitureBlock.ROTATION);

        for (AABB box : ApeiraFurnitureBlock.rotations(tuning.shape())[rotation].toAabbs()) {
            Gizmos.cuboid(box.move(pos), GizmoStyle.stroke(WHITE, 2.0F)).setAlwaysOnTop();
        }
        List<FurnitureTuning.Box> boxes = tuning.boxes();
        if (DevSession.box() < boxes.size()) {
            for (AABB box : ApeiraFurnitureBlock.rotations(boxes.get(DevSession.box()).shape())[rotation].toAabbs()) {
                Gizmos.cuboid(box.move(pos).inflate(0.002), GizmoStyle.strokeAndFill(YELLOW, 3.0F, YELLOW_FILL)).setAlwaysOnTop();
            }
        }

        // The model's origin: the block's centre column at the model's lifted bottom, moved by the sideways offset.
        double yaw = Math.toRadians(ApeiraFurnitureBlock.yaw(rotation) + 180.0);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        Vec3 origin = Vec3.atBottomCenterOf(pos).add(turn(tuning.offsetX() / 16.0, tuning.offsetZ() / 16.0, cos, sin))
                .add(0.0, tuning.offsetY() / 16.0, 0.0);
        Gizmos.arrow(origin, origin.add(turn(AXIS_LENGTH, 0.0, cos, sin)), RED).setAlwaysOnTop();
        Gizmos.arrow(origin, origin.add(0.0, AXIS_LENGTH, 0.0), GREEN).setAlwaysOnTop();
        Gizmos.arrow(origin, origin.add(turn(0.0, AXIS_LENGTH, cos, sin)), BLUE).setAlwaysOnTop();
        Vec3 front = Vec3.atBottomCenterOf(pos).add(turn(0.0, -0.9, cos, sin)).add(0.0, 0.05, 0.0);
        Gizmos.line(Vec3.atBottomCenterOf(pos).add(0.0, 0.05, 0.0), front, CYAN).setAlwaysOnTop();
        Gizmos.billboardText("front", front.add(0.0, 0.15, 0.0), TextGizmo.Style.forColorAndCentered(CYAN).withScale(0.4F)).setAlwaysOnTop();

        if (block instanceof FurnitureBlock.Seat && tuning.seat()) {
            Gizmos.point(Vec3.atBottomCenterOf(pos).add(0.0, tuning.seatHeight() / 16.0, 0.0), GREEN, 8.0F).setAlwaysOnTop();
        }
        Gizmos.billboardTextOverBlock(block.getName().getString(), pos, 0, WHITE, 0.5F);
    }

    /** {@code (x, z)} of the north-facing model turned with the piece, as its shape and model are. */
    private static Vec3 turn(double x, double z, double cos, double sin) {
        return new Vec3(x * cos - z * sin, 0.0, x * sin + z * cos);
    }
}
