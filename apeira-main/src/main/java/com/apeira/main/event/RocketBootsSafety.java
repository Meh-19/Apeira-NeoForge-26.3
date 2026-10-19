package com.apeira.main.event;

import com.apeira.main.item.RocketBootsItem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Xenon's rocket boots safety net: a survival player whose rocket boots gave them flight loses it, and falls, as soon as
 * the boots are no longer on their feet (the boots' own tick only runs while worn).
 */
public final class RocketBootsSafety {
    private RocketBootsSafety() {}

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()
                && !(player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof RocketBootsItem) && RocketBootsItem.grantsFlight(player)) {
            RocketBootsItem.revokeFlight(player);
        }
    }
}
