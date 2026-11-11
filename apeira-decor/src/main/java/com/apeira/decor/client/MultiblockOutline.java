package com.apeira.decor.client;

import java.util.List;

import com.apeira.decor.multiblock.Multiblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Outlines the whole multiblock piece under the crosshair, not only the block that was hit: when the player looks at
 * any part (or the controller) of a piece covering more than one block, the outside edges of the piece's shape in all
 * its blocks ({@code Footprint#outline}) are drawn in vanilla's outline colour, hidden behind blocks as vanilla's is.
 * Vanilla still draws its own outline of the hit block on top.
 */
final class MultiblockOutline implements DebugRenderer.SimpleDebugRenderer {
    /** Vanilla's block outline: black at 40% opacity. */
    private static final int COLOR = 0x66000000;

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.gui.hud.isHidden()
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        Multiblock.Piece piece = Multiblock.pieceAt(minecraft.level, hit.getBlockPos());
        if (piece == null || piece.parts().size() < 2) {
            return;
        }
        BlockPos pos = piece.pos();
        List<double[]> lines = piece.controller().footprint(piece.state()).outline(piece.controller().turn(piece.state()));
        for (double[] line : lines) {
            Gizmos.line(new Vec3(pos.getX() + line[0], pos.getY() + line[1], pos.getZ() + line[2]),
                    new Vec3(pos.getX() + line[3], pos.getY() + line[4], pos.getZ() + line[5]), COLOR);
        }
    }
}
