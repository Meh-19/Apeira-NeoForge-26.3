package com.apeira.main.client.realm;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.placement.PlacementCursor;
import com.apeira.main.realm.ConfirmSpawnPayload;
import com.apeira.main.realm.SpawnPlacementPayload;
import com.apeira.main.realm.balloon.StarterBalloon;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The realm spawn placement cursor, started by {@link SpawnPlacementPayload} ({@code /realm setspawn}, the realm
 * settings' Set Spawn): decor's {@link PlacementCursor} as a 3x3x2 box where the starter balloon's basket would stand,
 * with Ren's selection textures ({@code textures/misc/spawn_selection.png}, {@code spawn_selection_blocked.png}), clear
 * where {@link StarterBalloon#canPlace}. Left-click sends {@link ConfirmSpawnPayload}.
 */
public final class SpawnPlacement extends PlacementCursor {
    public static final SpawnPlacement CURSOR = new SpawnPlacement();

    /** The balloon being moved, which does not block its own new spot. */
    private @Nullable BlockPos balloon;

    private SpawnPlacement() {
        super(ApeiraIds.id("textures/misc/spawn_selection.png"), ApeiraIds.id("textures/misc/spawn_selection_blocked.png"), "hud.apeira.setspawn");
    }

    public static void onStart(SpawnPlacementPayload payload, IPayloadContext context) {
        CURSOR.balloon = payload.balloon().orElse(null);
        CURSOR.start();
    }

    @Override
    protected AABB footprint(BlockPos target) {
        return new AABB(target.getX() - StarterBalloon.RADIUS, target.getY(), target.getZ() - StarterBalloon.RADIUS,
                target.getX() + StarterBalloon.RADIUS + 1, target.getY() + StarterBalloon.HEIGHT, target.getZ() + StarterBalloon.RADIUS + 1);
    }

    @Override
    protected boolean fits(ClientLevel level, BlockPos target) {
        return StarterBalloon.canPlace(level, target, balloon);
    }

    @Override
    protected void confirm(BlockPos target) {
        ApeiraClientNetwork.sendToServer(new ConfirmSpawnPayload(target));
    }

    @Override
    protected void onStop() {
        balloon = null;
    }
}
