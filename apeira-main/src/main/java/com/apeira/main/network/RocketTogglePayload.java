package com.apeira.main.network;

import com.apeira.core.network.ApeiraPayload;
import com.apeira.core.network.ApeiraPayloadType;
import com.apeira.main.item.RocketBootsItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: Xenon's {@code rocket_toggle}, which turns rocket boots flight on or off, stopping the player's fall
 * as it starts. Xenon registered it for a "Rocket Jump" key it never added, so nothing sends it yet; the boots fly by
 * double-jumping, as creative flight does.
 */
public record RocketTogglePayload() implements ApeiraPayload<RocketTogglePayload> {
    public static final RocketTogglePayload INSTANCE = new RocketTogglePayload();
    public static final ApeiraPayloadType<RocketTogglePayload> TYPE = ApeiraPayloadType.unit("rocket_toggle", INSTANCE);

    @Override
    public ApeiraPayloadType<RocketTogglePayload> payloadType() {
        return TYPE;
    }

    /** Toggles flight for a survival or adventure player whose rocket boots let them fly. */
    public static void handle(RocketTogglePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
                || !RocketBootsItem.grantsFlight(player)) {
            return;
        }
        boolean enable = !player.getAbilities().flying;
        player.getAbilities().flying = enable;
        if (enable) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, 0.0, motion.z);
            player.syncVelocity = true;
        }
        player.onUpdateAbilities();
    }
}
