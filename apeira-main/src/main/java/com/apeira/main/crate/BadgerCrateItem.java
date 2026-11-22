package com.apeira.main.crate;

import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * A crate ({@link CrateKind}: the Badger or Animal Crate). A plain item, never placed (its 3D model shows in hands,
 * item frames and on shelves): holding use for {@link #HOLD_TICKS} opens it, which takes one crate and starts a {@link BadgerSession}. The client draws the hold
 * as a bar filling the slot from the bottom ({@code client/crate/BadgerCrateDecorator}).
 */
public class BadgerCrateItem extends Item {
    private final CrateKind kind;

    /** 2.5 seconds. */
    public static final int HOLD_TICKS = 50;

    public BadgerCrateItem(Properties properties, CrateKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (BadgerSessions.isOpening(player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    /** Letting go early stops the charging sound and plays the beacon powering down. */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundStopSoundPacket(SoundEvents.BEACON_ACTIVATE.location(), SoundSource.PLAYERS));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return HOLD_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundStopSoundPacket(SoundEvents.BEACON_ACTIVATE.location(), SoundSource.PLAYERS));
        }
        if (entity instanceof ServerPlayer player && BadgerSessions.start(player, kind)) {
            stack.consume(1, player);
        }
        return stack;
    }
}
