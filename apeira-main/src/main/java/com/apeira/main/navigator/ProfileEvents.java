package com.apeira.main.navigator;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Shift-right-clicking a player (or a dummy player's mannequin) opens their profile. */
public final class ProfileEvents {
    private ProfileEvents() {}

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getEntity().isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        UUID target = OpenProfilePayload.profileOf(event.getTarget());
        if (target == null) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            NavigatorService.openProfile(player, target);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
