package com.apeira.main.entity;

import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.apeira.main.registry.MainSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Xenon's wraith: a gravity-free, fire-immune ghost that floats about where it spawned and charges players (from 32
 * blocks, 16 when they sneak), iron golems and tamed wolves. Half the time it is hurt (with a 10 second cooldown) it
 * vanishes for 5 seconds, then reappears behind its target.
 */
public class WraithEntity extends Monster implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.wraith.idle");
    private static final RawAnimation AGGRAVATED = RawAnimation.begin().thenLoop("animation.wraith.aggravated");

    private static final EntityDataAccessor<Boolean> WRAITH_INVISIBLE = SynchedEntityData.defineId(WraithEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AGGRO = SynchedEntityData.defineId(WraithEntity.class,
            EntityDataSerializers.BOOLEAN);

    /** Ticks it stays invisible (5 s). */
    private static final int INVISIBLE_DURATION = 100;
    /** Ticks before it can vanish again (10 s). */
    private static final int INVIS_COOLDOWN_TICKS = 200;
    /** Wanders within this many blocks of its home. */
    private static final int HOME_RADIUS = 24;
    private static final double BASE_DETECTION_RANGE = 32.0;
    private static final double SNEAK_DETECTION_RANGE = 16.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int invisibleTicks = 0;
    private int invisCooldown = 0;
    /** Where it spawned (its first ticked position). */
    private @Nullable BlockPos homePos;

    public WraithEntity(EntityType<? extends WraithEntity> type, Level level) {
        super(type, level);
        this.moveControl = new WraithMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.FOLLOW_RANGE, BASE_DETECTION_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.getNodeEvaluator().setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WRAITH_INVISIBLE, false);
        builder.define(AGGRO, false);
    }

    public boolean isWraithInvisible() {
        return entityData.get(WRAITH_INVISIBLE);
    }

    public boolean isAggro() {
        return entityData.get(AGGRO);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WraithChargeGoal(this));
        this.goalSelector.addGoal(2, new WraithWanderGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, (player, level) -> {
            double range = player.isShiftKeyDown() ? SNEAK_DETECTION_RANGE : BASE_DETECTION_RANGE;
            return this.distanceToSqr(player) <= range * range;
        }));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Wolf.class, 10, true, false,
                (wolf, level) -> wolf instanceof Wolf w && w.isTame()));
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        // Gentle hover damping: no plummeting, slow vertical drift.
        Vec3 velocity = getDeltaMovement();
        if (!onGround()) {
            setDeltaMovement(velocity.multiply(1.0, 0.8, 1.0));
        }

        if (!level().isClientSide()) {
            entityData.set(AGGRO, getTarget() != null);

            if (homePos == null) {
                homePos = blockPosition();
            }

            if (invisCooldown > 0) invisCooldown--;

            if (invisibleTicks > 0) {
                invisibleTicks--;
                if (invisibleTicks <= 0) {
                    entityData.set(WRAITH_INVISIBLE, false);
                    setInvisible(false);
                    teleportBehindTarget();
                    playSound(MainSounds.WRAITH_APPEAR.get(), 1.0F, 1.0F);
                    invisCooldown = INVIS_COOLDOWN_TICKS;
                }
            }
        } else if (isWraithInvisible()) {
            // Smoke while vanished.
            for (int i = 0; i < 3; i++) {
                level().addParticle(ParticleTypes.SMOKE,
                        getX() + random.nextGaussian() * 0.3,
                        getY() + random.nextGaussian() * 0.5 + 1.0,
                        getZ() + random.nextGaussian() * 0.3,
                        0, 0.05, 0);
            }
        }
    }

    /** Half the time it is hurt, off cooldown, it vanishes. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean result = super.hurtServer(level, source, damage);
        if (result && isAlive() && !isWraithInvisible() && invisCooldown <= 0 && random.nextFloat() < 0.5F) {
            startInvisibility(level);
        }
        return result;
    }

    private void startInvisibility(ServerLevel level) {
        invisibleTicks = INVISIBLE_DURATION;
        entityData.set(WRAITH_INVISIBLE, true);
        setInvisible(true);
        playSound(MainSounds.WRAITH_APPARATE.get(), 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 20, 0.4, 0.5, 0.4, 0.02);
    }

    /** Moves two blocks behind the target (by its yaw), half a block up, when that block is air. */
    private void teleportBehindTarget() {
        LivingEntity target = getTarget();
        if (target == null) return;

        float yaw = target.getYRot() * ((float) Math.PI / 180F);
        double behindX = target.getX() + Mth.sin(yaw) * 2.0;
        double behindZ = target.getZ() - Mth.cos(yaw) * 2.0;
        double behindY = target.getY() + 0.5;

        if (level().isEmptyBlock(BlockPos.containing(behindX, behindY, behindZ))) {
            snapTo(behindX, behindY, behindZ, getYRot(), getXRot());
            getNavigation().stop();
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MainSounds.WRAITH_IDLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return MainSounds.WRAITH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return MainSounds.WRAITH_DEATH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    /**
     * Xenon's spawn rule: below y 0, in complete darkness (block and sky light 0), on a block mobs can spawn on. Xenon
     * registered it {@code ON_GROUND} with {@code MOTION_BLOCKING_NO_LEAVES} and spawned wraiths only in its Depths
     * biome (weight 15, groups of one).
     */
    public static boolean canSpawnInDepths(EntityType<WraithEntity> type, ServerLevelAccessor level, EntitySpawnReason reason,
            BlockPos pos, RandomSource random) {
        return pos.getY() < 0
                && level.getMaxLocalRawBrightness(pos) == 0
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    /** Prefers darkness. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.5F - level.getMaxLocalRawBrightness(pos);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {}

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (homePos != null) {
            output.putInt("HomeX", homePos.getX());
            output.putInt("HomeY", homePos.getY());
            output.putInt("HomeZ", homePos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getInt("HomeX").ifPresent(x -> homePos = new BlockPos(x, input.getIntOr("HomeY", 0), input.getIntOr("HomeZ", 0)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<WraithEntity>("body", 5,
                test -> test.setAndContinue(test.animatable().isAggro() ? AGGRAVATED : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Vex-style move control: steers its velocity straight at the wanted point (no path), blending 15% a tick, at the
     * flying speed times the goal's speed, and turns to face where it goes.
     */
    static class WraithMoveControl extends MoveControl<WraithEntity> {
        WraithMoveControl(WraithEntity wraith) {
            super(wraith);
        }

        @Override
        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO) return;

            double dx = this.wantedX - mob.getX();
            double dy = this.wantedY - mob.getY();
            double dz = this.wantedZ - mob.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq < 1.0) {
                this.operation = MoveControl.Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
                return;
            }

            double dist = Math.sqrt(distSq);
            float speed = (float) (this.speedModifier * mob.getAttributeValue(Attributes.FLYING_SPEED));
            double vx = dx / dist * speed;
            double vy = dy / dist * speed;
            double vz = dz / dist * speed;

            Vec3 current = mob.getDeltaMovement();
            mob.setDeltaMovement(
                    current.x + (vx - current.x) * 0.15,
                    current.y + (vy - current.y) * 0.15,
                    current.z + (vz - current.z) * 0.15);

            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            mob.setYRot(rotlerp(mob.getYRot(), targetYaw, 20.0F));
            mob.yBodyRot = mob.getYRot();
        }
    }

    /** Flies at the target's eyes and bites (every 20 goal ticks) within 2 blocks. */
    static class WraithChargeGoal extends Goal {
        private final WraithEntity wraith;
        private int cooldown;

        WraithChargeGoal(WraithEntity wraith) {
            this.wraith = wraith;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = wraith.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (wraith.isWraithInvisible()) return false;
            return wraith.distanceToSqr(target) < BASE_DETECTION_RANGE * BASE_DETECTION_RANGE;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = wraith.getTarget();
            return target != null && target.isAlive() && !wraith.isWraithInvisible();
        }

        @Override
        public void start() {
            wraith.playSound(MainSounds.WRAITH_TARGET.get(), 1.0F, 1.0F);
        }

        @Override
        public void tick() {
            LivingEntity target = wraith.getTarget();
            if (target == null) return;

            wraith.getMoveControl().setWantedPosition(target.getX(), target.getEyeY(), target.getZ(), 1.0);
            wraith.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (wraith.distanceToSqr(target) < 4.0 && cooldown <= 0) {
                wraith.doHurtTarget(getServerLevel(wraith), target);
                wraith.playSound(MainSounds.WRAITH_BITE.get(), 1.0F, 1.0F);
                cooldown = 20;
            }
            if (cooldown > 0) cooldown--;
        }

        @Override
        public void stop() {
            wraith.entityData.set(AGGRO, false);
        }
    }

    /** Slow aimless floating near home: about every 1.5 s when idle, a new air point within 24 blocks. */
    static class WraithWanderGoal extends Goal {
        private final WraithEntity wraith;

        WraithWanderGoal(WraithEntity wraith) {
            this.wraith = wraith;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return wraith.getTarget() == null && !wraith.isWraithInvisible();
        }

        @Override
        public boolean canContinueToUse() {
            return wraith.getTarget() == null && !wraith.isWraithInvisible();
        }

        @Override
        public void tick() {
            MoveControl<?> moveControl = wraith.getMoveControl();

            if (!moveControl.hasWanted() && wraith.getRandom().nextInt(30) == 0) {
                BlockPos home = wraith.homePos != null ? wraith.homePos : wraith.blockPosition();
                RandomSource random = wraith.getRandom();

                double x = home.getX() + random.nextInt(HOME_RADIUS * 2) - HOME_RADIUS;
                double y = home.getY() + random.nextInt(8) - 2;
                double z = home.getZ() + random.nextInt(HOME_RADIUS * 2) - HOME_RADIUS;

                y = Math.max(wraith.level().getMinY() + 2, Math.min(y, home.getY() + 10));

                if (wraith.level().isEmptyBlock(BlockPos.containing(x, y, z))) {
                    moveControl.setWantedPosition(x, y, z, 0.5);
                }
            }
        }
    }
}
