package com.apeira.main.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.apeira.main.item.ShadowArmorItem;
import com.apeira.main.registry.MainEffects;
import com.apeira.main.registry.MainGear;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Xenon's Shadow Mode: 4 seconds of {@link MainEffects#SHADOW_MODE} and invisibility, during which the player takes no
 * damage ({@code mixin/ShadowDamageMixin}), mobs can't target them ({@code mixin/ShadowDetectionMixin}) and their next
 * attack does 1.5 times the damage and ends it ({@code mixin/ShadowAttackMixin}). A full set of shadow armour enters it
 * when hit, then waits 30 seconds before it can again. Each shadow armour piece worn also cuts 3 blocks off the follow
 * range of mobs targeting the player.
 */
public final class ShadowModeManager {
    public static final int SHADOW_DURATION_TICKS = 4 * 20;
    public static final int COOLDOWN_TICKS = 30 * 20;
    public static final float DETECTION_REDUCTION_PER_PIECE = 3.0F;
    public static final float STEALTH_DAMAGE_MULTIPLIER = 1.5F;

    /** The game time each player can enter Shadow Mode again. Server side and not saved, as in Xenon. */
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    private ShadowModeManager() {}

    public static boolean isInShadowMode(Player player) {
        return player.hasEffect(MainEffects.SHADOW_MODE);
    }

    public static boolean canActivate(Player player) {
        Long cooldown = COOLDOWNS.get(player.getUUID());
        return cooldown == null || player.level().getGameTime() >= cooldown;
    }

    /** Enters Shadow Mode, with a low phantom call and a burst of smoke, and starts the cooldown. */
    public static void activate(ServerPlayer player) {
        if (!canActivate(player)) {
            return;
        }
        // Xenon showed the effect's icon, but had no texture for it; Apeira hides it rather than show a missing sprite.
        player.addEffect(new MobEffectInstance(MainEffects.SHADOW_MODE, SHADOW_DURATION_TICKS, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SHADOW_DURATION_TICKS, 0, false, false, false));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.02);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 0.5, player.getZ(), 15, 0.4, 0.6, 0.4, 0.01);
        COOLDOWNS.put(player.getUUID(), level.getGameTime() + COOLDOWN_TICKS);
    }

    /** Leaves Shadow Mode early, with a higher phantom call and smoke. */
    public static void deactivate(ServerPlayer player) {
        player.removeEffect(MainEffects.SHADOW_MODE);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PHANTOM_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.level().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.7, 0.4,
                0.02);
    }

    /** How many shadow armour pieces the player wears. */
    public static int getShadowArmorCount(Player player) {
        int count = 0;
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && player.getItemBySlot(slot).getItem() instanceof ShadowArmorItem) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasFullShadowSet(Player player) {
        return getShadowArmorCount(player) >= 4;
    }

    /** Blocks taken off the follow range of mobs targeting the player: 3 per shadow armour piece. */
    public static float getDetectionReduction(Player player) {
        return getShadowArmorCount(player) * DETECTION_REDUCTION_PER_PIECE;
    }

    /** Whether {@code stack} is one of the shadow tools or the shadow sword, which break blocks silently. */
    public static boolean isShadowTool(ItemStack stack) {
        return MainGear.shadowTools().stream().anyMatch(tool -> stack.is(tool.get()));
    }
}
