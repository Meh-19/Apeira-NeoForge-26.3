package com.apeira.main.toolskin;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Xenon's salvaging table, now Origin Realms' salvaging menu: right-clicking it opens {@link SalvagingTableMenu}, which takes a
 * tool skin off a tool.
 */
public class SalvagingTableBlock extends Block {
    /** The menu's title; the screen draws none, as Origin Realms' title is only its background glyph. */
    private static final Component TITLE = Component.translatable("container.apeira.salvaging_table");

    public SalvagingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((containerId, inventory, player) -> new SalvagingTableMenu(containerId, inventory,
                ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
