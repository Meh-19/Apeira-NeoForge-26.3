package com.apeira.decor.multiblock;

import org.jspecify.annotations.Nullable;

import com.apeira.decor.registry.DecorBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One block of a multiblock piece other than its controller ({@code apeira:multiblock_part}, one block shared by every
 * piece). Its state says where the controller is ({@link #X}, {@link #Y}, {@link #Z}: the offset from the part to the
 * controller, stored plus {@link #REACH} or {@link #HEIGHT} since block state integers can't be negative), so a part
 * needs no block entity and its shape is known wherever the controller's state is.
 *
 * <p>A part has the piece's shape inside its block, stops entities when the piece does, and sends everything else to the
 * controller: right-clicks, breaking speed, sounds, explosion resistance and pick-block. Breaking a part breaks the whole
 * piece: a player's break drops the controller's loot as if the controller itself were broken, and any other removal
 * (explosions, commands) destroys the controller with its drops, which then takes the other parts with it. A part whose
 * controller is gone (an orphan) is a plain full-block outline that breaks on its own.
 *
 * <p>Parts render nothing; the controller's renderer draws the piece.
 */
public class MultiblockPartBlock extends Block implements SimpleWaterloggedBlock {
    /** How far a part may be from its controller sideways, in blocks. */
    public static final int REACH = 4;
    /** How far a part may be above or below its controller, in blocks. */
    public static final int HEIGHT = 6;

    public static final IntegerProperty X = IntegerProperty.create("x", 0, 2 * REACH);
    public static final IntegerProperty Y = IntegerProperty.create("y", 0, 2 * HEIGHT);
    public static final IntegerProperty Z = IntegerProperty.create("z", 0, 2 * REACH);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public MultiblockPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(X, REACH).setValue(Y, HEIGHT).setValue(Z, REACH).setValue(WATERLOGGED, false));
    }

    /** The registered part block. */
    static Block block() {
        return DecorBlocks.MULTIBLOCK_PART.get();
    }

    /** Whether a part can be {@code offset} from its controller. */
    public static boolean reaches(BlockPos offset) {
        return Math.abs(offset.getX()) <= REACH && Math.abs(offset.getY()) <= HEIGHT && Math.abs(offset.getZ()) <= REACH;
    }

    /** {@code state} pointing at a controller {@code toController} away. */
    static BlockState pointingAt(BlockState state, BlockPos toController) {
        return state.setValue(X, toController.getX() + REACH).setValue(Y, toController.getY() + HEIGHT).setValue(Z, toController.getZ() + REACH);
    }

    /** Where the controller of the part {@code state} at {@code pos} stands. */
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return pos.offset(state.getValue(X) - REACH, state.getValue(Y) - HEIGHT, state.getValue(Z) - REACH);
    }

    private static Multiblock.@Nullable Piece piece(BlockGetter level, BlockPos pos, BlockState state) {
        return Multiblock.pieceOfPart(level, pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X, Y, Z, WATERLOGGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Multiblock.Piece piece = piece(level, pos, state);
        return piece == null ? Shapes.block() : piece.parts().getOrDefault(pos.subtract(piece.pos()), Shapes.empty());
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece == null || !piece.controller().partsCollide(piece.state())) {
            return Shapes.empty();
        }
        return piece.parts().getOrDefault(pos.subtract(piece.pos()), Shapes.empty());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return piece.state().useItemOn(stack, level, player, hand, hitResult.withPosition(piece.pos()));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece == null) {
            return InteractionResult.PASS;
        }
        return piece.state().useWithoutItem(level, player, hitResult.withPosition(piece.pos()));
    }

    /** Breaks as fast as the piece itself does. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        Multiblock.Piece piece = piece(level, pos, state);
        return piece == null ? super.getDestroyProgress(state, player, level, pos) : piece.state().getDestroyProgress(player, level, piece.pos());
    }

    /**
     * A player breaking a part breaks the piece: the controller drops what it would drop if the player broke it with the
     * same tool (nothing in creative), then goes, taking the other parts with it.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece != null && !level.isClientSide()) {
            if (!player.preventsBlockDrops() && player.hasCorrectToolForDrops(piece.state())) {
                Block.dropResources(piece.state(), level, piece.pos(), level.getBlockEntity(piece.pos()), player, player.getMainHandItem());
            }
            level.destroyBlock(piece.pos(), false, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** A part removed any other way (explosions, commands, pistons) breaks the piece, with its drops. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece != null) {
            level.destroyBlock(piece.pos(), true);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        Multiblock.Piece piece = piece(level, pos, state);
        if (piece == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = piece.state().getCloneItemStack(level, piece.pos(), includeData);
        BlockEntity blockEntity = level.getBlockEntity(piece.pos());
        if (includeData && blockEntity != null && !stack.isEmpty()) {
            stack.applyComponents(blockEntity.collectComponents());
        }
        return stack;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        Multiblock.Piece piece = piece(level, pos, state);
        return piece == null ? super.getSoundType(state, level, pos, entity) : piece.state().getSoundType(level, piece.pos(), entity);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        Multiblock.Piece piece = piece(level, pos, state);
        return piece == null ? super.getExplosionResistance(state, level, pos, explosion)
                : piece.state().getExplosionResistance(level, piece.pos(), explosion);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }
}
