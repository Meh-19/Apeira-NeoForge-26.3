package com.apeira.dev.client;

import java.util.List;

import com.apeira.decor.block.ApeiraFurnitureBlock;
import com.apeira.decor.furniture.FurnitureBlock;
import com.apeira.decor.furniture.FurnitureTuning;
import com.apeira.decor.furniture.SeatFurnitureBlock;

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
 * Draws the bound piece's tuning in the world, through anything in front of it: its footprint boxes as placed (white),
 * and by mode the seats (green, the selected one yellow), the box under the crosshair that a right-click deletes (red),
 * or the light and collision; and the piece's front (north before turning).
 */
final class TuningVisualizer implements DebugRenderer.SimpleDebugRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFFF33;
    private static final int RED = 0xFFFF4040;
    private static final int RED_FILL = 0x44FF4040;
    private static final int GREEN = 0xFF40FF40;
    private static final int CYAN = 0xFF40FFFF;

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        BlockPos pos = DevSession.target();
        FurnitureBlock block = DevSession.block();
        BlockState state = DevSession.state();
        if (pos == null || block == null || state == null) {
            return;
        }
        FurnitureTuning tuning = DevSession.tuning(block);
        int turn = block.turn(state);

        for (AABB box : block.footprint(state).boxes(turn)) {
            Gizmos.cuboid(box.move(pos), GizmoStyle.stroke(WHITE, 1.5F)).setAlwaysOnTop();
        }

        switch (DevSession.mode()) {
            case SEATING -> {
                if (block instanceof SeatFurnitureBlock seat) {
                    List<Vec3> seats = seat.seats(state);
                    for (int i = 0; i < seats.size(); i++) {
                        Vec3 at = Vec3.atLowerCornerOf(pos).add(seats.get(i));
                        boolean selected = i == DevSession.seat();
                        Gizmos.point(at, selected ? YELLOW : GREEN, selected ? 10.0F : 7.0F).setAlwaysOnTop();
                        Gizmos.billboardText(String.valueOf(i + 1), at.add(0.0, 0.15, 0.0),
                                TextGizmo.Style.forColorAndCentered(selected ? YELLOW : GREEN).withScale(0.35F)).setAlwaysOnTop();
                    }
                }
            }
            case BOXES -> {
                DevSession.TargetBox target = DevSession.targetBox();
                if (target != null) {
                    Gizmos.cuboid(target.world().inflate(0.003), GizmoStyle.strokeAndFill(RED, 3.0F, RED_FILL)).setAlwaysOnTop();
                }
            }
            case LIGHT -> {
                String light = tuning.light() < 0 ? "own light" : "light " + tuning.light();
                Gizmos.billboardText(light + ", collision " + (tuning.collision() ? "on" : "off"), Vec3.atCenterOf(pos).add(0.0, 0.9, 0.0),
                        TextGizmo.Style.forColorAndCentered(YELLOW).withScale(0.4F)).setAlwaysOnTop();
            }
        }

        // The front, where a sitter looks out: north before turning.
        double yaw = Math.toRadians(ApeiraFurnitureBlock.yaw(state.getValue(ApeiraFurnitureBlock.ROTATION)) + 180.0);
        Vec3 base = Vec3.atBottomCenterOf(pos).add(0.0, 0.05, 0.0);
        Vec3 front = base.add(-Math.sin(yaw) * -0.9, 0.0, Math.cos(yaw) * -0.9);
        Gizmos.line(base, front, CYAN).setAlwaysOnTop();
        Gizmos.billboardText("front", front.add(0.0, 0.15, 0.0), TextGizmo.Style.forColorAndCentered(CYAN).withScale(0.4F)).setAlwaysOnTop();
        Gizmos.billboardTextOverBlock(block.getName().getString(), pos, 0, WHITE, 0.5F);
    }
}
