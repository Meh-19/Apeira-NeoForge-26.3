package com.apeira.main.block;

import com.apeira.main.world.feature.OrangeMycenaFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Xenon's tall orange mycena: bone meal always takes and grows one of {@link OrangeMycenaFeature}'s mushrooms there,
 * which needs dirt, grass, mycelium or podzol underneath.
 */
public class TallOrangeMycenaBlock extends XenonPlantBlock {
    public TallOrangeMycenaBlock(Properties properties) {
        super(properties, TALL, null, false);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        OrangeMycenaFeature.generateAt(level, pos, random);
    }
}
