package com.apeira.decor.item;

import com.apeira.decor.paint.Paint;
import com.apeira.decor.paint.PaintBlockEntity;
import com.apeira.decor.paint.PaintableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Paints {@link PaintableBlock}s and paintable cosmetics. The brush gets its paint in the painting table;
 * right-clicking a paintable block paints it and uses up one durability point. Sneak-right-clicking a painted block
 * copies its paint onto the brush. Right-clicking the air does the same to a paintable cosmetic held in the other hand
 * (CosmoLib's brush only painted blocks; its cosmetics were painted in the table).
 *
 * <p>The brush acts before the block's own right-click (NeoForge's {@code onItemUseFirst}), so painting a chair does
 * not sit the player down.
 */
public class PaintbrushItem extends Item {
    public PaintbrushItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level.getBlockState(pos).getBlock() instanceof PaintableBlock)
                || !(level.getBlockEntity(pos) instanceof PaintBlockEntity block)) {
            return InteractionResult.PASS;
        }

        if (player != null && player.isSecondaryUseActive()) {
            Integer copied = block.paint();
            if (copied == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                copyPaint(level, player, stack, copied, pos);
            }
            return InteractionResult.SUCCESS;
        }

        Integer paint = Paint.get(stack);
        if (paint == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            block.setPaint(paint);
            server.sendParticles(new DustParticleOptions(Paint.tint(paint), 1.0F), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 10,
                    0.3, 0.2, 0.3, 0.05);
            if (player != null) {
                stack.hurtAndBreak(1, player, context.getHand());
            }
        }
        level.playSound(player, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack brush = player.getItemInHand(hand);
        ItemStack cosmetic = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!Paint.isPaintableCosmetic(cosmetic)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = player.blockPosition();

        if (player.isSecondaryUseActive()) {
            Integer copied = Paint.get(cosmetic);
            if (copied == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                copyPaint(level, player, brush, copied, pos);
            }
            return InteractionResult.SUCCESS;
        }

        Integer paint = Paint.get(brush);
        if (paint == null || paint.equals(Paint.get(cosmetic))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Paint.set(cosmetic, paint);
            brush.hurtAndBreak(1, player, hand);
        }
        level.playSound(player, pos, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void copyPaint(Level level, Player player, ItemStack brush, int paint, BlockPos pos) {
        Paint.set(brush, paint);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
        Component description = Paint.describe(paint);
        player.sendOverlayMessage(Component.translatable("message.apeira.paint_copied",
                description != null ? description : Component.literal(String.format("#%06X", paint))));
    }
}
