package com.apeira.decor.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

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
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Outlines the whole multiblock piece under the crosshair, not only the block that was hit: when the player looks at
 * any part (or the controller) of a piece covering more than one block, the outside edges of the piece's shape in all
 * its blocks ({@code Footprint#outline}) are drawn in vanilla's outline colour, hidden behind blocks as vanilla's is.
 * The lines are the model's own edges where the footprint has them ({@code Footprint#outline}), so the outline follows
 * the drawn model, not its hitboxes. Vanilla's outline of the hit block, which would draw that block's hitbox edges, is
 * cancelled for such pieces ({@link #hideVanillaOutline()}).
 */
final class MultiblockOutline implements DebugRenderer.SimpleDebugRenderer {
    /** Vanilla's block outline: black at 40% opacity. */
    private static final int COLOR = 0x66000000;

    /** NeoForge's cancellable block outline events, newest first: the first one found is used. */
    private static final String[] OUTLINE_EVENTS = {
            "net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent",
            "net.neoforged.neoforge.client.event.RenderHighlightEvent$Block"};

    /**
     * Cancels vanilla's outline of the hit block while it belongs to a piece covering more than one block, which this
     * renderer outlines whole. The event is looked up by name, as its name has changed between NeoForge versions;
     * without one, vanilla's outline is drawn too.
     */
    static void hideVanillaOutline() {
        for (String name : OUTLINE_EVENTS) {
            Class<?> type;
            try {
                type = Class.forName(name);
            } catch (ClassNotFoundException e) {
                continue;
            }
            if (Event.class.isAssignableFrom(type) && ICancellableEvent.class.isAssignableFrom(type)) {
                listen(type.asSubclass(Event.class));
                return;
            }
        }
    }

    private static <T extends Event> void listen(Class<T> type) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, event -> {
            if (hoveredPiece() != null) {
                ((ICancellableEvent) event).setCanceled(true);
            }
        });
    }

    /** The piece covering more than one block that the crosshair is on, or {@code null}. */
    private static Multiblock.@Nullable Piece hoveredPiece() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        Multiblock.Piece piece = Multiblock.pieceAt(minecraft.level, hit.getBlockPos());
        return piece == null || piece.parts().size() < 2 ? null : piece;
    }

    @Override
    public void emitGizmos(double camX, double camY, double camZ, DebugValueAccess debugValues, Frustum frustum, float partialTicks) {
        Multiblock.Piece piece = hoveredPiece();
        if (piece == null || Minecraft.getInstance().gui.hud.isHidden()) {
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
