package com.apeira.main.event;

import com.apeira.main.world.MainConfiguredFeatures;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;

/**
 * Xenon's birch saplings: a vanilla birch sapling grows Xenon's birch tree ({@code apeira:birch_tree}, the one its birch
 * forests have) instead of vanilla's, with or without flowers nearby. Xenon swapped the sapling's tree grower with a mixin.
 */
public final class BirchSaplingGrowth {
    private BirchSaplingGrowth() {}

    public static void onGrowFeature(BlockGrowFeatureEvent event) {
        if (event.getLevel().getBlockState(event.getPos()).is(Blocks.BIRCH_SAPLING)) {
            event.setFeature(MainConfiguredFeatures.BIRCH_TREE);
        }
    }
}
