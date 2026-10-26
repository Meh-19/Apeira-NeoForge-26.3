package com.apeira.main.realm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Visitors are observers: mobs ignore them ({@code mixin/RealmVisitorTargetMixin} on {@code LivingEntity#canAttack},
 * {@link #onChangeTarget}, {@link #onIncomingDamage}) and nothing pushes them or is pushed by them
 * ({@code mixin/RealmVisitorPushMixin}). Operators are never visitors here. On the server a player's role comes from
 * {@link RealmService#roleIn}; the client only simulates its own player, whose role arrives in {@link RealmHudPayload}
 * ({@link #setClientVisitor}), so its pushes agree with the server's.
 */
public final class RealmVisitors {
    private static volatile boolean clientVisitor;

    private RealmVisitors() {}

    /** Whether {@code entity} is a player who is a visitor in the realm they stand in. */
    public static boolean isVisitor(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return !RealmService.bypasses(player) && RealmService.roleIn(player, player.level()).orElse(null) == RealmRole.VISITOR;
        }
        return clientVisitor && entity instanceof Player player && player.isLocalPlayer();
    }

    /** Whether the local player is a visitor where they stand, from {@link RealmHudPayload#visitor}. */
    public static void setClientVisitor(boolean visitor) {
        clientVisitor = visitor;
    }

    /** Whether a push between {@code a} and {@code b} is skipped because one of them is a visitor. */
    public static boolean blocksPush(Entity a, Entity b) {
        return isVisitor(a) || isVisitor(b);
    }

    /** Mobs never pick a visitor as their target (anger, retaliation and brain-chosen targets included). */
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && event.getEntity() instanceof Mob && isVisitor(target)) {
            event.setCanceled(true);
        }
    }

    /** Mobs and their projectiles and explosions do not hurt visitors. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity cause = event.getSource().getEntity();
        if ((cause instanceof Mob || event.getSource().getDirectEntity() instanceof Mob) && isVisitor(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
