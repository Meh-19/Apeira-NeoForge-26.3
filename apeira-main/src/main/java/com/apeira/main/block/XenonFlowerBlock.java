package com.apeira.main.block;

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
    public XenonFlowerBlock(Holder<MobEffect> stewEffect, float effectSeconds, Properties properties) {
        super(stewEffect, effectSeconds, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(pos);
        return XenonPlantBlock.SHORT.move(offset.x, offset.y, offset.z);
    }
}
