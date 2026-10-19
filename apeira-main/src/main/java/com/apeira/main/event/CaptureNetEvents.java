package com.apeira.main.event;

import com.apeira.main.item.CaptureNetItem;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Lets the capture net catch a mob before the mob's own right-click runs, so villagers, horses and llamas are caught
 * instead of trading or being mounted (Xenon's {@code CaptureNetEventHandler}).
 */
public final class CaptureNetEvents {
    private CaptureNetEvents() {}

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof CaptureNetItem net) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        InteractionResult result = net.capture(stack, event.getEntity(), target, event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
