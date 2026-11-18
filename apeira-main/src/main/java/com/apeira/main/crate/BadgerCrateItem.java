package com.apeira.main.crate;

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
 * The Badger Crate. A plain item, never placed (its 3D model shows in hands, item frames and on shelves): holding use
 * for {@link #HOLD_TICKS} opens it, which takes one crate and starts a {@link BadgerSession}. The client draws the hold
 * as a bar filling the slot from the bottom ({@code client/crate/BadgerCrateDecorator}).
 */
public class BadgerCrateItem extends Item {
    /** 2.5 seconds. */
    public static final int HOLD_TICKS = 50;

    public BadgerCrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (BadgerSessions.isOpening(player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
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
        if (entity instanceof ServerPlayer player && BadgerSessions.start(player)) {
            stack.consume(1, player);
        }
        return stack;
    }
}
