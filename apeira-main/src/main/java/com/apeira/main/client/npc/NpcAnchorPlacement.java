package com.apeira.main.client.npc;

import org.jspecify.annotations.Nullable;

import com.apeira.core.network.client.ApeiraClientNetwork;
import com.apeira.core.registry.ApeiraIds;
import com.apeira.decor.client.placement.PlacementCursor;
import com.apeira.main.npc.ConfirmNpcAnchorPayload;
import com.apeira.main.npc.NpcAnchorPayload;
import com.apeira.main.npc.NpcEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The NPC anchor placement cursor, started by {@link NpcAnchorPayload} (the NPC settings' Set Anchor): decor's
 * {@link PlacementCursor}, the same editor as the realm spawn's, sized to the NPC's collision box
 * ({@link NpcEntity#footprint}), clear where {@link NpcEntity#canStandAt}. Left-click sends
 * {@link ConfirmNpcAnchorPayload}, and the server moves the NPC there facing the player; the NPC leaving the level drops it.
 */
public final class NpcAnchorPlacement extends PlacementCursor {
    public static final NpcAnchorPlacement CURSOR = new NpcAnchorPlacement();

    private int npc;
    private @Nullable EntityType<?> type;

    private NpcAnchorPlacement() {
        super(ApeiraIds.id("textures/misc/spawn_selection.png"), ApeiraIds.id("textures/misc/spawn_selection_blocked.png"), "hud.apeira.npc_anchor");
    }

    public static void onStart(NpcAnchorPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getEntity(payload.npc()) instanceof NpcEntity entity) {
            CURSOR.npc = payload.npc();
            CURSOR.type = entity.getType();
            CURSOR.start();
        }
    }

    @Override
    protected boolean stillValid(Minecraft minecraft) {
        return type != null && minecraft.level != null && minecraft.level.getEntity(npc) instanceof NpcEntity;
    }

    @Override
    protected AABB footprint(BlockPos target) {
        EntityType<?> npcType = type;
        return npcType == null ? new AABB(target) : NpcEntity.footprint(npcType, target);
    }

    @Override
    protected boolean fits(ClientLevel level, BlockPos target) {
        EntityType<?> npcType = type;
        return npcType != null && NpcEntity.canStandAt(level, npcType, target);
    }

    @Override
    protected void confirm(BlockPos target) {
        ApeiraClientNetwork.sendToServer(new ConfirmNpcAnchorPayload(npc, target));
    }

    @Override
    protected void onStop() {
        type = null;
    }
}
