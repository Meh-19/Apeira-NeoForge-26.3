package com.apeira.main.realm.balloon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One of the eight cells of the starter balloon's basket floor around its {@link StarterBalloonBlock}: a full,
 * invisible, unbreakable block that the balloon's model is drawn over, so the 3x3 floor holds players up. It has no item
 * and drops nothing; {@link StarterBalloon} places and removes it with the balloon.
 */
public class StarterBalloonPartBlock extends Block {
    public StarterBalloonPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
