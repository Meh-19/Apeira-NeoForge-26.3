package com.apeira.main.farming;

import org.jspecify.annotations.Nullable;

import com.apeira.main.registry.MainFarming;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Harvest quality: what a ripe crop gives and at how many stars.
 *
 * <p>Every harvest is one crop (two or three with {@link Fertilizer#PRODUCE_MULTIPLIER}). A golden crop gives its
 * golden produce, which has no stars. Any other harvest is rated 1 to 3 stars, saved in the {@code apeira:crop_quality}
 * component (so crops of different ratings do not stack):
 * <ul>
 * <li>{@link Fertilizer#THREE_STAR_GUARANTEE}: always 3 stars.</li>
 * <li>Harvested without a player (turkeys, explosions, pistons, anything automated): always 1 star.</li>
 * <li>Otherwise a roll on a score {@code s = 0.1 + 0.35 (soiled) + 0.45 * hoe}, where {@code hoe} is the harvesting
 * item's {@code apeira:hoe_quality} data map value (0 for anything that is not a hoe; netherite 1.0) plus
 * {@link #ENCHANTED_BONUS} when it is enchanted, at most 1: 3 stars with chance {@code 0.6 s}, 2 stars with chance
 * {@link #TWO_STAR_CHANCE}, else 1 star. A soiled crop cut with an enchanted netherite hoe is 3-star 54% of the time.</li>
 * </ul>
 * A crop turns golden when it ripens: always with {@link Fertilizer#GOLDEN_GOODS}, else with chance
 * {@link #GOLDEN_CHANCE} if it was soiled (it was watered every day it grew, or it would not have ripened).
 */
public final class CropQuality {
    public static final float GOLDEN_CHANCE = 0.01F;
    public static final float ENCHANTED_BONUS = 0.1F;
    public static final float TWO_STAR_CHANCE = 0.4F;
    public static final int MAX_STARS = 3;

    private CropQuality() {}

    /** Whether a crop that has just ripened turns golden. */
    public static boolean ripensGolden(boolean soiled, @Nullable Fertilizer fertilizer, RandomSource random) {
        return fertilizer == Fertilizer.GOLDEN_GOODS || soiled && random.nextFloat() < GOLDEN_CHANCE;
    }

    /** The harvest of a ripe crop, cut with {@code tool} (empty for a bare hand), or automatically. */
    public static ItemStack harvest(FarmCrop crop, boolean golden, boolean soiled, @Nullable Fertilizer fertilizer, ItemStack tool,
            boolean automated, RandomSource random) {
        int count = fertilizer == Fertilizer.PRODUCE_MULTIPLIER ? (random.nextBoolean() ? 2 : 3) : 1;
        ItemStack produce = new ItemStack(crop.produce(golden), count);
        if (!golden) {
            produce.set(MainFarming.CROP_QUALITY.get(), stars(soiled, fertilizer, tool, automated, random));
        }
        return produce;
    }

    static int stars(boolean soiled, @Nullable Fertilizer fertilizer, ItemStack tool, boolean automated, RandomSource random) {
        if (fertilizer == Fertilizer.THREE_STAR_GUARANTEE) {
            return 3;
        }
        if (automated) {
            return 1;
        }
        float score = 0.1F + (soiled ? 0.35F : 0.0F) + 0.45F * hoeQuality(tool);
        float roll = random.nextFloat();
        float threeStars = 0.6F * score;
        if (roll < threeStars) {
            return 3;
        }
        return roll < threeStars + TWO_STAR_CHANCE ? 2 : 1;
    }

    /** The harvesting item's hoe quality, 0 to 1. */
    public static float hoeQuality(ItemStack tool) {
        if (tool.isEmpty()) {
            return 0.0F;
        }
        Float quality = tool.typeHolder().getData(MainFarming.HOE_QUALITY);
        if (quality == null) {
            return 0.0F;
        }
        return Mth.clamp(quality + (tool.isEnchanted() ? ENCHANTED_BONUS : 0.0F), 0.0F, 1.0F);
    }

    /** A crop's star rating, or 0 for an item without one. */
    public static int starsOf(ItemStack stack) {
        return stack.getOrDefault(MainFarming.CROP_QUALITY.get(), 0);
    }
}
