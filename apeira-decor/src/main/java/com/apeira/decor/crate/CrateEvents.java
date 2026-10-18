package com.apeira.decor.crate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Game events for crates: rerolling, protecting the opener and loading {@link CrateLoot}. */
public final class CrateEvents {
    private CrateEvents() {}

    /** Adds the listeners to the game event bus ({@code NeoForge.EVENT_BUS}). */
    public static void register(IEventBus gameEventBus) {
        gameEventBus.addListener(CrateLootManager::addListener);
        gameEventBus.addListener(CrateEvents::onLeftClickBlock);
        gameEventBus.addListener(CrateEvents::onIncomingDamage);
    }

    /** Punching an open crate rerolls it (for its opener) instead of breaking it. */
    private static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CrateBlockEntity crate) || crate.state() == CrateState.CLOSED) {
            return;
        }
        event.setCanceled(true);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && event.getEntity() instanceof ServerPlayer player) {
            crate.reroll(player);
        }
    }

    /** A player opening a crate takes no damage, as in CosmoLib. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()
                && CrateBlockEntity.isOpening(player.getUUID())) {
            event.setCanceled(true);
        }
    }
}
