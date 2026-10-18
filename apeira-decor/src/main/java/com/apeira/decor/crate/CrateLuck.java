package com.apeira.decor.crate;

import java.util.List;

import com.apeira.decor.cosmetic.CosmeticRarity;
import com.apeira.decor.registry.DecorAttachments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player's crate luck, CosmoLib's pity system: the weight {@link CrateLoot} moves from common to rare. It is kept in
 * the {@code apeira:crate_luck} attachment and changes each time the player collects a roll:
 * <ul>
 * <li>a legendary or limited item resets it to 0;</li>
 * <li>three commons add 15, one rare and two commons add 8, two or more rares without an epic add 3;</li>
 * <li>anything with an epic leaves it as it is.</li>
 * </ul>
 * It never goes above {@value #MAX_LUCK}.
 */
public final class CrateLuck {
    public static final float MAX_LUCK = 60.0F;

    private CrateLuck() {}

    public static float get(Player player) {
        return player.getData(DecorAttachments.CRATE_LUCK);
    }

    /** Updates the player's luck for the items they just collected from a crate with this loot. */
    public static void onCollected(Player player, CrateLoot loot, List<ItemStack> items) {
        float delta = delta(loot, items);
        float luck = delta < 0 ? 0.0F : Math.min(MAX_LUCK, get(player) + delta);
        player.setData(DecorAttachments.CRATE_LUCK, luck);
    }

    /** The change in luck for a roll, or -1 for a reset. */
    static float delta(CrateLoot loot, List<ItemStack> items) {
        int[] counts = new int[CosmeticRarity.values().length];
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                counts[loot.rarityOf(stack).ordinal()]++;
            }
        }
        int common = counts[CosmeticRarity.COMMON.ordinal()];
        int rare = counts[CosmeticRarity.RARE.ordinal()];
        int epic = counts[CosmeticRarity.EPIC.ordinal()];
        if (counts[CosmeticRarity.LEGENDARY.ordinal()] > 0 || counts[CosmeticRarity.LIMITED.ordinal()] > 0) {
            return -1.0F;
        }
        if (common == 3) {
            return 15.0F;
        }
        if (rare == 1 && common == 2) {
            return 8.0F;
        }
        if (rare >= 2 && epic == 0) {
            return 3.0F;
        }
        return 0.0F;
    }
}
