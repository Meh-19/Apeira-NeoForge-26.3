package com.apeira.main.item;

import java.util.function.Consumer;

import com.apeira.main.Apeira;
import com.apeira.main.registry.MainDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * Xenon's rocket boots: while worn and fuelled they allow creative-style flight (double-jump to fly). Flying burns one
 * tick of fuel per tick, from 30 minutes ({@link MainDataComponents#ROCKET_FUEL}, full on every new pair), sheds
 * firework sparks and hisses, and stops fall damage; with a minute left the boots warn once. There is no way to refuel
 * them, as in Xenon.
 *
 * <p>Xenon granted flight through PlayerAbilityLib's {@code xenon:rocket_boots} ability source; Apeira adds the transient
 * {@link #FLIGHT_MODIFIER} to NeoForge's {@link NeoForgeMod#CREATIVE_FLIGHT} attribute, which syncs to the client and
 * never touches a creative player's own flight. {@link com.apeira.main.event.RocketBootsSafety} takes it away when the
 * boots come off.
 */
public class RocketBootsItem extends Item {
    public static final int MAX_FUEL_TICKS = 30 * 60 * 20;
    /** The flight modifier's id, {@code apeira:rocket_boots}. */
    public static final Identifier FLIGHT_MODIFIER = Apeira.REGISTRAR.id("rocket_boots");

    private static final int PARTICLE_INTERVAL = 4;
    private static final int SOUND_INTERVAL = 5;
    private static final int WARNING_TICKS = 60 * 20;

    public RocketBootsItem(Properties properties) {
        super(properties.component(MainDataComponents.ROCKET_FUEL.get(), MAX_FUEL_TICKS));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        // Only the worn boots do anything.
        if (slot != EquipmentSlot.FEET || !(owner instanceof Player player)) {
            return;
        }
        int fuel = getFuel(stack);
        if (fuel <= 0) {
            revokeFlight(player);
            return;
        }
        grantFlight(player);
        if (!player.getAbilities().flying) {
            return;
        }

        setFuel(stack, fuel - 1);
        player.fallDistance = 0.0;
        if (fuel <= WARNING_TICKS && !stack.has(MainDataComponents.ROCKET_FUEL_WARNED)) {
            stack.set(MainDataComponents.ROCKET_FUEL_WARNED, Unit.INSTANCE);
            player.sendOverlayMessage(Component.translatable("message.apeira.rocket_boots.low_fuel").withStyle(ChatFormatting.RED));
        }

        Vec3 pos = player.position();
        if (level.getGameTime() % PARTICLE_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.FIREWORK, pos.x, pos.y + 0.1, pos.z, 2, 0.15, 0.05, 0.15, 0.01);
        }
        if (level.getGameTime() % SOUND_INTERVAL == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.AZALEA_HIT, SoundSource.PLAYERS, 0.85F, 0.8F);
        }
    }

    /** Whether the rocket boots currently let {@code player} fly. */
    public static boolean grantsFlight(Player player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        return flight != null && flight.hasModifier(FLIGHT_MODIFIER);
    }

    private static void grantFlight(Player player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && !flight.hasModifier(FLIGHT_MODIFIER)) {
            flight.addTransientModifier(new AttributeModifier(FLIGHT_MODIFIER, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Takes away the boots' flight and, unless the player may fly anyway, stops them flying. */
    public static void revokeFlight(Player player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT_MODIFIER);
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    public static int getFuel(ItemStack stack) {
        return stack.getOrDefault(MainDataComponents.ROCKET_FUEL, 0);
    }

    public static void setFuel(ItemStack stack, int fuel) {
        stack.set(MainDataComponents.ROCKET_FUEL, Math.max(fuel, 0));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.apeira.rocket_boots.1").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.apeira.rocket_boots.2").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.apeira.rocket_boots.fuel", getFuel(stack) / 1200).withStyle(ChatFormatting.YELLOW));
    }
}
