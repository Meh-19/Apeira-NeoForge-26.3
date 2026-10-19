package com.apeira.main.block;

import com.apeira.main.registry.MainTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A vanilla flower (suspicious stew effect, bees) with Xenon's wider outline, that of its short plants. */
public class XenonFlowerBlock extends FlowerBlock {
    private boolean netherSoil;

    public XenonFlowerBlock(Holder<MobEffect> stewEffect, float effectSeconds, Properties properties) {
        super(stewEffect, effectSeconds, properties);
    }

    /** Lets the flower grow on {@code #apeira:nether_plant_soil} as well as dirt; call it when constructing. */
    public XenonFlowerBlock onNetherSoil() {
        netherSoil = true;
        return this;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || netherSoil && state.is(MainTags.NETHER_PLANT_SOIL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(pos);
        return XenonPlantBlock.SHORT.move(offset.x, offset.y, offset.z);
    }
}
