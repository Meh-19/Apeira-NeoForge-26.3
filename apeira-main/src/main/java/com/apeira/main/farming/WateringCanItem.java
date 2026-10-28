package com.apeira.main.farming;

import java.util.function.Consumer;

import com.apeira.main.registry.MainFarming;

import net.minecraft.ChatFormatting;
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
 * The wooden watering can. Right-clicking water fills it ({@link #CAPACITY} waterings, kept in the {@code apeira:water}
 * component, which its item model shows as the full can); each right-click on a planter box waters it for the day, and
 * on a sprinkler adds a day of water. The bar under it is the water left.
 */
public class WateringCanItem extends Item {
    public static final int CAPACITY = 20;
    private static final int BAR_COLOUR = 0x3F76E4;

    public WateringCanItem(Item.Properties properties) {
        super(properties);
    }

    public static int water(ItemStack stack) {
        return stack.getOrDefault(MainFarming.WATER.get(), 0);
    }

    /** Uses one watering, unless the player is in creative. Returns false when the can is empty. */
    public static boolean use(ItemStack stack, Player player) {
        int water = water(stack);
        if (water <= 0) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            if (water == 1) {
                stack.remove(MainFarming.WATER.get());
            } else {
                stack.set(MainFarming.WATER.get(), water - 1);
            }
        }
        return true;
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
        builder.accept(Component.translatable("tooltip.apeira.farming.watering_can", water(stack), CAPACITY).withStyle(ChatFormatting.AQUA));
    }
}
