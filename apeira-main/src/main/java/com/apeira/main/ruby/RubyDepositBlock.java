package com.apeira.main.ruby;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.apeira.main.registry.MainRubies;

/**
 * Origin Realms' ruby deposit (its ruby ATM): right-click opens the {@link ApeiraRubyDepositMenu}, where rubies put in
 * are added to the player's balance. The block is drawn by its block entity ({@code client/ruby/RubyDepositRenderer}:
 * the base and a crystal spinning and bobbing over it), which also plays its ambient loop on the client; the block
 * model only gives the breaking particles. Mined like an enchanting table: a pickaxe, strength 5, blast resistance 1200.
 */
public class RubyDepositBlock extends BaseEntityBlock {
    private static final Component TITLE = Component.translatable("container.apeira.ruby_deposit");
    /** The base's body and lid and its four feet, from the model. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 3, 1, 15, 13, 15), Block.box(0, 13, 0, 16, 16, 16),
            Block.box(0, 0, 0, 4, 3, 4), Block.box(12, 0, 0, 16, 3, 4), Block.box(0, 0, 12, 4, 3, 16), Block.box(12, 0, 12, 16, 3, 16),
            Block.box(1, 0, 1, 15, 3, 15));

    public RubyDepositBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubyDepositBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, MainRubies.RUBY_DEPOSIT_ENTITY.get(), RubyDepositBlockEntity::clientTick) : null;
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
        return new SimpleMenuProvider((containerId, inventory, player) -> new ApeiraRubyDepositMenu(containerId, inventory,
                ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
