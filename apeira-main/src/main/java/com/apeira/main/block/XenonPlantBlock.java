package com.apeira.main.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Xenon's grasses, bushes and mushrooms: one-block plants that grow where vanilla grass does. A short plant has a
 * 13-pixel outline and bone meal turns it into its tall variant when the block above is air; a tall plant is still a
 * single block, with an outline two blocks high. Brambles slow and hurt living things inside them, like sweet berry
 * bushes, but on every tick and whatever their age.
 *
 * <p>Xenon's bone meal placed the tall variant as a two-block plant, which it is not; here it replaces the short one.
 */
public class XenonPlantBlock extends VegetationBlock implements BonemealableBlock {
    public static final VoxelShape SHORT = Block.box(2, 0, 2, 14, 13, 14);
    public static final VoxelShape TALL = Block.box(2, 0, 2, 14, 32, 14);

    private final VoxelShape shape;
    private final @Nullable Supplier<? extends Block> grownInto;
    private final boolean thorny;

    public XenonPlantBlock(Properties properties, VoxelShape shape, @Nullable Supplier<? extends Block> grownInto, boolean thorny) {
        super(properties);
        this.shape = shape;
        this.grownInto = grownInto;
        this.thorny = thorny;
    }

    /** A short plant that bone meal turns into {@code tall}. */
    public static XenonPlantBlock growsInto(Properties properties, Supplier<? extends Block> tall) {
        return new XenonPlantBlock(properties, SHORT, tall, false);
    }

    public static XenonPlantBlock tall(Properties properties) {
        return new XenonPlantBlock(properties, TALL, null, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(pos);
        return shape.move(offset.x, offset.y, offset.z);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
            boolean isPrecise) {
        if (thorny && entity instanceof LivingEntity living) {
            entity.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));
            if (level instanceof ServerLevel serverLevel) {
                living.hurtServer(serverLevel, level.damageSources().sweetBerryBush(), 1.0F);
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return grownInto != null && level.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (grownInto != null) {
            BlockState grown = grownInto.get().defaultBlockState();
            if (grown.canSurvive(level, pos)) {
                level.setBlock(pos, grown, Block.UPDATE_ALL);
            }
        }
    }
}
