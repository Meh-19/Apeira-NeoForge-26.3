package com.apeira.main.entity;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.apeira.main.registry.MainEntities;
import com.apeira.main.registry.MainSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Xenon's turkey: a breedable bird (wheat or melon seeds; feeding a chick speeds its growth) that falls slowly and
 * takes no fall damage, pecks the ground now and then, and harvests fully grown crops within 12 blocks, dropping the
 * produce and replanting them at age 0. Hurt, it panics; when another adult turkey is within 10 blocks it charges the
 * attacker instead and turns every turkey within 16 blocks against it. Drops come from its loot table.
 */
public class TurkeyEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.turkey.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.turkey.walk");
    private static final RawAnimation PECK = RawAnimation.begin().thenPlayAndHold("animation.turkey.peck");
    private static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.turkey.flap");
    private static final RawAnimation PANIC = RawAnimation.begin().thenLoop("animation.turkey.panic");

    private static final EntityDataAccessor<Boolean> IS_PECKING = SynchedEntityData.defineId(TurkeyEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PANICKING = SynchedEntityData.defineId(TurkeyEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_CHARGING = SynchedEntityData.defineId(TurkeyEntity.class,
            EntityDataSerializers.BOOLEAN);

    private static final int PECK_DURATION = 40;
    private static final int PANIC_DURATION = 60;
    /** Seconds a chick grows when fed. */
    private static final int GROWTH_BOOST = 200;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int peckTimer = 0;
    private int panicTimer = 0;

    public TurkeyEntity(EntityType<? extends TurkeyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_PECKING, false);
        builder.define(IS_PANICKING, false);
        builder.define(IS_CHARGING, false);
    }

    /** Xenon's attributes. The base is the animal attributes, as 26.x's tempt goal needs a tempt range (vanilla's 10). */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new ChargeAttackGoal(this));
        this.goalSelector.addGoal(6, new CropHarvestGoal(this));
        this.goalSelector.addGoal(7, new PeckGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        // Only retaliates.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean result = super.hurtServer(level, source, amount);
        if (result && isAlive()) {
            entityData.set(IS_PECKING, false);
            entityData.set(IS_PANICKING, true);
            panicTimer = PANIC_DURATION;

            Entity attacker = source.getEntity();
            if (attacker != null && isInGroup()) {
                // A turkey in a group charges; one alone flees.
                entityData.set(IS_CHARGING, true);
                alertNearbyTurkeys(attacker);
            }
        }
        return result;
    }

    /** Whether another adult turkey is within 10 blocks. */
    private boolean isInGroup() {
        AABB box = getBoundingBox().inflate(10.0);
        List<TurkeyEntity> nearby = level().getEntitiesOfClass(TurkeyEntity.class, box, e -> e != this && !e.isBaby());
        return !nearby.isEmpty();
    }

    /** Turns every turkey within 16 blocks that has no target against the attacker. */
    private void alertNearbyTurkeys(Entity attacker) {
        AABB box = getBoundingBox().inflate(16.0);
        List<TurkeyEntity> nearby = level().getEntitiesOfClass(TurkeyEntity.class, box, e -> e != this);
        for (TurkeyEntity turkey : nearby) {
            if (turkey.getTarget() == null) {
                turkey.setTarget(attacker instanceof LivingEntity living ? living : null);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Falls slowly.
        if (!onGround() && getDeltaMovement().y < 0.0) {
            setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
        }

        if (!level().isClientSide()) {
            if (isPanicking()) {
                panicTimer--;
                if (panicTimer <= 0) entityData.set(IS_PANICKING, false);
            }
            if (isPecking()) {
                peckTimer--;
                if (peckTimer <= 0) entityData.set(IS_PECKING, false);
            }
            if (isCharging() && getTarget() == null) {
                entityData.set(IS_CHARGING, false);
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    public void startPecking() {
        entityData.set(IS_PECKING, true);
        peckTimer = PECK_DURATION;
    }

    public boolean isPecking() {
        return entityData.get(IS_PECKING);
    }

    public boolean isPanicking() {
        return entityData.get(IS_PANICKING);
    }

    public boolean isCharging() {
        return entityData.get(IS_CHARGING);
    }

    public boolean isFlapping() {
        return !onGround() && !isInWater();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getRandom().nextBoolean() ? MainSounds.TURKEY_IDLE_1.get() : MainSounds.TURKEY_IDLE_2.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MainSounds.TURKEY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MainSounds.TURKEY_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MELON_SEEDS);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBaby() && isFood(stack)) {
            if (!level().isClientSide()) {
                ageUp(GROWTH_BOOST, true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return MainEntities.TURKEY.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TurkeyEntity>("body", 2, test -> {
            TurkeyEntity turkey = test.animatable();
            // Charging (the group attack) uses the panic animation.
            if (turkey.isCharging()) return test.setAndContinue(PANIC);
            if (turkey.isPanicking()) return test.setAndContinue(PANIC);
            if (turkey.isFlapping()) return test.setAndContinue(FLAP);
            if (turkey.isPecking()) return test.setAndContinue(PECK);
            if (test.isMoving()) return test.setAndContinue(WALK);
            return test.setAndContinue(IDLE);
        }));

        controllers.add(new AnimationController<TurkeyEntity>("head", 5, test -> {
            TurkeyEntity turkey = test.animatable();
            if (turkey.isPanicking() || turkey.isPecking() || turkey.isCharging()) {
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** The group charge: melee at 1.8 times speed while charging. */
    static class ChargeAttackGoal extends MeleeAttackGoal {
        private final TurkeyEntity turkey;

        ChargeAttackGoal(TurkeyEntity turkey) {
            super(turkey, 1.8, true);
            this.turkey = turkey;
        }

        @Override
        public boolean canUse() {
            return turkey.isCharging() && turkey.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return turkey.isCharging() && turkey.getTarget() != null;
        }
    }

    /** Stops now and then (every 100-239 goal checks) to peck the ground for two seconds. */
    static class PeckGoal extends Goal {
        private final TurkeyEntity turkey;
        private int cooldown;

        PeckGoal(TurkeyEntity turkey) {
            this.turkey = turkey;
            this.cooldown = nextCooldown(turkey.getRandom());
        }

        @Override
        public boolean canUse() {
            if (turkey.isBaby() || turkey.isPecking() || turkey.isPanicking()) return false;
            if (turkey.getTarget() != null) return false;
            if (cooldown-- > 0) return false;
            if (turkey.isInWater()) return false;
            return !turkey.getNavigation().isInProgress();
        }

        @Override
        public boolean canContinueToUse() {
            return turkey.isPecking();
        }

        @Override
        public void start() {
            turkey.getNavigation().stop();
            turkey.startPecking();
        }

        @Override
        public void stop() {
            cooldown = nextCooldown(turkey.getRandom());
        }

        private static int nextCooldown(RandomSource random) {
            return 100 + random.nextInt(140);
        }
    }

    /**
     * Now and then looks for the nearest fully grown crop within 12 blocks (2 up or down), walks to it and harvests
     * it: the crop drops its produce and is set back to age 0.
     */
    static class CropHarvestGoal extends Goal {
        private final TurkeyEntity turkey;
        private @Nullable BlockPos targetCrop = null;
        private int cooldown;

        CropHarvestGoal(TurkeyEntity turkey) {
            this.turkey = turkey;
            this.cooldown = 200 + turkey.getRandom().nextInt(200);
        }

        @Override
        public boolean canUse() {
            if (turkey.isBaby()) return false;
            if (turkey.getTarget() != null) return false;
            if (cooldown-- > 0) return false;
            targetCrop = findNearestCrop();
            return targetCrop != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (targetCrop == null) return false;
            if (!turkey.getNavigation().isInProgress()) return false;
            return turkey.distanceToSqr(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5) > 2.0;
        }

        @Override
        public void start() {
            if (targetCrop != null) {
                turkey.getNavigation().moveTo(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void tick() {
            if (targetCrop == null) return;
            if (turkey.distanceToSqr(targetCrop.getX() + 0.5, targetCrop.getY(), targetCrop.getZ() + 0.5) <= 2.5) {
                harvest();
                cooldown = 200 + turkey.getRandom().nextInt(200);
                targetCrop = null;
            }
        }

        @Override
        public void stop() {
            targetCrop = null;
            turkey.getNavigation().stop();
        }

        private void harvest() {
            if (targetCrop == null) return;
            Level level = turkey.level();
            BlockState state = level.getBlockState(targetCrop);
            if (!(state.getBlock() instanceof CropBlock crop)) return;
            if (!crop.isMaxAge(state)) return;
            Block.dropResources(state, level, targetCrop);
            level.setBlock(targetCrop, crop.getStateForAge(0), Block.UPDATE_ALL);
        }

        private @Nullable BlockPos findNearestCrop() {
            BlockPos origin = turkey.blockPosition();
            int range = 12;
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;

            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        BlockState state = turkey.level().getBlockState(candidate);
                        if (!(state.getBlock() instanceof CropBlock crop)) continue;
                        if (!crop.isMaxAge(state)) continue;

                        double dist = turkey.distanceToSqr(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = candidate;
                        }
                    }
                }
            }
            return best;
        }
    }
}
