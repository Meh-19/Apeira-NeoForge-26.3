package com.apeira.main.farming;

import java.util.function.Consumer;

import com.apeira.main.registry.MainFarming;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The wooden watering can. Right-clicking water fills it with {@link #CAPACITY} charges, kept in the {@code apeira:water}
 * component (which its item model shows as the full can): as many as a full sprinkler's days, so one fill fills one empty
 * sprinkler. A right-click on a planter box waters it for the day for one charge; on a sprinkler it pours in as many
 * charges as the sprinkler has room for days. Charges are used in creative too, and the bar under the can, one fifth per
 * charge, goes down as they are.
 */
public class WateringCanItem extends Item {
    /** A full can's charges: a full sprinkler's days, {@link SprinklerBlock#CAPACITY}. */
    public static final int CAPACITY = SprinklerBlock.CAPACITY;
    private static final int BAR_COLOUR = 0x3F76E4;

    public WateringCanItem(Item.Properties properties) {
        super(properties);
    }

    /** The charges left, at most {@link #CAPACITY} (cans filled when it was 20 read as full). */
    public static int water(ItemStack stack) {
        return Math.min(CAPACITY, stack.getOrDefault(MainFarming.WATER.get(), 0));
    }

    /** Uses up to {@code charges} charges and returns how many were used (0 when the can is empty). */
    public static int use(ItemStack stack, int charges) {
        int water = water(stack);
        int used = Math.min(water, Math.max(0, charges));
        if (used == water) {
            stack.remove(MainFarming.WATER.get());
        } else if (used > 0) {
            stack.set(MainFarming.WATER.get(), water - used);
        }
        return used;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || water(stack) >= CAPACITY) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return InteractionResult.PASS;
        }
        level.playSound(player, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            stack.set(MainFarming.WATER.get(), CAPACITY);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * water(stack) / CAPACITY), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOUR;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        int drops = water(stack);
        builder.accept(FarmingLore.glyphs(FarmingLore.DROP.repeat(drops) + FarmingLore.EMPTY_DROP.repeat(CAPACITY - drops)));
    }
}
