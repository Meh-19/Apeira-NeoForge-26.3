package com.apeira.main.event;

import com.apeira.main.registry.MainPlants;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/**
 * Xenon's stick placement: right-clicking a block with a vanilla stick, when the block itself does nothing with it,
 * lays the stick down as an {@code apeira:twig} block where a block would be placed.
 */
public final class StickPlacement {
    private StickPlacement() {}

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK || !event.getItemStack().is(Items.STICK)
                || event.getPlayer() == null) {
            return;
        }
        BlockPlaceContext context = new BlockPlaceContext(event.getUseOnContext());
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = MainPlants.TWIG.get().getStateForPlacement(context);
        if (!context.canPlace() || state == null || !state.canSurvive(level, pos)) {
            return;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            SoundType sound = state.getSoundType(level, pos, event.getPlayer());
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            event.getItemStack().consume(1, event.getPlayer());
        }
        event.cancelWithResult(InteractionResult.SUCCESS);
    }
}
