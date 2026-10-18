package com.apeira.main.item;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Xenon's raw and cooked turkey. As in Xenon they are not eaten from the hand: right-clicking a block places the whole
 * turkey on it, to be eaten there in bites ({@link com.apeira.main.block.TurkeyMeatBlock}).
 */
public class TurkeyItem extends Item {
    private final Supplier<? extends Block> placed;

    public TurkeyItem(Properties properties, Supplier<? extends Block> placed) {
        super(properties);
        this.placed = placed;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(pos.below()).isSolid() || !level.getBlockState(pos).isAir()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, placed.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
