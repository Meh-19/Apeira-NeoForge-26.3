package com.apeira.main.item;

import java.util.function.Consumer;

import org.slf4j.Logger;

import com.apeira.main.registry.MainDataComponents;
import com.apeira.main.registry.MainTags;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Xenon's capture net (3 uses): right-click a mob in {@code #apeira:capturable} to catch it, or one in
 * {@code #apeira:capturable_when_weakened} while it has Weakness; right-click a block to let it out on that face, which
 * costs one use. The mob keeps all its data. {@link com.apeira.main.event.CaptureNetEvents} catches mobs before their own
 * right-click (trading, mounting) runs.
 */
public class CaptureNetItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CaptureNetItem(Properties properties) {
        super(properties.durability(3));
    }

    /** Catches {@code target} in {@code stack}; PASS when the net is full or the mob can't be caught. */
    public InteractionResult capture(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (stack.has(MainDataComponents.CAPTURED_MOB)) {
            return InteractionResult.PASS;
        }
        EntityType<?> type = target.getType();
        boolean capturable = target.is(MainTags.CAPTURABLE);
        if (!capturable && !target.is(MainTags.CAPTURABLE_WHEN_WEAKENED)) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            // Mob effects of other entities are not synced, so the server decides on weakness.
            return InteractionResult.SUCCESS;
        }
        if (!capturable && !target.hasEffect(MobEffects.WEAKNESS)) {
            player.sendOverlayMessage(Component.translatable("message.apeira.capture_net.weaken").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        CompoundTag data;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(target.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, target.registryAccess());
            target.saveWithoutId(output);
            data = output.buildResult();
        }
        data.remove("Pos");
        data.remove("Motion");
        data.remove("UUID");
        stack.set(MainDataComponents.CAPTURED_MOB, new CapturedMob(TypedEntityData.of(type, data), target.getName()));
        target.discard();

        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0.0, 0.0,
                0.0, 0.0);
        player.swing(hand, SwingAnimation.DEFAULT, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return capture(stack, player, target, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CapturedMob captured = stack.get(MainDataComponents.CAPTURED_MOB);
        Player player = context.getPlayer();
        if (captured == null || player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        Direction side = context.getClickedFace();
        BlockPos spawn = clicked.relative(side);
        double x = spawn.getX() + 0.5;
        double y = side == Direction.UP ? clicked.getY() + 1.0 : side == Direction.DOWN ? clicked.getY() - 1.0 : spawn.getY();
        double z = spawn.getZ() + 0.5;

        Entity entity = EntityType.loadEntityRecursive(captured.entity().type(), captured.entity().copyTagWithoutId(), level,
                EntitySpawnReason.SPAWN_ITEM_USE, loaded -> loaded);
        if (entity == null) {
            return InteractionResult.FAIL;
        }
        entity.snapTo(x, y, z, player.getYRot() + 180.0F, 0.0F);
        level.addFreshEntityWithPassengers(entity);

        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + entity.getBbHeight() * 0.5, z, 1, 0.0, 0.0, 0.0, 0.0);
        stack.remove(MainDataComponents.CAPTURED_MOB);
        stack.hurtAndBreak(1, player, context.getHand());
        player.swing(context.getHand(), SwingAnimation.DEFAULT, true);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
            TooltipFlag flag) {
        CapturedMob captured = stack.get(MainDataComponents.CAPTURED_MOB);
        if (captured != null) {
            builder.accept(Component.translatable("tooltip.apeira.capture_net.mob", captured.name().copy().withStyle(ChatFormatting.WHITE))
                    .withStyle(ChatFormatting.GRAY));
        }
        builder.accept(Component.empty());
        builder.accept(Component.translatable("tooltip.apeira.capture_net.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.apeira.capture_net.2").withStyle(ChatFormatting.GRAY));
    }
}
