package com.apeira.main.entity;

import org.jspecify.annotations.Nullable;

import com.apeira.core.registry.ApeiraIds;
import com.apeira.main.registry.MainEntities;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Xenon's crocodile: an amphibious, breedable animal that charges players and livestock (chickens, cows, sheep, pigs
 * and rabbits, which flee from it through {@code LivestockFleeCrocodileMixin}). It breathes underwater, is 80% faster
 * in water and 60% faster while it has a target, and bites for a random 3 to 14 damage. Its teeth drop through its
 * loot table.
 */
public class CrocodileEntity extends Animal implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.crocodile.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.crocodile.walk");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.crocodile.swim");
    private static final RawAnimation WATER_IDLE = RawAnimation.begin().thenLoop("animation.crocodile.water_idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.crocodile.attack");

    /** Ticks the attack animation plays (0.6 s). */
    private static final int ATTACK_DURATION = 12;
    private static final float MIN_DAMAGE = 3.0F;
    private static final float MAX_DAMAGE = 14.0F;

    private static final Identifier SWIM_SPEED_ID = ApeiraIds.id("croc_swim_speed");
    private static final Identifier AGGRO_SPEED_ID = ApeiraIds.id("croc_aggro_speed");
    /** +80% speed in water. */
    private static final AttributeModifier SWIM_SPEED_BOOST = new AttributeModifier(SWIM_SPEED_ID, 0.80,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** +60% speed while it has a target. */
    private static final AttributeModifier AGGRO_SPEED_BOOST = new AttributeModifier(AGGRO_SPEED_ID, 0.60,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final EntityDataAccessor<Boolean> IS_AGGROED = SynchedEntityData.defineId(CrocodileEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ATTACKING_ANIM = SynchedEntityData.defineId(CrocodileEntity.class,
            EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int attackAnimTimer = 0;
    private boolean wasInWater = false;
    private boolean wasAggroed = false;

    public CrocodileEntity(EntityType<? extends CrocodileEntity> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl<>(this, 85, 20, 1.0F, 0.8F, false);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_AGGROED, false);
        builder.define(IS_ATTACKING_ANIM, false);
    }

    @Override
    protected void registerGoals() {
        // No roar: it charges straight at its target.
        this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (target, level) -> target instanceof Player player && !player.isCreative() && !player.isSpectator()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Chicken.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractCow.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Pig.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Breathes underwater.
        if (isInWater()) setAirSupply(getMaxAirSupply());

        if (!level().isClientSide()) {
            LivingEntity target = getTarget();
            boolean hasTarget = target != null && target.isAlive();
            entityData.set(IS_AGGROED, hasTarget);

            if (isAttackingAnim()) {
                attackAnimTimer--;
                if (attackAnimTimer <= 0) entityData.set(IS_ATTACKING_ANIM, false);
            }

            AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                boolean inWater = isInWater();
                if (inWater && !wasInWater) {
                    if (!speed.hasModifier(SWIM_SPEED_ID)) speed.addTransientModifier(SWIM_SPEED_BOOST);
                } else if (!inWater && wasInWater) {
                    speed.removeModifier(SWIM_SPEED_ID);
                }
                wasInWater = inWater;

                if (hasTarget && !wasAggroed) {
                    if (!speed.hasModifier(AGGRO_SPEED_ID)) speed.addTransientModifier(AGGRO_SPEED_BOOST);
                } else if (!hasTarget && wasAggroed) {
                    speed.removeModifier(AGGRO_SPEED_ID);
                }
                wasAggroed = hasTarget;
            }
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Bites for a random 3 to 14 damage (not the attack damage attribute), as Xenon did. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float damage = MIN_DAMAGE + getRandom().nextFloat() * (MAX_DAMAGE - MIN_DAMAGE);
        DamageSource source = damageSources().mobAttack(this);
        boolean hit = target.hurtServer(level, source, damage);
        if (hit) {
            if (target instanceof LivingEntity living) {
                living.knockback(0.4, getX() - target.getX(), getZ() - target.getZ(), source, damage);
            }
            entityData.set(IS_ATTACKING_ANIM, true);
            attackAnimTimer = ATTACK_DURATION;
            playSound(SoundEvents.EVOKER_FANGS_ATTACK, 0.8F, 0.6F);
        }
        return hit;
    }

    public boolean isAggroed() {
        return entityData.get(IS_AGGROED);
    }

    public boolean isAttackingAnim() {
        return entityData.get(IS_ATTACKING_ANIM);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON) || stack.is(Items.RABBIT)
                || stack.is(Items.CHICKEN);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return MainEntities.CROCODILE.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CrocodileEntity>("main", 3, test -> {
            CrocodileEntity croc = test.animatable();

            if (croc.isAttackingAnim()) return test.setAndContinue(ATTACK_ANIM);

            if (croc.isInWater()) {
                if (test.isMoving()) {
                    // Faster swimming while it has a target.
                    test.controller().setAnimationSpeed(croc.isAggroed() ? 1.8 : 1.3);
                    return test.setAndContinue(SWIM);
                }
                test.controller().setAnimationSpeed(1.0);
                return test.setAndContinue(WATER_IDLE);
            }

            if (test.isMoving()) {
                test.controller().setAnimationSpeed(croc.isAggroed() ? 1.6 : 1.0);
                return test.setAndContinue(WALK);
            }

            test.controller().setAnimationSpeed(1.0);
            return test.setAndContinue(IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
